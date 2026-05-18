package com.technokratos.apigateway.service;

import com.technokratos.apigateway.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakService {

    private final WebClient keyCloakWebClient;
    private String cachedAdminToken;
    private Instant tokenExpiry;
    @Value("${keycloak.client-secret}")
    private String clientSecret;
    private final WebClient userServiceWebClient;

    public MultiValueMap<String, String> buildForm(RefreshRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", "gateway");
        form.add("refresh_token", request.refreshToken());
        form.add("client_secret", clientSecret);

        return form;
    }

    public MultiValueMap<String, String> buildForm(UserLoginRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "gateway");
        form.add("username", request.email());
        form.add("password", request.password());
        form.add("client_secret", clientSecret);

        return form;
    }

    public AuthResponse toAuthResponse(Map<String, Object> response) {
        return AuthResponse.builder()
                .accessToken((String) response.get("access_token"))
                .refreshToken((String) response.get("refresh_token"))
                .expiresIn((Integer) response.get("expires_in"))
                .tokenType((String) response.get("token_type"))
                .build();
    }

    public Mono<String> createKeycloakUser(UserRegisterRequest request) {
        return getAdminAccessToken()
                .flatMap(accessToken ->
                        // 1. Создаём пользователя
                        keyCloakWebClient.post()
                                .uri("/admin/realms/myrealm/users")
                                .header("Authorization", "Bearer " + accessToken)
                                .bodyValue(Map.of(
                                        "username", request.username(),
                                        "email", request.email(),
                                        "enabled", true,
                                        "emailVerified", true,
                                        "credentials", List.of(Map.of(
                                                "type", "password",
                                                "value", request.password(),
                                                "temporary", false
                                        ))
                                ))
                                .retrieve()
                                .toBodilessEntity()
                                .flatMap(response -> {
                                    // 2. Получаем ID созданного пользователя
                                    String location = Objects.requireNonNull(response.getHeaders().getFirst("Location"), "Location header missing");
                                    String keycloakId = location.substring(location.lastIndexOf("/") + 1);
                                    log.debug("Location: {}, keycloakId: {}", location, keycloakId);

                                    // 3. Назначаем роль USER
                                    return assignRealmRole(keycloakId)
                                            .thenReturn(keycloakId);
                                })
                );
    }

    private Mono<Void> assignRealmRole(String keycloakId) {
        return getAdminAccessToken()
                .flatMap(token ->
                        keyCloakWebClient.get()
                                .uri("/admin/realms/myrealm/roles/{roleName}", "USER")
                                .header("Authorization", "Bearer " + token)
                                .retrieve()
                                .bodyToMono(Map.class)
                                .flatMap(role -> {
                                            String roleId = (String) role.get("id");

                                            return keyCloakWebClient.post()
                                                    .uri("/admin/realms/myrealm/users/{id}/role-mappings/realm", keycloakId)
                                                    .header("Authorization", "Bearer " + token)
                                                    .bodyValue(List.of(Map.of(
                                                            "id", roleId,
                                                            "name", "USER"
                                                    )))
                                                    .retrieve()
                                                    .toBodilessEntity()
                                                    .then();

                                        }
                                )
                );
    }

    public Mono<RegisterResponse> createUserInUserService(UserRegisterRequest request, String keycloakId) {
        return userServiceWebClient.post()
                .uri("/api/users")
                .bodyValue(Map.of(
                        "username", request.username(),
                        "email", request.email(),
                        "keycloakId", keycloakId
                ))
                .retrieve()
                .bodyToMono(RegisterResponse.class)
                .onErrorResume(e -> deleteKeycloakUser(keycloakId).then(Mono.error(e)));
    }

    private Mono<Void> deleteKeycloakUser(String keycloakId) {
        return getAdminAccessToken()
                .flatMap(token -> keyCloakWebClient.delete()
                        .uri("/admin/realms/myrealm/users/{id}", keycloakId)
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .toBodilessEntity()
                        .then());
    }

    public Mono<String> getAdminAccessToken() {

        if (cachedAdminToken != null && tokenExpiry != null && tokenExpiry.isAfter(Instant.now())) {
            log.debug("Using cached admin token");
            return Mono.just(cachedAdminToken);
        }

        return keyCloakWebClient.post()
                .uri("/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("client_id=admin-cli&username=admin&password=admin&grant_type=password")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    String token = (String) response.get("access_token");
                    Integer expiresIn = (Integer) response.get("expires_in");
                    if (token != null) {
                        cachedAdminToken = token;
                        tokenExpiry = Instant.now().plus(expiresIn - 30L, ChronoUnit.SECONDS);
                        log.debug("Token has been cached");
                        return cachedAdminToken;
                    }
                    log.error("Token is null");
                    return null;
                });
    }
}
