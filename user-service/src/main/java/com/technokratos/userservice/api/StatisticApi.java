package com.technokratos.userservice.api;

import com.technokratos.common.dto.PageResponse;
import com.technokratos.userservice.dto.AccountInfoResponse;
import com.technokratos.userservice.dto.AccountResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@RequestMapping("/api/v1/statistics")
public interface StatisticApi {

    @GetMapping("/me")
    AccountResponse profile(@AuthenticationPrincipal Jwt jwt);

    @GetMapping("/account/{accountId}")
    AccountResponse getUserProfile(@PathVariable UUID accountId);

    @GetMapping("/leaderboard")
    PageResponse<AccountInfoResponse> leaderboard(
            @RequestParam(defaultValue = "0", required = false) @Min(0) int page,
            @RequestParam(defaultValue = "10", required = false) @Min(1) @Max(100) int size
    );
}
