package com.technokratos.apigateway.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.technokratos.apigateway.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        String path = exchange.getRequest().getURI().getPath();
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(401)
                .error("UNAUTHORIZED")
                .code("MISSING_OR_INVALID_TOKEN")
                .message(ex != null ? ex.getMessage() : "No token provided or token is invalid")
                .path(path)
                .build();

        log.warn("Authentication failed for {}: {}", path, error.message());

        return writeResponse(exchange, error);
    }

    private Mono<Void> writeResponse(ServerWebExchange exchange, ErrorResponse error) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(error);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Failed to write error response", e);
            return Mono.error(e);
        }
    }
}