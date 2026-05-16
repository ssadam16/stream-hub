package com.technokratos.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class UserContextFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(auth -> {
                    Jwt jwt = auth.getToken();

                    String userId = jwt.getSubject();
                    String email = jwt.getClaimAsString("email");
                    String roles = extractRoles(jwt);

                    log.debug("Propagating user context: userId={}, roles={}", userId, roles);

                    ServerHttpRequest mutated = exchange.getRequest().mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Email", email != null ? email : "")
                            .header("X-User-Roles", roles)
                            .build();

                    return exchange.mutate().request(mutated).build();
                })
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    private String extractRoles(Jwt jwt) {
        // У тебя роли в resource_access.gateway.roles
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null && resourceAccess.containsKey("gateway")) {
            Map<String, Object> gateway = (Map<String, Object>) resourceAccess.get("gateway");
            List<String> roles = (List<String>) gateway.get("roles");
            if (roles != null) {
                return String.join(",", roles);
            }
        }
        return "";
    }

    @Override
    public int getOrder() {
        return -1; // раньше роутинга
    }
}
