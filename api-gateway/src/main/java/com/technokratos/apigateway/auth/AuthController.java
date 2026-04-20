package com.technokratos.apigateway.auth;

import com.technokratos.apigateway.dto.*;
import com.technokratos.apigateway.service.KeycloakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final WebClient keyCloakWebClient;

    private final KeycloakService keycloakService;

    @PostMapping("/token")
    public Mono<AuthResponse> login(@RequestBody UserLoginRequest request) {
        log.info("Login attempt for email: {}", request.email());

        return keyCloakWebClient.post()
                .uri("/realms/myrealm/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(keycloakService.buildForm(request)))
                .retrieve()
                .bodyToMono(Map.class)
                .map(keycloakService::toAuthResponse);
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<RegisterResponse>> register(@RequestBody UserRegisterRequest request) {
        log.info("Register attempt for email: {}", request.email());

        return keycloakService.createKeycloakUser(request)
                .flatMap(keycloakId -> {
                    log.debug("Keycloak Id: {}", keycloakId);
                    return keycloakService.createUserInUserService(request, keycloakId);
                })
                .map(user -> ResponseEntity.status(HttpStatus.CREATED).body(user))
                .onErrorResume(e -> {
                    log.error("Registration failed: {}", e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    @PostMapping("/refresh")
    public Mono<AuthResponse> refresh(@RequestBody RefreshRequest request) {
        log.info("Refresh token request");

        return keyCloakWebClient.post()
                .uri("/realms/myrealm/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(keycloakService.buildForm(request)))
                .retrieve()
                .bodyToMono(Map.class)
                .map(keycloakService::toAuthResponse);
    }
}