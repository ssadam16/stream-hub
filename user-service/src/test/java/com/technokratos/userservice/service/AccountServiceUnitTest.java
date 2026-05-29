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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Account Service Unit Tests")
class AccountServiceUnitTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserDifficultyStatsRepository difficultyStatsRepository;

    @Mock
    private UserLanguageStatsRepository languageStatsRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountService accountService;

    private UUID userId;
    private Jwt mockJwt;
    private AccountEntity accountEntity;
    private AccountResponse accountResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockJwt = createMockJwt(userId.toString());

        accountEntity = AccountEntity.builder()
                .accountId(userId)
                .username("testuser")
                .email("test@example.com")
                .fio("Test User")
                .totalSubmissions(10)
                .completedSubmissions(7)
                .solvedTasksCount(5)
                .totalPoints(500)
                .build();

        accountResponse = AccountResponse.builder()
                .accountId(userId)
                .username("testuser")
                .build();
    }

    @Nested
    @DisplayName("getMyAccount")
    class GetMyAccount {

        @Test
        @DisplayName("Should return existing account")
        void shouldReturnExistingAccount() {
            when(accountRepository.findByAccountId(userId)).thenReturn(Optional.of(accountEntity));
            when(difficultyStatsRepository.findByAccountId(userId)).thenReturn(List.of());
            when(languageStatsRepository.findByAccountId(userId)).thenReturn(List.of());
            when(accountMapper.toAccountResponse(accountEntity, List.of(), List.of()))
                    .thenReturn(accountResponse);

            AccountResponse result = accountService.getMyAccount(mockJwt);

            assertThat(result).isEqualTo(accountResponse);
            verify(accountRepository, never()).insert(any(AccountInfoResponse.class));
        }

        @Test
        @DisplayName("Should create new account when not exists")
        void shouldCreateNewAccountWhenNotExists() {
            when(accountRepository.findByAccountId(userId))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(accountEntity));
            when(difficultyStatsRepository.findByAccountId(userId)).thenReturn(List.of());
            when(languageStatsRepository.findByAccountId(userId)).thenReturn(List.of());
            when(accountMapper.toAccountResponse(accountEntity, List.of(), List.of()))
                    .thenReturn(accountResponse);

            AccountResponse result = accountService.getMyAccount(mockJwt);

            assertThat(result).isEqualTo(accountResponse);
            verify(accountRepository).insert(any(AccountInfoResponse.class));
        }
    }

    @Nested
    @DisplayName("getUserAccount")
    class GetUserAccount {

        @Test
        @DisplayName("Should return account when exists")
        void shouldReturnAccountWhenExists() {
            when(accountRepository.findByAccountId(userId)).thenReturn(Optional.of(accountEntity));
            when(difficultyStatsRepository.findByAccountId(userId)).thenReturn(List.of());
            when(languageStatsRepository.findByAccountId(userId)).thenReturn(List.of());
            when(accountMapper.toAccountResponse(accountEntity, List.of(), List.of()))
                    .thenReturn(accountResponse);

            AccountResponse result = accountService.getUserAccount(userId);

            assertThat(result).isEqualTo(accountResponse);
        }

        @Test
        @DisplayName("Should throw when account not found")
        void shouldThrowWhenAccountNotFound() {
            when(accountRepository.findByAccountId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.getUserAccount(userId))
                    .isInstanceOf(AccountNotFound.class);
        }
    }

    @Nested
    @DisplayName("leaderboard")
    class Leaderboard {

        @Test
        @DisplayName("Should return page response")
        void shouldReturnPageResponse() {
            PageResponse<AccountInfoResponse> expected = new PageResponse<>(List.of(), 0, 10, 0, 0);
            when(accountRepository.getTopByRating(0, 10)).thenReturn(expected);

            PageResponse<AccountInfoResponse> result = accountService.leaderboard(0, 10);

            assertThat(result).isEqualTo(expected);
        }
    }

    private Jwt createMockJwt(String userId) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", userId)
                .claim("preferred_username", "testuser")
                .claim("email", "test@example.com")
                .claim("name", "Test User")
                .build();
    }
}