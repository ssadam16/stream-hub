package com.technokratos.solutionservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.technokratos.common.dto.*;
import com.technokratos.common.enums.OutboxEventStatus;
import com.technokratos.common.enums.SolutionStatus;
import com.technokratos.common.events.CodeExecutionRequest;
import com.technokratos.common.events.CodeExecutionResult;
import com.technokratos.common.events.SolutionCompletedEvent;
import com.technokratos.common.events.TaskSubmissionEvent;
import com.technokratos.common.handler.exceptions.AccessDeniedException;
import com.technokratos.common.handler.exceptions.SolutionNotFound;
import com.technokratos.common.topics.KafkaTopics;
import com.technokratos.securityspringbootstarter.SecurityUtil;
import com.technokratos.solutionservice.client.TaskClient;
import com.technokratos.solutionservice.dto.SolutionRequest;
import com.technokratos.solutionservice.mapper.SolutionMapper;
import com.technokratos.solutionservice.model.OutboxEvent;
import com.technokratos.solutionservice.model.SolutionEntity;
import com.technokratos.solutionservice.model.SolutionSource;
import com.technokratos.solutionservice.repository.MongoSolutionRepository;
import com.technokratos.solutionservice.repository.OutboxEventRepository;
import com.technokratos.solutionservice.repository.SolutionRepository;
import com.technokratos.solutionservice.util.SolutionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class SolutionService {

    private static final Set<SolutionStatus> FINAL_STATUSES = Set.of(
            SolutionStatus.COMPLETED,
            SolutionStatus.WRONG_ANSWER,
            SolutionStatus.TIME_LIMIT_EXCEEDED,
            SolutionStatus.MEMORY_LIMIT_EXCEEDED,
            SolutionStatus.COMPILATION_ERROR,
            SolutionStatus.RUNTIME_ERROR,
            SolutionStatus.INTERNAL_ERROR
    );

    private final SecurityUtil securityUtil;
    private final SolutionUtil solutionUtil;
    private final SolutionRepository solutionRepository;
    private final MongoSolutionRepository mongoSolutionRepository;
    private final SolutionMapper solutionMapper;
    private final TaskClient taskClient;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "completedTasks", key = "#userId")
    })
    public UUID submit(SolutionRequest dto, UUID taskId, UUID userId) {
        log.info("User {} submitting task {}", userId, taskId);

        TaskAdminView task = taskClient.getForExecution(taskId);

        SolutionEntity solution = SolutionEntity.builder()
                .userId(userId)
                .taskId(taskId)
                .language(dto.language())
                .status(SolutionStatus.PROCESSING)
                .difficulty(task.difficulty())
                .points(task.points())
                .build();

        SolutionEntity savedSolution = solutionRepository.save(solution);
        UUID solutionId = savedSolution.getId();

        String payload;
        try {
            payload = objectMapper.writeValueAsString(
                    CodeExecutionRequest.builder()
                            .solutionId(solutionId)
                            .taskId(taskId)
                            .userId(userId)
                            .language(dto.language())
                            .code(dto.code())
                            .timeLimitMs(task.timeLimitMs())
                            .memoryLimitMb(task.memoryLimitMb())
                            .testCases(solutionMapper.toExecutorTests(task.testCases()))
                            .build()
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize CodeExecutionRequest", e);
        }

        outboxEventRepository.save(
                OutboxEvent.builder()
                        .topic(KafkaTopics.TOPIC_CODE_EXECUTION)
                        .aggregateId(solutionId)
                        .messageKey(userId)
                        .status(OutboxEventStatus.NEW)
                        .eventType("CODE_EXECUTION_REQUEST")
                        .payload(payload)
                        .build()
        );

        saveSolutionSource(solutionId, taskId, userId, dto);

        log.info("Solution {} submitted and execution requested via outbox", solutionId);
        return solutionId;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "solutions", key = "#solutionId")
    public SolutionView getById(UUID solutionId, Jwt jwt) {
        SolutionEntity s = solutionRepository.findById(solutionId)
                .orElseThrow(() -> new SolutionNotFound(solutionId));

        deniedAccessIfNotResourceOwnerOrAdmin(jwt, s);

        String code = mongoSolutionRepository.findBySolutionIdAndUserId(solutionId, s.getUserId())
                .map(SolutionSource::getCode)
                .orElse(null);
        return solutionMapper.toView(s, code);
    }

    @Transactional(readOnly = true)
    public PageResponse<SolutionShortView> listMine(UUID userId, Pageable pageable) {
        Page<SolutionShortView> content = solutionRepository.findAllByUserId(userId, pageable)
                .map(solutionMapper::toShortView);

        return new PageResponse<>(
                content.getContent(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                content.getTotalElements(),
                content.getTotalPages()
        );
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "solutions", key = "#id"),
            @CacheEvict(value = "completedTasks", key = "#jwt.subject")
    })
    public void delete(UUID id, Jwt jwt) {
        SolutionEntity s = solutionRepository.findById(id)
                .orElseThrow(() -> new SolutionNotFound(id));

        deniedAccessIfNotResourceOwnerOrAdmin(jwt, s);

        solutionRepository.deleteById(id);

        mongoSolutionRepository.findBySolutionIdAndUserId(id, s.getUserId())
                .ifPresent(mongoSolutionRepository::delete);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "completedTasks", key = "#userId")
    public List<CompletedTaskView> getMyCompletedTasks(UUID userId) {
        List<UserTaskSummary> summaries = solutionRepository.findAllUserTaskSummaries(userId);

        if (summaries.isEmpty()) {
            return List.of();
        }

        List<UUID> taskIds = summaries.stream()
                .map(UserTaskSummary::taskId)
                .toList();

        Map<UUID, TaskShortView> taskMap = taskClient.getBatch(taskIds);

        List<SolutionEntity> all = solutionRepository.findAllByUserIdAndTaskIdIn(userId, taskIds);

        Map<UUID, List<SolutionShortView>> byTaskId = all.stream()
                .collect(Collectors.groupingBy(
                        SolutionEntity::getTaskId,
                        Collectors.mapping(solutionMapper::toShortView, Collectors.toList())));

        return summaries.stream()
                .map(s -> CompletedTaskView.builder()
                        .task(taskMap.get(s.taskId()))
                        .solutions(byTaskId.getOrDefault(s.taskId(), List.of()))
                        .lastSolvedAt(s.lastSolvedAt())
                        .attemptsCount((int) s.attemptsCount())
                        .build())
                .toList();
    }

    @Transactional
    @CacheEvict(value = "solutions", key = "#result.solutionId()")
    public void onExecutionResult(CodeExecutionResult result) {
        UUID solutionId = result.solutionId();
        log.info("Got execution result solution={} status={}", solutionId, result.status());

        SolutionEntity solution = solutionRepository.findById(solutionId)
                .orElseThrow(() -> new SolutionNotFound(solutionId));

        var completedTasksCache = cacheManager.getCache("completedTasks");
        if (completedTasksCache != null) {
            completedTasksCache.evict(solution.getUserId());
        }

        if (FINAL_STATUSES.contains(solution.getStatus())) {
            log.warn("Solution {} already in final status {}, skipping duplicate", solutionId, solution.getStatus());
            return;
        }

        if (solution.getStatus() == result.status() &&
                Objects.equals(solution.getPassedTests(), result.passedTests())) {
            log.info("Duplicate identical result for solution {}, skipping", solutionId);
            return;
        }

        log.info("Solution difficulty: {}, language: {}, points: {}",
                solution.getDifficulty(), solution.getLanguage(), solution.getPoints());

        solution.setStatus(result.status());
        solution.setPassedTests(result.passedTests());
        solution.setTotalTests(result.totalTests());
        solution.setTotalTimeMs(result.totalTimeMs());
        solution.setTestResults(result.testResults());
        solutionRepository.save(solution);

        if (FINAL_STATUSES.contains(result.status())) {
            try {
                String statsPayload = objectMapper.writeValueAsString(
                        TaskSubmissionEvent.builder()
                                .solutionId(solutionId)
                                .taskId(solution.getTaskId())
                                .userId(solution.getUserId())
                                .completed(result.status() == SolutionStatus.COMPLETED)
                                .timestamp(Instant.now())
                                .points(solution.getPoints())
                                .language(solution.getLanguage())
                                .difficulty(solution.getDifficulty())
                                .build()
                );
                outboxEventRepository.save(OutboxEvent.builder()
                        .topic(KafkaTopics.TOPIC_TASK_STATS)
                        .aggregateId(solutionId)
                        .messageKey(solution.getUserId())
                        .status(OutboxEventStatus.NEW)
                        .eventType("TASK_SUBMISSION")
                        .payload(statsPayload)
                        .build());
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize TaskSubmissionEvent", e);
            }
        }

        if (result.status() == SolutionStatus.COMPLETED) {
            try {
                String plagPayload = objectMapper.writeValueAsString(
                        SolutionCompletedEvent.builder()
                                .solutionId(solutionId)
                                .taskId(solution.getTaskId())
                                .userId(solution.getUserId())
                                .language(solution.getLanguage())
                                .build()
                );
                outboxEventRepository.save(OutboxEvent.builder()
                        .topic(KafkaTopics.TOPIC_SOLUTION_COMPLETED)
                        .aggregateId(solutionId)
                        .messageKey(solutionId)
                        .status(OutboxEventStatus.NEW)
                        .eventType("SOLUTION_COMPLETED")
                        .payload(plagPayload)
                        .build());
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize SolutionCompletedEvent", e);
            }
        }
    }

    private void saveSolutionSource(UUID solutionId, UUID taskId, UUID userId, SolutionRequest dto) {
        try {
            mongoSolutionRepository.save(SolutionSource.builder()
                    .solutionId(solutionId)
                    .taskId(taskId)
                    .userId(userId)
                    .language(dto.language())
                    .code(dto.code())
                    .codeHash(solutionUtil.sha256(dto.code()))
                    .normalizedCode(solutionUtil.normalize(dto.code()))
                    .build());

            log.debug("Solution source saved to Mongo for solution {}", solutionId);
        } catch (Exception e) {
            log.error("Failed to save solution source to Mongo for solution {}. " +
                    "Solution exists in Postgres and outbox event was created.", solutionId, e);
        }
    }

    private void deniedAccessIfNotResourceOwnerOrAdmin(Jwt jwt, SolutionEntity s) {
        if (!securityUtil.isOwner(jwt, s.getUserId()) && !securityUtil.isAdmin(jwt)) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
    }
}
