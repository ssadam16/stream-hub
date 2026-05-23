package com.technokratos.taskservice.api;

import com.technokratos.common.dto.TagDto;
import com.technokratos.common.enums.TagCategory;
import com.technokratos.taskservice.dto.request.TagCreateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Tags API", description = "Read-only tag listing for users; mutations require ADMIN")
@RequestMapping("/api/v1/tags")
public interface TagApi {

    @Operation(summary = "Get all tags (optionally filtered by category)")
    @GetMapping
    List<TagDto> getAll(@RequestParam(value = "category", required = false) TagCategory category);

    @Operation(summary = "Get tag by slug")
    @GetMapping("/slug/{slug}")
    TagDto getBySlug(@PathVariable String slug);

    @Operation(summary = "Get tag by id")
    @GetMapping("/{id}")
    TagDto getById(@PathVariable UUID id);

    @Operation(summary = "[ADMIN] Create new tag")
    @PostMapping
    ResponseEntity<TagDto> create(@Valid @RequestBody TagCreateRequest request);

    @Operation(summary = "[ADMIN] Delete tag")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id);
}
