package com.technokratos.userservice.controller;

import com.technokratos.common.dto.PageResponse;
import com.technokratos.userservice.api.StatisticApi;
import com.technokratos.userservice.dto.AccountInfoResponse;
import com.technokratos.userservice.dto.AccountResponse;
import com.technokratos.userservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class StatisticController implements StatisticApi {

    private final AccountService accountService;

    @Override
    public AccountResponse profile(Jwt jwt) {
        return accountService.getMyAccount(jwt);
    }

    @Override
    public AccountResponse getUserProfile(UUID accountId) {
        return accountService.getUserAccount(accountId);
    }

    @Override
    public PageResponse<AccountInfoResponse> leaderboard(int page, int size) {
        return accountService.leaderboard(page, size);
    }
}
