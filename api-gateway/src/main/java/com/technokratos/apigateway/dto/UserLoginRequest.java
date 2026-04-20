package com.technokratos.apigateway.dto;

public record UserLoginRequest(
        String email,
        String password
) {
}
