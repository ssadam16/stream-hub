package com.technokratos.taskservice.api;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.common.enums.TaskTestType;
import com.technokratos.common.exceptions.ExceptionMessage;
import com.technokratos.common.exceptions.ValidationExceptionMessage;
import com.technokratos.taskservice.parser.ImportResult;
import com.technokratos.taskservice.request.TaskCreatingRequest;
import com.technokratos.common.dto.TaskResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
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
    @ResponseStatus(HttpStatus.CREATED)
    TaskResponse createTask(@RequestBody @Valid TaskCreatingRequest request);

    @GetMapping
    @Operation(
            summary = "Получение всех задач с фильтрацией и пагинацией",
            description = "Возвращает страницу с задачами, отфильтрованными по заданным критериям. " +
                    "Поддерживает поиск по заголовку и условию задачи, фильтрацию по сложности, " +
                    "языку, типу, рейтингу и автору, а также сортировку и пагинацию."
    )
    @ResponseStatus(HttpStatus.OK)
    Page<TaskResponse> getAllTasks(
            @Parameter(description = "Уровни сложности задачи (можно указать несколько)", example = "EASY")
            @RequestParam(required = false) Set<TaskDifficulty> difficulty,

            @Parameter(description = "Языки программирования (можно указать несколько)", example = "JAVA")
            @RequestParam(required = false) Set<ProgrammingLanguage> language,

            @Parameter(description = "ID автора задачи", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam(required = false) UUID authorId,

            @Parameter(description = "Типы тестов (можно указать несколько)", example = "CONSOLE_IO")
            @RequestParam(required = false) Set<TaskTestType> type,

            @Parameter(description = "Минимальный рейтинг (0-5)", example = "3")
            @RequestParam(required = false) Short minRating,

            @Parameter(description = "Максимальный рейтинг (0-5)", example = "5")
            @RequestParam(required = false) Short maxRating,

            @Parameter(description = "Поисковый запрос (поиск в title и condition)", example = "алгоритм")
            @RequestParam(required = false) String search,

            @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Количество элементов на странице", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Поле для сортировки (createdAt, rating, title)", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Направление сортировки (asc, desc)", example = "desc")
            @RequestParam(defaultValue = "desc") String sortType
    );

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
    //@PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ImportResult> importTasks(
            @RequestParam("file") MultipartFile file, @RequestHeader("X-User-Id") UUID userId);


}
