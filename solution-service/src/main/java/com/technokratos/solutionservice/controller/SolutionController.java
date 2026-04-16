package com.technokratos.solutionservice.controller;

import com.technokratos.common.annotations.CurrentUser;
import com.technokratos.common.dto.CurrentUserInfo;
import com.technokratos.solutionservice.dto.SolutionRequest;
import com.technokratos.solutionservice.dto.SolutionResponse;
import com.technokratos.solutionservice.service.SolutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/solutions")
@RequiredArgsConstructor
public class SolutionController {

    private final SolutionService solutionService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<SolutionResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return solutionService.getAll(PageRequest.of(page, size));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOne(@PathVariable UUID id) {
        solutionService.deleteOne(id);
    }

    @GetMapping("/{id}")
    public SolutionResponse getOne(@PathVariable UUID id) {
        return solutionService.getOne(id);
    }

    @PostMapping("/{taskId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public UUID solveTask(@RequestBody @Valid SolutionRequest dto, @CurrentUser CurrentUserInfo user,
                          @PathVariable UUID taskId) {
        return solutionService.solveTask(dto, user, taskId);
    }
}

