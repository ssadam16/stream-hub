package com.technokratos.userservice.service;

import com.technokratos.common.dto.PageResponse;
import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.userservice.dto.AccountInfoResponse;
import com.technokratos.userservice.dto.AccountResponse;
import com.technokratos.userservice.exception.AccountNotFound;
import com.technokratos.userservice.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@Transactional
@Tag("integration")
@DisplayName("Account Service Integration Tests")
class AccountServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("userdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID userId;
    private Jwt mockJwt;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockJwt = createMockJwt(userId.toString());
        jdbcTemplate.execute("DELETE FROM account");
    }

    @Test
    @DisplayName("Should create account on first getMyAccount")
    void shouldCreateAccountOnFirstGetMyAccount() {
        AccountResponse response = accountService.getMyAccount(mockJwt);

        assertThat(response).isNotNull();
        assertThat(response.accountId()).isEqualTo(userId);
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.fio()).isEqualTo("Test User");
        assertThat(response.totalSubmissions()).isZero();
    }

    @Test
    @DisplayName("Should return existing account without recreation")
    void shouldReturnExistingAccount() {
        accountService.getMyAccount(mockJwt);

        AccountResponse response = accountService.getMyAccount(mockJwt);

        assertThat(response).isNotNull();
        assertThat(response.accountId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should return user account by ID")
    void shouldReturnUserAccountById() {
        accountService.getMyAccount(mockJwt);

        AccountResponse response = accountService.getUserAccount(userId);

        assertThat(response).isNotNull();
        assertThat(response.accountId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should throw AccountNotFound for non-existent user")
    void shouldThrowForNonExistentUser() {
        UUID nonExistentId = UUID.randomUUID();

        assertThatThrownBy(() -> accountService.getUserAccount(nonExistentId))
                .isInstanceOf(AccountNotFound.class)
                .hasMessageContaining(nonExistentId.toString());
    }

    @Test
    @DisplayName("Should return leaderboard with correct pagination")
    void shouldReturnLeaderboardWithPagination() {
        for (int i = 0; i < 15; i++) {
            UUID id = UUID.randomUUID();
            accountRepository.insert(id);
            accountRepository.updateStats(id, true, true, 100 * (i + 1));
        }

        PageResponse<AccountInfoResponse> firstPage = accountService.leaderboard(0, 10);
        PageResponse<AccountInfoResponse> secondPage = accountService.leaderboard(1, 10);

        assertThat(firstPage.content()).hasSize(10);
        assertThat(secondPage.content()).hasSize(5);
        assertThat(firstPage.totalElements()).isGreaterThanOrEqualTo(15);
    }

    @Test
    @DisplayName("Should return empty leaderboard when no users")
    void shouldReturnEmptyLeaderboard() {
        PageResponse<AccountInfoResponse> response = accountService.leaderboard(0, 10);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }

    @Test
    @DisplayName("Should include stats in account response")
    void shouldIncludeStatsInAccountResponse() {
        accountService.getMyAccount(mockJwt);

        accountRepository.updateStats(userId, true, true, 100);

        AccountResponse response = accountService.getMyAccount(mockJwt);

        assertThat(response.totalSubmissions()).isEqualTo(1);
        assertThat(response.acceptedSubmissions()).isEqualTo(1);
        assertThat(response.solvedTasksCount()).isEqualTo(1);
        assertThat(response.totalPoints()).isEqualTo(100);
        assertThat(response.acceptance()).isEqualTo(100.0);
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