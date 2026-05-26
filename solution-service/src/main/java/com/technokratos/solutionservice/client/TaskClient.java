package com.technokratos.solutionservice.client;

import com.technokratos.common.dto.TaskAdminView;
import com.technokratos.common.dto.TaskShortView;
import com.technokratos.common.handler.exceptions.TaskNotAvailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskClient {

    private final RestClient taskRestClient;

    @CircuitBreaker(name = "defaultCircuitBreaker", fallbackMethod = "circuitBreakerFallback")
    @Retry(name = "defaultRetry", fallbackMethod = "retryFallback")
    public TaskAdminView getForExecution(UUID taskId) {
        return taskRestClient.get()
                .uri("/internal/v1/tasks/{id}", taskId)
                .retrieve()
                .body(TaskAdminView.class);
    }


    @CircuitBreaker(name = "defaultCircuitBreaker", fallbackMethod = "getBatchFallback")
    @Retry(name = "defaultRetry", fallbackMethod = "getBatchFallback")
    public Map<UUID, TaskShortView> getBatch(List<UUID> taskIds) {
        return taskRestClient.post()
                .uri("/internal/v1/tasks/batch")
                .body(taskIds)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    private Map<UUID, TaskShortView> getBatchFallback(List<UUID> taskIds, Throwable e) {
        log.warn("Task service unavailable for batch request, using fallback: {}", taskIds, e);
        Map<UUID, TaskShortView> fallbackMap = new HashMap<>();
        for (UUID id : taskIds) {
            fallbackMap.put(id, TaskShortView.builder()
                    .id(id)
                    .title("Задача временно недоступна")
                    .difficulty(null)
                    .build());
        }
        return fallbackMap;
    }


    private TaskAdminView retryFallback(UUID taskId, Throwable t) {
        log.error("Retry exhausted for task {}, cannot execute. Error: {}", taskId, t.getMessage());
        throw new TaskNotAvailableException("Cannot fetch task " + taskId + " for execution. Service unavailable.", t);
    }

    private TaskAdminView circuitBreakerFallback(UUID taskId, Throwable t) {
        log.error("Circuit breaker is OPEN for task {}. Error: {}", taskId, t.getMessage());

        throw new TaskNotAvailableException("Service temporarily unavailable. Circuit breaker open.", t);
    }
}
