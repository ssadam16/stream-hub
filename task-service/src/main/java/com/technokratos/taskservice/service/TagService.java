package com.technokratos.taskservice.service;

import com.technokratos.common.dto.TagDto;
import com.technokratos.common.enums.TagCategory;
import com.technokratos.taskservice.dto.request.TagCreateRequest;
import com.technokratos.taskservice.exception.TagAlreadyExistsException;
import com.technokratos.taskservice.exception.TagNotFoundException;
import com.technokratos.taskservice.mapper.TagMapper;
import com.technokratos.taskservice.model.Tag;
import com.technokratos.taskservice.repository.TagRepository;
import com.technokratos.taskservice.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    @Transactional(readOnly = true)
    public List<TagDto> getAll(TagCategory category) {
        return category == null
                ? tagMapper.toDtoList(tagRepository.findAll())
                : tagMapper.toDtoList(tagRepository.findAllByCategory(category));
    }

    @Transactional(readOnly = true)
    public TagDto getById(UUID id) {
        return tagMapper.toDto(tagRepository.findById(id)
                .orElseThrow(() -> TagNotFoundException.byId(id)));
    }

    @Transactional(readOnly = true)
    public TagDto getBySlug(String slug) {
        return tagMapper.toDto(tagRepository.findBySlug(slug)
                .orElseThrow(() -> TagNotFoundException.bySlug(slug)));
    }

    @Transactional
    public TagDto create(TagCreateRequest request) {
        String slug = SlugUtil.slugify(request.name());
        if (tagRepository.existsBySlug(slug)) {
            throw new TagAlreadyExistsException(slug);
        }
        Tag tag = Tag.builder()
                .name(request.name().trim())
                .slug(slug)
                .category(request.category())
                .build();
        Tag saved = tagRepository.save(tag);
        log.info("Created tag id={} slug={}", saved.getId(), saved.getSlug());
        return tagMapper.toDto(saved);
    }

    @Transactional
    public void delete(UUID id) {
        if (!tagRepository.existsById(id)) {
            throw TagNotFoundException.byId(id);
        }
        tagRepository.deleteById(id);
        log.info("Deleted tag id={}", id);
    }
}
