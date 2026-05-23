package com.technokratos.taskservice.service;

import com.technokratos.common.events.TaskSubmissionEvent;
import com.technokratos.taskservice.exception.TaskNotFoundException;
import com.technokratos.taskservice.model.Task;
import com.technokratos.taskservice.model.TaskUserStats;
import com.technokratos.taskservice.model.TaskUserStatsId;
import com.technokratos.taskservice.repository.TaskRepository;
import com.technokratos.taskservice.repository.TaskUserStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskStatisticsListener {

    private final TaskUserStatsRepository repository;

    @KafkaListener(
            topics = "task.statistics.events",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onSubmission(TaskSubmissionEvent event) {
        log.info("Received submission: taskId={} userId={} accepted={}",
                event.taskId(), event.userId(), event.accepted());

        repository.upsert(event.taskId(), event.userId(), event.accepted());
    }
}
