package com.technokratos.common.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SolutionStatus {
    ACCEPTED("Решение ожидает выполнения"),
    PROCESSING("Решение выполняется"),

    // Успешные статусы
    COMPLETED("Решение выполнено успешно"),
    WRONG_ANSWER("Решение выполнено частично (не все тесты пройдены)"),

    // Ошибки выполнения
    COMPILATION_ERROR("Ошибка компиляции"),
    RUNTIME_ERROR("Ошибка во время выполнения"),
    TIME_LIMIT_EXCEEDED("Превышен лимит времени"),
    MEMORY_LIMIT_EXCEEDED("Превышен лимит памяти"),

    // Системные ошибки
    INTERNAL_ERROR("Внутренняя ошибка сервера"),
    TIMEOUT("Таймаут выполнения");

    private final String description;
}
