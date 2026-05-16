package com.technokratos.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class MdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String traceId = request.getHeader("X-Trace-Id");
            String userId  = request.getHeader("X-User-Id");
            String solutionId = request.getHeader("X-Solution-Id");

            MDC.put("traceId", traceId != null ? traceId : UUID.randomUUID().toString());
            MDC.put("spanId", UUID.randomUUID().toString());
            if (userId != null) MDC.put("userId", userId);
            if (solutionId != null) MDC.put("solutionId", solutionId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}