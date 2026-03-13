package com.technokratos.apigateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.technokratos.apigateway.dto.ErrorJwtResponse;
import com.technokratos.apigateway.service.TokenValidationService;
import com.technokratos.common.dto.TokenValidationResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

@Component
@Slf4j
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final TokenValidationService tokenValidationService;
    private final ObjectMapper objectMapper;
    private static final String exceptionName = "UnauthorizedException";

    public JwtAuthenticationFilter(TokenValidationService tokenValidationService, ObjectMapper objectMapper) {
        super(Config.class);
        this.tokenValidationService = tokenValidationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();
            String path = request.getURI().getPath();

            log.info("Processing request for path: {}", path);

            if (isPublicPath(path, config.getPublicPaths())) {
                log.info("Public path: {}, passing through", path);
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst("Authorization");

            if (Objects.isNull(authHeader) || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or invalid Authorization header for path: {}", path);
                return unauthorized(null, response, "The token is either missing or not a Bearer");
            }

            String token = authHeader.substring(7);
            log.debug("Token extracted for validation");

            return tokenValidationService.validateToken(token)
                    .flatMap(validationResponse -> {
                        if (validationResponse.isValid()) {
                            log.info("Token is valid for user: {}", validationResponse.email());

                            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                                    .header("X-User-Email", validationResponse.email())
                                    .header("X-User-Roles", String.join(",", validationResponse.roles()))
                                    .header("X-User-Id", validationResponse.userId())
                                    .build();

                            return chain.filter(exchange.mutate()
                                    .request(modifiedRequest)
                                    .build());
                        } else {
                            log.warn("Token is invalid: {}", validationResponse.error());
                            return unauthorized(validationResponse, response, null);
                        }
                    });
        };
    }

    private Mono<Void> unauthorized(TokenValidationResponse validationResponse,
                                    ServerHttpResponse response,
                                    String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);

        String errorMessage = message != null ? message : validationResponse.error();

        ErrorJwtResponse unauthorizedResponse = ErrorJwtResponse.builder()
                .exceptionName(exceptionName)
                .message(errorMessage)
                .build();

        try {
            return response.writeWith(
                    Mono.just(response.bufferFactory()
                            .wrap(objectMapper.writeValueAsBytes(unauthorizedResponse)))
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize exceptionName response", e);
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return response.writeWith(Mono.just(
                    response.bufferFactory().wrap("{\"exceptionName\":\"Internal Server Error\"}".getBytes())
            ));
        }
    }

    private boolean isPublicPath(String path, List<String> publicPaths) {
        if (publicPaths == null || publicPaths.isEmpty()) {
            return false;
        }

        return publicPaths.stream()
                .anyMatch(pattern -> {
                    if (pattern.endsWith("/**")) {
                        String basePath = pattern.substring(0, pattern.length() - 3);
                        return path.startsWith(basePath);
                    }
                    return path.equals(pattern);
                });
    }

    @Getter
    @Setter
    public static class Config {
        private List<String> publicPaths;
    }
}