package com.technokratos.taskservice.controller;

import com.technokratos.common.dto.TagDto;
import com.technokratos.common.enums.TagCategory;
import com.technokratos.taskservice.api.TagApi;
import com.technokratos.taskservice.dto.request.TagCreateRequest;
import com.technokratos.taskservice.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TagController implements TagApi {

    private final TagService tagService;

    @Override
    public List<TagDto> getAll(TagCategory category) {
        return tagService.getAll(category);
    }

    @Override
    public TagDto getBySlug(String slug) {
        return tagService.getBySlug(slug);
    }

    @Override
    public TagDto getById(UUID id) {
        return tagService.getById(id);
    }

    @Override
    public ResponseEntity<TagDto> create(TagCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tagService.create(request));
    }

    @Override
    public ResponseEntity<Void> delete(UUID id) {
        tagService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
