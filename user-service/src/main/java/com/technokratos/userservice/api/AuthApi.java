package com.technokratos.userservice.api;

import com.technokratos.userservice.api.dto.request.TokenValidationRequest;
import com.technokratos.userservice.api.dto.request.UserAuthRequest;
import com.technokratos.userservice.api.dto.response.TokenValidationResponse;
import com.technokratos.userservice.api.dto.response.UserAuthResponse;
import com.technokratos.userservice.api.dto.response.UserResponse;
import com.technokratos.userservice.handler.exceptions.ExceptionMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/auth/token")
public interface AuthApi {

    @PostMapping
    @Operation(summary = "Аунтефикация с получением токена")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно аунтефицирован в системе",
                    content = @Content(schema = @Schema(implementation = UserAuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Ошибка аунтефикации",
                    content = @Content(schema = @Schema(implementation = AuthenticationException.class))),
    })
    ResponseEntity<UserAuthResponse> getToken(@RequestBody UserAuthRequest request);

    @PostMapping("/validate")
    @Operation(summary = "Проверка токена на валидность")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Токен валидный",
                    content = @Content(schema = @Schema(implementation = TokenValidationResponse.class))),
            @ApiResponse(responseCode = "401", description = "Токен не валидный",
                    content = @Content(schema = @Schema(implementation = TokenValidationResponse.class))),
    })
    ResponseEntity<TokenValidationResponse> validateToken(@RequestBody TokenValidationRequest request);
}
