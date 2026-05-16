package com.technokratos.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();

        ServerHttpRequest request = exchange.getRequest();

        return chain.filter(exchange).doFinally(signal -> {
           long duration = System.currentTimeMillis() - startTime;
           int status = exchange.getResponse().getStatusCode() != null
                   ? exchange.getResponse().getStatusCode().value()
                   : 0;

            log.info("method={} path={} status={} duration={}ms traceId={}",
                    request.getMethod(),
                    request.getURI().getPath(),
                    status,
                    duration,
                    request.getHeaders().getFirst("X-Trace-Id")
            );
        });

    }

    @Override
    public int getOrder() {
        return 0;
    }
}
