package com.technokratos.solutionservice.scheduling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.technokratos.common.enums.OutboxEventStatus;
import com.technokratos.common.events.CodeExecutionRequest;
import com.technokratos.common.events.SolutionCompletedEvent;
import com.technokratos.common.events.TaskSubmissionEvent;
import com.technokratos.solutionservice.model.OutboxEvent;
import com.technokratos.solutionservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<UUID, Object> kafkaTemplate; // универсальный

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void publishEvents() {
        List<OutboxEvent> events = outboxEventRepository
            .findTop100ByStatusOrderByCreatedAt(OutboxEventStatus.NEW, Instant.now());
        
        for (OutboxEvent event : events) {
            try {
                // Определяем топик и отправляем
                Object payload = deserializePayload(event);
                kafkaTemplate.send(event.getTopic(), event.getMessageKey(), payload)
                    .get(5, TimeUnit.SECONDS);
                
                event.setStatus(OutboxEventStatus.PROCESSED);
                event.setPublishedAt(Instant.now());
                outboxEventRepository.save(event);
                log.info("Outbox event {} sent to {}", event.getId(), event.getTopic());
                
            } catch (Exception e) {
                log.error("Failed to send outbox event {}: {}", event.getId(), e.getMessage());
                int retryCount = event.getRetryCount() + 1;
                event.setRetryCount(retryCount);
                long backoffSec = (long) Math.pow(2, retryCount);
                event.setNextRetryAt(Instant.now().plusSeconds(backoffSec));

                if (event.getRetryCount() > 5) {
                    event.setStatus(OutboxEventStatus.FAILED);
                }
                outboxEventRepository.save(event);
            }
        }
    }

    @Scheduled(cron = "0 0 3 * * *") // каждую ночь в 3:00
    @Transactional
    public void cleanProcessedEvents() {
        Instant threshold = Instant.now().minus(7, ChronoUnit.DAYS);
        int deleted = outboxEventRepository.deleteByStatusAndPublishedAtBefore(
                OutboxEventStatus.PROCESSED, threshold
        );
        log.info("Cleaned {} processed outbox events older than 7 days", deleted);
    }

    private Object deserializePayload(OutboxEvent event) throws Exception {
        return switch (event.getEventType()) {
            case "CODE_EXECUTION_REQUEST" ->
                    objectMapper.readValue(event.getPayload(), CodeExecutionRequest.class);
            case "TASK_SUBMISSION" ->
                    objectMapper.readValue(event.getPayload(), TaskSubmissionEvent.class);
            case "SOLUTION_COMPLETED" ->
                    objectMapper.readValue(event.getPayload(), SolutionCompletedEvent.class);
            default -> throw new IllegalArgumentException("Unknown: " + event.getEventType());
        };
    }
}