package com.technokratos.taskservice.api;

import com.technokratos.common.handler.exceptions.ExceptionMessage;
import com.technokratos.common.handler.exceptions.ValidationExceptionMessage;
import com.technokratos.taskservice.parser.ImportResult;
import com.technokratos.taskservice.request.TaskCreatingRequest;
import com.technokratos.common.dto.TaskResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/tasks")
public interface AdminManagementTaskApi {

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
    @ResponseStatus(HttpStatus.CREATED)
    TaskResponse createTask(@RequestBody @Valid TaskCreatingRequest request);

    @GetMapping
    @Operation(summary = "Получение всех задач")
    @ResponseStatus(HttpStatus.OK)
    List<TaskResponse> getAllTasks();

    @GetMapping("/{id}")
    @Operation(summary = "Получение задачи по id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "Задача не найдена",
                    content = @Content(schema = @Schema(implementation = ExceptionMessage.class))),
            @ApiResponse(responseCode = "200", description = "Задача найдена",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
    })
    @ResponseStatus(HttpStatus.OK)
    TaskResponse getTaskById(@PathVariable UUID id);

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ImportResult> importTasks(
            @RequestParam("file") MultipartFile file, @RequestHeader("X-User-Id") UUID userId);
}
