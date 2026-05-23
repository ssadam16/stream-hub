package com.technokratos.common.dto;

import com.technokratos.common.enums.TagCategory;
import lombok.Builder;

import java.util.UUID;

@Builder
public record TagDto(
        UUID id,
        String name,
        String slug,
        TagCategory category
) {}
