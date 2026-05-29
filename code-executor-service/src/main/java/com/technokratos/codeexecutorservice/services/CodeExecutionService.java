package com.technokratos.codeexecutorservice.services;

import com.technokratos.codeexecutorservice.exception.TaskExecutionException;
import com.technokratos.common.enums.SolutionStatus;
import com.technokratos.common.events.CodeExecutionRequest;
import com.technokratos.common.events.CodeExecutionResult;
import com.technokratos.common.topics.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeExecutionService {

    private final DockerExecutionService dockerExecutionService;
    private final KafkaTemplate<UUID, CodeExecutionResult> kafkaTemplate;

    @KafkaListener(groupId = "${spring.kafka.consumer.group-id}", topics = KafkaTopics.TOPIC_CODE_EXECUTION)
    public void executeCode(CodeExecutionRequest event) {
        log.info("Received solution execution event: solutionId={}, taskId={}, language={}",
                event.solutionId(), event.taskId(), event.language());

        try {
            CodeExecutionResult executionResult = dockerExecutionService.executeCode(event.solutionId(), event);

            if (executionResult.status() == SolutionStatus.INTERNAL_ERROR) {
                throw new RuntimeException("Internal error during execution: " + executionResult.errorMessage());
            }

            log.info("Code execution completed for solution: {}, passedTests count: {}",
                    event.solutionId(), executionResult.passedTests());

            log.debug("LOGS FROM CONTAINER:\n {}", executionResult);

            kafkaTemplate.send(KafkaTopics.TOPIC_EXECUTION_RESULTS, event.solutionId(), executionResult);
            log.info("Sent execution result to Kafka for solution: {}", event.solutionId());
        } catch (Exception e) {
            log.error("Failed to execute code for solution: {}", event.solutionId(), e);
            throw TaskExecutionException.bySolutionId(event.solutionId(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.TOPIC_EXECUTION_DLQ, groupId = "${spring.kafka.consumer.group-id}-dlq")
    public void handleDlqMessage(
            CodeExecutionRequest event,
            @Header(value = "kafka_dlt-exception-stacktrace", required = false) String stacktrace,
            @Header(value = "kafka_dlt-exception-message", required = false) String errorMessage
    ) {
        log.error("DLQ message received: solutionId={}, error={}",
                event.solutionId(), errorMessage);

        // Отправить INTERNAL_ERROR результат чтобы юзер не ждал вечно
        kafkaTemplate.send(KafkaTopics.TOPIC_EXECUTION_RESULTS, event.solutionId(),
                CodeExecutionResult.builder()
                        .solutionId(event.solutionId())
                        .status(SolutionStatus.INTERNAL_ERROR)
                        .passedTests(0)
                        .totalTests(event.testCases() != null ? event.testCases().size() : 0)
                        .testResults(new ArrayList<>())
                        .errorMessage("Execution failed after retries")
                        .build());
    }
}
