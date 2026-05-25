package com.technokratos.taskservice.service;

import com.technokratos.common.events.TaskSubmissionEvent;
import com.technokratos.common.topics.KafkaTopics;
import com.technokratos.taskservice.repository.TaskUserStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskStatisticsListener {

    private final TaskUserStatsRepository repository;

    @KafkaListener(
            topics = KafkaTopics.TOPIC_TASK_STATS,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onSubmission(TaskSubmissionEvent event) {
        log.info("Received submission: taskId={} userId={} completed={}",
                event.taskId(), event.userId(), event.completed());

        repository.upsert(event.taskId(), event.userId(), event.completed());
    }
}
