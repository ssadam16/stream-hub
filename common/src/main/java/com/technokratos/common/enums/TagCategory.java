package com.technokratos.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TagCategory {
    TOPIC("Topic"),
    DATA_STRUCTURE("Data Structure"),
    TECHNIQUE("Technique");

    private final String displayName;
}
