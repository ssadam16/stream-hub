package com.technokratos.codeexecutorservice.services;

import com.technokratos.codeexecutorservice.exception.TaskExecutionException;
import com.technokratos.common.enums.SolutionStatus;
import com.technokratos.common.events.CodeExecutionRequest;
import com.technokratos.common.events.CodeExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeExecutionService {

    private final DockerExecutionService dockerExecutionService;
    private final KafkaTemplate<UUID, CodeExecutionResult> kafkaTemplate;

    @KafkaListener(groupId = "${spring.kafka.consumer.group-id}", topics = "code-execution-topic")
    public void executeCode(CodeExecutionRequest event) {
        log.info("Received solution execution event: solutionId={}, taskId={}, language={}",
                event.solutionId(), event.taskId(), event.language());

        try {
            CodeExecutionResult executionResult = dockerExecutionService.executeCode(event.solutionId(), event);

            if (executionResult.status() == SolutionStatus.INTERNAL_ERROR) {
                //Уйдет в catch(), так что думаю рантайм итс окей
                throw new RuntimeException("Internal error during execution: " + executionResult.errorMessage());
            }

            log.info("Code execution completed for solution: {}, passedTests count: {}",
                    event.solutionId(), executionResult.passedTests());

            log.debug("LOGS FROM CONTAINER:\n {}", executionResult);

            kafkaTemplate.send("code-execution-results", event.solutionId(), executionResult);
            log.info("Sent execution result to Kafka for solution: {}", event.solutionId());
        } catch (Exception e) {
            log.error("Failed to execute code for solution: {}", event.solutionId(), e);
            throw TaskExecutionException.bySolutionId(event.solutionId(), e);
        }
    }

    public void handleDlqMessage(
            CodeExecutionRequest event,
            @Header(KafkaHeaders.DLT_EXCEPTION_STACKTRACE) String exceptionStackTrace,
            @Header(KafkaHeaders.DLT_ORIGINAL_TOPIC) String originalTopic) {

        log.error("Solution moved to DLQ after all retries: solutionId={}, taskId={}, originalTopic={}", event.solutionId(), event.taskId(), originalTopic);
        log.error("Exception: {}", exceptionStackTrace);

        CodeExecutionResult errorResult = CodeExecutionResult.builder()
                .solutionId(event.solutionId())
                .totalTests(event.testCases().size())
                .passedTests(0)
                .status(SolutionStatus.INTERNAL_ERROR)
                .errorMessage("All retry attempts exhausted. Check DLQ for details")
                .build();

        kafkaTemplate.send("code-execution-results", event.solutionId(), errorResult);
        log.info("Sent INTERNAL_ERROR to results topic for solution: {}", event.solutionId());
    }
}
