package com.technokratos.common.enums;

public enum ErrorType {
    OK,                // тест прошёл
    WRONG_ANSWER,      // вывод не совпал с expectedOutput
    TIMEOUT,           // не уложился в timeLimit
    RUNTIME_ERROR,     // программа крашнулась (exit code != 0)
    MEMORY_LIMIT       // превысил memoryLimit (пока не реализовано)
}