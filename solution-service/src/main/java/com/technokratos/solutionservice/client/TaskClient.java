package com.technokratos.solutionservice.client;

import com.technokratos.common.dto.TaskAdminView;
import com.technokratos.common.dto.TaskShortView;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TaskClient {

    private final RestClient taskRestClient;

    /** Fetches task with hidden tests included — used to build executor payload. */
    public TaskAdminView getForExecution(UUID taskId) {
        return taskRestClient.get()
                .uri("/internal/v1/tasks/{id}", taskId)
                .retrieve()
                .body(TaskAdminView.class);
    }

    public Map<UUID, TaskShortView> getBatch(List<UUID> taskIds) {
        return taskRestClient.post()
                .uri("/internal/v1/tasks/batch")
                .body(taskIds)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
