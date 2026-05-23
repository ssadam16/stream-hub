package com.technokratos.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskStatus {
    DRAFT("Черновик, виден только админам"),
    PUBLISHED("Опубликована, видна всем"),
    ARCHIVED("Архив, скрыта из общих списков");

    private final String description;
}
