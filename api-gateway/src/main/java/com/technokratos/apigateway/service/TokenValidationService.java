package com.technokratos.apigateway.service;

import com.technokratos.common.dto.TokenValidationRequest;
import com.technokratos.common.dto.TokenValidationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenValidationService {

    private final WebClient webClient;

    public Mono<TokenValidationResponse> validateToken(String token) {
        log.info("Validating token: {}", token);

        TokenValidationRequest request = new TokenValidationRequest(token);

        return webClient.post()
                .uri("/api/auth/token/validate")
                .bodyValue(request)
                .exchangeToMono(response -> response.bodyToMono(TokenValidationResponse.class));
    }
}