package com.technokratos.taskservice.mapper;

import com.technokratos.common.dto.TagDto;
import com.technokratos.taskservice.model.Tag;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class TagMapper {

    public TagDto toDto(Tag tag) {
        return TagDto.builder()
                .id(tag.getId())
                .name(tag.getName())
                .slug(tag.getSlug())
                .category(tag.getCategory())
                .build();
    }

    public List<TagDto> toDtoList(List<Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        return tags.stream().map(this::toDto).toList();
    }
}
