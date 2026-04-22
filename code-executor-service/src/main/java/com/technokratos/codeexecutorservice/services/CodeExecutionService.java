package com.technokratos.codeexecutorservice.services;

import com.technokratos.common.events.CodeExecutionRequest;
import com.technokratos.common.events.CodeExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
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

            log.info("Code execution completed for solution: {}, passedTests count: {}",
                    event.solutionId(), executionResult.passedTests());

            log.debug("LOGS FROM CONTAINER:\n {}", executionResult);

            kafkaTemplate.send("code-execution-results", event.solutionId(), executionResult);
            log.info("Sent execution result to Kafka for solution: {}", event.solutionId());
        } catch (Exception e) {
            log.error("Failed to execute code for solution: {}", event.solutionId(), e);
            // TODO: Отправить сообщение об ошибке
        }

    }
}
