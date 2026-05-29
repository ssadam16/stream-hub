package com.technokratos.apigateway.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class RateLimiterConfig {

    //TODO: не работает как будто, доделать
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .flatMap(principal -> {
                    log.info("Principal : {}", principal);
                    if (principal instanceof Jwt token) {
                        log.info("Jwt token : {}", token);
                        String userId = token.getSubject();
                        return Mono.just(userId);
                    }
                    log.info("Principal is not jwt : {}", principal);
                    return Mono.just("anonymous");
                });
    }
}