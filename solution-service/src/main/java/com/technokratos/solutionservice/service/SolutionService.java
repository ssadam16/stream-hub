package com.technokratos.solutionservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.technokratos.common.dto.*;
import com.technokratos.common.dto.tests.TestCaseFull;
import com.technokratos.common.dto.tests.TestCasePublic;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class SolutionService {

    private final SecurityUtil securityUtil;

    private static final Set<SolutionStatus> FINAL_STATUSES = Set.of(
            SolutionStatus.COMPLETED,
            SolutionStatus.WRONG_ANSWER,
            SolutionStatus.TIME_LIMIT_EXCEEDED,
            SolutionStatus.MEMORY_LIMIT_EXCEEDED,
            SolutionStatus.COMPILATION_ERROR,
            SolutionStatus.RUNTIME_ERROR,
            SolutionStatus.INTERNAL_ERROR
    );

    private final SolutionRepository solutionRepository;
    private final MongoSolutionRepository mongoSolutionRepository;
    private final SolutionMapper solutionMapper;
    private final TaskClient taskClient;

    private final KafkaTemplate<UUID, CodeExecutionRequest> kafkaExecution;
    private final KafkaTemplate<UUID, TaskSubmissionEvent> kafkaStats;
    private final KafkaTemplate<UUID, SolutionCompletedEvent> kafkaPlagiarism;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
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
                            .testCases(toExecutorTests(task.testCases()))
                            .build()
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize CodeExecutionRequests ", e);
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

        saveSolutionSourceAsync(solutionId, taskId, userId, dto);

        log.info("Solution {} submitted and execution requested via outbox", solutionId);
        return solutionId;
    }

    @KafkaListener(topics = KafkaTopics.TOPIC_EXECUTION_RESULTS, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onExecutionResult(CodeExecutionResult result) {
        UUID solutionId = result.solutionId();
        log.info("Got execution result solution={} status={}", solutionId, result.status());

        SolutionEntity solution = solutionRepository.findById(solutionId)
                .orElseThrow(() -> new SolutionNotFound(solutionId));

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

    @Transactional(readOnly = true)
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
    public void delete(UUID id, Jwt jwt) {
        SolutionEntity s = solutionRepository.findById(id)
                .orElseThrow(() -> new SolutionNotFound(id));

        deniedAccessIfNotResourceOwnerOrAdmin(jwt, s);

        solutionRepository.deleteById(id);

        mongoSolutionRepository.findBySolutionIdAndUserId(id, s.getUserId())
                .ifPresent(mongoSolutionRepository::delete);
    }

    @Transactional(readOnly = true)
    public PageResponse<CompletedTaskView> getMyCompletedTasks(UUID userId, Pageable pageable) {

        Page<UserTaskSummary> summaries = solutionRepository.findUserTaskSummaries(userId, pageable);

        if (summaries.isEmpty()) {
            return new PageResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0, 0);
        }

        List<UUID> taskIds = summaries.getContent().stream()
                .map(UserTaskSummary::taskId).toList();

        Map<UUID, TaskShortView> taskMap = taskClient.getBatch(taskIds);

        List<SolutionEntity> all = solutionRepository.findAllByUserIdAndTaskIdIn(userId, taskIds);

        Map<UUID, List<SolutionShortView>> byTaskId = all.stream()
                .collect(Collectors.groupingBy(
                        SolutionEntity::getTaskId,
                        Collectors.mapping(solutionMapper::toShortView, Collectors.toList())));

        List<CompletedTaskView> content = summaries.getContent().stream()
                .map(s -> CompletedTaskView.builder()
                        .task(taskMap.get(s.taskId()))
                        .solutions(byTaskId.getOrDefault(s.taskId(), List.of()))
                        .lastSolvedAt(s.lastSolvedAt())
                        .attemptsCount((int) s.attemptsCount())
                        .build())
                .toList();

        return new PageResponse<>(
                content,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                summaries.getTotalElements(),
                summaries.getTotalPages()
        );
    }

    private void saveSolutionSourceAsync(UUID solutionId, UUID taskId, UUID userId, SolutionRequest dto) {
        try {
            mongoSolutionRepository.save(SolutionSource.builder()
                    .solutionId(solutionId)
                    .taskId(taskId)
                    .userId(userId)
                    .language(dto.language())
                    .code(dto.code())
                    .codeHash(sha256(dto.code()))
                    .normalizedCode(normalize(dto.code()))
                    .build());

            log.debug("Solution source saved to Mongo for solution {}", solutionId);

        } catch (Exception e) {
            log.error("Failed to save solution source to Mongo for solution {}. " +
                    "Solution exists in Postgres and outbox event was created.", solutionId, e);
            // Можно добавить метрику или отправить в отдельный outbox для повторной попытки
        }
    }

    private static List<TestCasePublic> toExecutorTests(List<TestCaseFull> cases) {
        if (cases == null || cases.isEmpty()) {
            return List.of();
        }
        return cases.stream()
                .map(c -> TestCasePublic.builder()
                        .id(c.id())
                        .input(c.input())
                        .expectedOutput(c.expectedOutput())
                        .build())
                .toList();
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static String normalize(String code) {
        return code
                .replaceAll("//[^\n]*", "")
                .replaceAll("/\\*.*?\\*/", "")
                .replaceAll("#[^\n]*", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void deniedAccessIfNotResourceOwnerOrAdmin(Jwt jwt, SolutionEntity s) {
        if (!securityUtil.isOwner(jwt, s.getUserId()) && !securityUtil.isAdmin(jwt)) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
    }
}
