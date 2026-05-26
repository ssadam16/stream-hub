package com.technokratos.solutionservice.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.technokratos.common.enums.OutboxEventStatus;
import com.technokratos.common.enums.SolutionStatus;
import com.technokratos.common.events.CodeExecutionResult;
import com.technokratos.common.events.SolutionCompletedEvent;
import com.technokratos.common.events.TaskSubmissionEvent;
import com.technokratos.common.handler.exceptions.SolutionNotFound;
import com.technokratos.common.topics.KafkaTopics;
import com.technokratos.solutionservice.model.OutboxEvent;
import com.technokratos.solutionservice.model.SolutionEntity;
import com.technokratos.solutionservice.repository.OutboxEventRepository;
import com.technokratos.solutionservice.repository.SolutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class CodeExecutionConsumer {


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

    private final ObjectMapper objectMapper;

    private final OutboxEventRepository outboxEventRepository;

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

}
