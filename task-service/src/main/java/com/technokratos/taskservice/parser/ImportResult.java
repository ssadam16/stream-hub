package com.technokratos.taskservice.parser;

import lombok.Builder;

@Builder
public record ImportResult(
        int imported,    // сколько задач импортировано
        int updated,     // сколько обновлено (по externalId)
        String message   // OK или текст ошибки
) {}
