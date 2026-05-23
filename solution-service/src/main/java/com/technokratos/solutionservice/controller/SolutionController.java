package com.technokratos.solutionservice.controller;

import com.technokratos.common.dto.CompletedTaskView;
import com.technokratos.common.dto.PageResponse;
import com.technokratos.common.dto.SolutionShortView;
import com.technokratos.common.dto.SolutionView;
import com.technokratos.solutionservice.api.SolutionApi;
import com.technokratos.solutionservice.dto.SolutionRequest;
import com.technokratos.solutionservice.service.SolutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SolutionController implements SolutionApi {

    private final SolutionService service;

    @Override
    public UUID submit(SolutionRequest dto, UUID taskId, Jwt jwt) {
        return service.submit(dto, taskId, UUID.fromString(jwt.getSubject()));
    }

    @Override
    public PageResponse<SolutionShortView> listMine(Pageable pageable, Jwt jwt) {
        return service.listMine(UUID.fromString(jwt.getSubject()), pageable);
    }

    @Override
    public PageResponse<CompletedTaskView> myCompleted(Pageable pageable, Jwt jwt) {
        return service.getMyCompletedTasks(UUID.fromString(jwt.getSubject()), pageable);
    }

    @Override
    public SolutionView getById(UUID id, Jwt jwt) {
        return service.getById(id, jwt);
    }

    @Override
    public void delete(UUID solutionId, Jwt jwt) {
        service.delete(solutionId, jwt);
    }
    //getAllCompletedSolutionsByTaskId
}
