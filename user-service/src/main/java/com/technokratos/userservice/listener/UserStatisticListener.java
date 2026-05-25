package com.technokratos.userservice.listener;

import com.technokratos.common.events.TaskSubmissionEvent;
import com.technokratos.common.topics.KafkaTopics;
import com.technokratos.userservice.service.UserStatisticService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserStatisticListener {

    private final UserStatisticService statisticService;

    @KafkaListener(
            topics = KafkaTopics.TOPIC_TASK_STATS,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onSubmission(TaskSubmissionEvent event) {
        log.info("Received submission event: userId={} taskId={} accepted={} points={} language={} difficulty={}",
                event.userId(), event.taskId(), event.completed(), event.points(), event.language(), event.difficulty());
        statisticService.applySubmission(event);
    }
}