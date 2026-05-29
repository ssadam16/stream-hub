package com.technokratos.solutionservice.consumer;

import com.technokratos.common.events.CodeExecutionResult;
import com.technokratos.common.topics.KafkaTopics;
import com.technokratos.solutionservice.service.SolutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CodeExecutionConsumer {

    private final SolutionService solutionService;

    @KafkaListener(topics = KafkaTopics.TOPIC_EXECUTION_RESULTS, groupId = "${spring.kafka.consumer.group-id}")
    public void onExecutionResult(CodeExecutionResult result) {
        solutionService.onExecutionResult(result);
    }
}
