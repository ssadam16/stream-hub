package com.technokratos.taskservice.api.controllers;

import com.technokratos.common.exceptions.ExceptionMessage;
import com.technokratos.common.exceptions.ValidationExceptionMessage;
import com.technokratos.taskservice.api.dto.request.TaskCreatingRequest;
import com.technokratos.taskservice.api.dto.response.TaskResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/tasks")
public interface TaskApi {

    @PostMapping
    @Operation(summary = "Создание задачи")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Задача успешно создана",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации данных",
                    content = @Content(schema = @Schema(implementation =  ValidationExceptionMessage.class))),

            @ApiResponse(responseCode = "404", description = "Ресурс не найден",
                    content = @Content(schema = @Schema(implementation = ExceptionMessage.class))),

            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                    content = @Content(schema = @Schema(implementation = ExceptionMessage.class)))
    })
    ResponseEntity<TaskResponse> createTask(@RequestBody @Valid TaskCreatingRequest request);

    @GetMapping
    @Operation(summary = "Получение всех задач")
    ResponseEntity<List<TaskResponse>> getAllTasks();

    @GetMapping("/{id}")
    @Operation(summary = "Получение задачи по id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "Задача не найдена",
                    content = @Content(schema = @Schema(implementation = ExceptionMessage.class))),
            @ApiResponse(responseCode = "200", description = "Задача найдена",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
    })
    ResponseEntity<TaskResponse> getTaskById(@PathVariable UUID id);
}
