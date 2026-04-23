package com.technokratos.solutionservice.client;

import com.technokratos.common.dto.TaskResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TaskClient {

    private final RestClient restClient;

    public TaskResponse getTask(UUID taskId) {
        return restClient.get()
                .uri("/{id}", taskId)
                .retrieve()
                .body(TaskResponse.class);
    }
}
