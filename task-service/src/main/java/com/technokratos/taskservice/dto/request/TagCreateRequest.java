package com.technokratos.taskservice.dto.request;

import com.technokratos.common.enums.TagCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TagCreateRequest(
        @NotBlank(message = "Tag name is required")
        @Size(min = 2, max = 64, message = "Tag name must be between 2 and 64 characters")
        String name,

        @NotNull(message = "Tag category is required")
        TagCategory category
) {}
