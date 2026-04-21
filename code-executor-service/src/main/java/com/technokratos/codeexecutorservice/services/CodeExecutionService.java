package com.technokratos.codeexecutorservice.services;

import com.technokratos.common.dto.tests.ConsoleTestCaseResponse;
import com.technokratos.common.events.SolutionCreationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeExecutionService {

    private final DockerExecutionService dockerExecutionService;

    @KafkaListener(groupId = "${spring.kafka.consumer.group-id}", topics = "code-execution-topic")
    public void executeCode(SolutionCreationEvent event) {
        log.info("Received solution execution event: solutionId={}, taskId={}, language={}",
                event.solutionId(), event.taskId(), event.language());

        try {

            String executionResult = dockerExecutionService.executeCode(event.solutionId(), event.code(), event.language());


            log.info("Code execution completed for solution: {}, result length: {}",
                    event.solutionId(), executionResult.length());

            log.debug("LOGS FROM CONTAINER:\n {}", executionResult);

            // TODO: Отправить результат обратно в Kafka
            // TODO: Обновить статус решения в БД через отдельный сервис
        } catch (Exception e) {
            log.error("Failed to execute code for solution: {}", event.solutionId(), e);
            // TODO: Отправить сообщение об ошибке
        }

    }
}
