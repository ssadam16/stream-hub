package com.technokratos.apigateway.dto;

public record UserRegisterRequest(
        String username,
        String email,
        String password
) {}

