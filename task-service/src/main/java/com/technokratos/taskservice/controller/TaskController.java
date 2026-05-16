package com.technokratos.taskservice.controller;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.common.enums.TaskTestType;
import com.technokratos.taskservice.api.TaskApi;
import com.technokratos.taskservice.parser.*;
import com.technokratos.taskservice.request.TaskCreatingRequest;
import com.technokratos.common.dto.TaskResponse;
import com.technokratos.taskservice.request.TaskFilterRequest;
import com.technokratos.taskservice.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TaskController implements TaskApi {

    private final TaskService taskService;
    private final FileParserFactory fileParserFactory;
    private final TaskImportService taskImportService;

    @Override
    public TaskResponse createTask(TaskCreatingRequest request) {
        return taskService.create(request);
    }

    @Override
    public Page<TaskResponse> getAllTasks(@RequestParam(required = false) Set<TaskDifficulty> difficulty,
                                          @RequestParam(required = false) Set<ProgrammingLanguage> language,
                                          @RequestParam(required = false) UUID authorId,
                                          @RequestParam(required = false) Set<TaskTestType> type,
                                          @RequestParam(required = false) Short minRating,
                                          @RequestParam(required = false) Short maxRating,
                                          @RequestParam(required = false) String search,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(defaultValue = "createdAt") String sortBy,
                                          @RequestParam(defaultValue = "desc") String sortType) {

        return taskService.getTasksByFilters(TaskFilterRequest.builder()
                .difficulties(difficulty)
                .languages(language)
                .authorId(authorId)
                .types(type)
                .minRating(minRating)
                .maxRating(maxRating)
                .searchText(search)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortType(sortType)
                .build()
        );
    }

    @Override
    public TaskResponse getTaskById(UUID id) {
        return taskService.getById(id);
    }

    @Override
    public ResponseEntity<ImportResult> importTasks(MultipartFile file, UUID userId) {
        FileParser parser = fileParserFactory.getParser(Objects.requireNonNull(file.getOriginalFilename()));

        List<TaskImportRequest> tasks;

        try {
            tasks = parser.parse(file);
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(new ImportResult(0, 0, "Failed to parse file: " + e.getMessage()));
        }

        ImportResult result = taskImportService.importTasks(tasks, userId);
        return ResponseEntity.ok(result);
    }


}
