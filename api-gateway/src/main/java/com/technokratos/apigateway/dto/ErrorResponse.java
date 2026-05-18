// api-gateway/dto/ErrorResponse.java
package com.technokratos.apigateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import java.time.Instant;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String traceId,
        List<FieldError> errors
) {
    public record FieldError(String field, String message) {}
}