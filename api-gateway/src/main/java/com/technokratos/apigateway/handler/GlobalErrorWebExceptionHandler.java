// api-gateway/handler/GlobalErrorWebExceptionHandler.java
package com.technokratos.apigateway.handler;

import com.technokratos.apigateway.dto.ErrorResponse;
import com.technokratos.apigateway.handler.exception.GatewayException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class GlobalErrorWebExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ErrorResponse error = buildErrorResponse(exchange, ex);

        if (error.status() >= 500) {
            log.error("Server error on {}: {}", exchange.getRequest().getURI(), ex.getMessage(), ex);
        } else {
            log.warn("Client error on {}: {} - {}",
                    exchange.getRequest().getURI(), error.code(), error.message());
        }

        exchange.getResponse().setStatusCode(HttpStatus.valueOf(error.status()));
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

    private ErrorResponse buildErrorResponse(ServerWebExchange exchange, Throwable ex) {
        String path = exchange.getRequest().getURI().getPath();
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        Instant now = Instant.now();

        if (ex instanceof GatewayException e) {
            return ErrorResponse.builder()
                    .timestamp(now)
                    .status(e.getStatus().value())
                    .error(e.getStatus().name())
                    .code(e.getCode())
                    .message(e.getMessage())
                    .path(path)
                    .traceId(traceId)
                    .build();
        }

        if (ex instanceof WebClientRequestException) {
            return ErrorResponse.builder()
                    .timestamp(now)
                    .status(503)
                    .error("SERVICE_UNAVAILABLE")
                    .code("AUTH_SERVICE_UNAVAILABLE")
                    .message("Authentication service is temporarily unavailable")
                    .path(path)
                    .traceId(traceId)
                    .build();
        }

        // 2. Ошибки от Keycloak/downstream через WebClient
        if (ex instanceof WebClientResponseException e) {
            return mapWebClientException(e, path, traceId, now);
        }

        // 3. Spring Security
        if (ex instanceof InvalidBearerTokenException) {
            return ErrorResponse.builder()
                    .timestamp(now)
                    .status(401)
                    .error("UNAUTHORIZED")
                    .code("INVALID_TOKEN")
                    .message("Access token is invalid or expired")
                    .path(path)
                    .traceId(traceId)
                    .build();
        }

        if (ex instanceof AuthenticationException) {
            return ErrorResponse.builder()
                    .timestamp(now)
                    .status(401)
                    .error("UNAUTHORIZED")
                    .code("AUTHENTICATION_FAILED")
                    .message("Authentication required")
                    .path(path)
                    .traceId(traceId)
                    .build();
        }

        if (ex instanceof AccessDeniedException) {
            return ErrorResponse.builder()
                    .timestamp(now)
                    .status(403)
                    .error("FORBIDDEN")
                    .code("ACCESS_DENIED")
                    .message("You don't have permission to access this resource")
                    .path(path)
                    .traceId(traceId)
                    .build();
        }

        // 4. Валидация
        if (ex instanceof WebExchangeBindException e) {
            List<ErrorResponse.FieldError> fieldErrors = e.getFieldErrors().stream()
                    .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                    .toList();

            return ErrorResponse.builder()
                    .timestamp(now)
                    .status(400)
                    .error("BAD_REQUEST")
                    .code("VALIDATION_FAILED")
                    .message("Request validation failed")
                    .path(path)
                    .traceId(traceId)
                    .errors(fieldErrors)
                    .build();
        }

        // 5. Spring дефолтное ResponseStatusException (404 от роутинга и т.д.)
        if (ex instanceof ResponseStatusException e) {
            return ErrorResponse.builder()
                    .timestamp(now)
                    .status(e.getStatusCode().value())
                    .error(HttpStatus.valueOf(e.getStatusCode().value()).name())
                    .code(mapStatusToCode(e.getStatusCode().value()))
                    .message(e.getReason() != null ? e.getReason() : "Request failed")
                    .path(path)
                    .traceId(traceId)
                    .build();
        }

        // 6. Всё остальное - 500
        return ErrorResponse.builder()
                .timestamp(now)
                .status(500)
                .error("INTERNAL_SERVER_ERROR")
                .code("INTERNAL_ERROR")
                .message("An unexpected error occurred")
                .path(path)
                .traceId(traceId)
                .build();
    }

    private ErrorResponse mapWebClientException(
            WebClientResponseException e, String path, String traceId, Instant now) {

        int statusCode = e.getStatusCode().value();
        String body = e.getResponseBodyAsString();

        // Парсим тело ответа Keycloak для понятных сообщений
        String keycloakError = extractKeycloakError(body);

        return switch (statusCode) {
            case 400 -> ErrorResponse.builder()
                    .timestamp(now)
                    .status(400)
                    .error("BAD_REQUEST")
                    .code("INVALID_REQUEST")
                    .message(keycloakError != null ? keycloakError : "Invalid request data")
                    .path(path).traceId(traceId).build();

            case 401 -> ErrorResponse.builder()
                    .timestamp(now)
                    .status(401)
                    .error("UNAUTHORIZED")
                    .code("INVALID_CREDENTIALS")
                    .message("Invalid email or password")
                    .path(path).traceId(traceId).build();

            case 403 -> ErrorResponse.builder()
                    .timestamp(now)
                    .status(403)
                    .error("FORBIDDEN")
                    .code("ACCESS_DENIED")
                    .message("Access denied")
                    .path(path).traceId(traceId).build();

            case 404 -> ErrorResponse.builder()
                    .timestamp(now)
                    .status(404)
                    .error("NOT_FOUND")
                    .code("RESOURCE_NOT_FOUND")
                    .message("Resource not found")
                    .path(path).traceId(traceId).build();

            case 409 -> ErrorResponse.builder()
                    .timestamp(now)
                    .status(409)
                    .error("CONFLICT")
                    .code("USER_ALREADY_EXISTS")
                    .message(keycloakError != null ? keycloakError : "User already exists")
                    .path(path).traceId(traceId).build();

            default -> e.getStatusCode().is5xxServerError()
                    ? ErrorResponse.builder()
                    .timestamp(now)
                    .status(503)
                    .error("SERVICE_UNAVAILABLE")
                    .code("AUTH_SERVICE_UNAVAILABLE")
                    .message("Authentication service is temporarily unavailable")
                    .path(path).traceId(traceId).build()
                    : ErrorResponse.builder()
                    .timestamp(now)
                    .status(statusCode)
                    .error("ERROR")
                    .code("EXTERNAL_ERROR")
                    .message("External service error")
                    .path(path).traceId(traceId).build();
        };
    }

    private String extractKeycloakError(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            var node = objectMapper.readTree(body);
            if (node.has("error_description")) return node.get("error_description").asText();
            if (node.has("errorMessage")) return node.get("errorMessage").asText();
            if (node.has("error")) return node.get("error").asText();
        } catch (Exception ignored) {}
        return null;
    }

    private String mapStatusToCode(int status) {
        return switch (status) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 409 -> "CONFLICT";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            case 429 -> "TOO_MANY_REQUESTS";
            default -> status >= 500 ? "INTERNAL_ERROR" : "ERROR";
        };
    }
}