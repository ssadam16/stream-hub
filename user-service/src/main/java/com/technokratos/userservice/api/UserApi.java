package com.technokratos.userservice.api;

import com.technokratos.common.exceptions.ExceptionMessage;
import com.technokratos.common.exceptions.ValidationExceptionMessage;
import com.technokratos.userservice.api.dto.request.UserRegisterRequest;
import com.technokratos.userservice.api.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/users")
public interface UserApi {

    @PostMapping
    @Operation(summary = "Создание пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно создан",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),

            @ApiResponse(responseCode = "400", description = "Ошибка валидации данных",
                    content = @Content(schema = @Schema(implementation =  ValidationExceptionMessage.class))),

            @ApiResponse(responseCode = "404", description = "Ресурс не найден",
                    content = @Content(schema = @Schema(implementation = ExceptionMessage.class))),

            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                    content = @Content(schema = @Schema(implementation = ExceptionMessage.class)))
    })
    ResponseEntity<UserResponse> createUser(@RequestBody @Valid UserRegisterRequest request);

    @GetMapping
    @Operation(summary = "Получение всех пользователей")
    ResponseEntity<List<UserResponse>> getAllUsers();

    @GetMapping("/{id}")
    @Operation(summary = "Получение пользователя по id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                    content = @Content(schema = @Schema(implementation = ExceptionMessage.class))),
            @ApiResponse(responseCode = "200", description = "Пользователь найден",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
    })
    ResponseEntity<UserResponse> getUserById(@PathVariable UUID id);
}
