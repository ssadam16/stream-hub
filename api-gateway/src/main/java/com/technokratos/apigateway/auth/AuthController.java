package com.technokratos.apigateway.auth;

import com.technokratos.apigateway.dto.*;
import com.technokratos.apigateway.service.KeycloakService;
import jakarta.validation.Valid;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;
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
    public Mono<AuthResponse> login(@Valid @RequestBody UserLoginRequest request) {
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
    public Mono<ResponseEntity<RegisterResponse>> register(@Valid @RequestBody UserRegisterRequest request) {
        log.info("Register attempt for email: {}", request.email());

        return keycloakService.createKeycloakUser(request)
                .flatMap(keycloakId -> {
                    log.debug("Keycloak Id: {}", keycloakId);
                    return keycloakService.createUserInUserService(request, keycloakId);
                })
                .map(user -> ResponseEntity.status(HttpStatus.CREATED).body(user))
                .doOnError(WebClientResponseException.class, e ->
                        log.warn("Registration failed: status={}, body={}",
                        e.getStatusCode(), e.getResponseBodyAsString())
                );
    }

    @PostMapping("/refresh")
    public Mono<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
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