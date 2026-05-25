package com.technokratos.userservice.service;

import com.technokratos.common.dto.PageResponse;
import com.technokratos.userservice.dto.AccountInfoResponse;
import com.technokratos.userservice.dto.AccountResponse;
import com.technokratos.userservice.exception.AccountNotFound;
import com.technokratos.userservice.mapper.AccountMapper;
import com.technokratos.userservice.model.AccountEntity;
import com.technokratos.userservice.model.UserDifficultyStatsEntity;
import com.technokratos.userservice.model.UserLanguageStatsEntity;
import com.technokratos.userservice.repository.AccountRepository;
import com.technokratos.userservice.repository.UserDifficultyStatsRepository;
import com.technokratos.userservice.repository.UserLanguageStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserDifficultyStatsRepository difficultyStatsRepository;
    private final UserLanguageStatsRepository languageStatsRepository;
    private final AccountMapper accountMapper;

    @Transactional
    public AccountResponse getMyAccount(Jwt jwt) {
        UUID accountId = UUID.fromString(jwt.getSubject());

        AccountEntity account = accountRepository.findByAccountId(accountId)
                .orElseGet(() -> {
                    accountRepository.insert(getAccountInfo(jwt, accountId));
                    return accountRepository.findByAccountId(accountId).orElseThrow();
                });

        List<UserDifficultyStatsEntity> difficultyStats = difficultyStatsRepository.findByAccountId(accountId);
        List<UserLanguageStatsEntity> languageStats = languageStatsRepository.findByAccountId(accountId);

        return accountMapper.toAccountResponse(account, difficultyStats, languageStats);
    }

    public AccountResponse getUserAccount(UUID accountId) {
        log.info("Getting account with id {}", accountId);

        AccountEntity account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new AccountNotFound(accountId));

        List<UserDifficultyStatsEntity> difficultyStats = difficultyStatsRepository.findByAccountId(accountId);
        List<UserLanguageStatsEntity> languageStats = languageStatsRepository.findByAccountId(accountId);

        return accountMapper.toAccountResponse(account,  difficultyStats, languageStats);
    }

    private AccountInfoResponse getAccountInfo(Jwt jwt, UUID accountId) {
        return new AccountInfoResponse(
                accountId,
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("name")
        );
    }

    public PageResponse<AccountInfoResponse> leaderboard(int page, int size) {
        return accountRepository.getTopByRating(page, size);
    }
}