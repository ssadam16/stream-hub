package com.technokratos.userservice.service;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.common.events.TaskSubmissionEvent;
import com.technokratos.userservice.model.AccountEntity;
import com.technokratos.userservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
@Tag("integration")
@ActiveProfiles("test")
@DisplayName("User Statistic Service Integration Tests")
class UserStatisticServiceIntegrationTest {

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
    private UserStatisticService userStatisticService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private SolvedTaskRepository solvedTaskRepository;

    @Autowired
    private UserDifficultyStatsRepository difficultyStatsRepository;

    @Autowired
    private UserLanguageStatsRepository languageStatsRepository;

    private UUID userId;
    private UUID taskId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should create new user on first submission")
    void shouldCreateNewUserOnFirstSubmission() {
        TaskSubmissionEvent event = TaskSubmissionEvent.builder()
                .solutionId(UUID.randomUUID())
                .taskId(taskId)
                .userId(userId)
                .completed(false)
                .timestamp(Instant.now())
                .points(0)
                .language(ProgrammingLanguage.JAVA)
                .difficulty(TaskDifficulty.EASY)
                .build();

        userStatisticService.applySubmission(event);

        AccountEntity account = accountRepository.findByAccountId(userId).orElseThrow();
        assertThat(account).isNotNull();
        assertThat(account.getTotalSubmissions()).isEqualTo(1);
        assertThat(account.getCompletedSubmissions()).isEqualTo(0);
        assertThat(account.getSolvedTasksCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should increment submission count on failed attempt")
    void shouldIncrementSubmissionCountOnFailed() {
        TaskSubmissionEvent firstEvent = TaskSubmissionEvent.builder()
                .solutionId(UUID.randomUUID())
                .taskId(taskId)
                .userId(userId)
                .completed(false)
                .timestamp(Instant.now())
                .points(0)
                .language(ProgrammingLanguage.JAVA)
                .difficulty(TaskDifficulty.EASY)
                .build();
        userStatisticService.applySubmission(firstEvent);

        TaskSubmissionEvent secondEvent = TaskSubmissionEvent.builder()
                .solutionId(UUID.randomUUID())
                .taskId(taskId)
                .userId(userId)
                .completed(false)
                .timestamp(Instant.now())
                .points(0)
                .language(ProgrammingLanguage.JAVA)
                .difficulty(TaskDifficulty.EASY)
                .build();
        userStatisticService.applySubmission(secondEvent);

        AccountEntity account = accountRepository.findByAccountId(userId).orElseThrow();
        assertThat(account.getTotalSubmissions()).isEqualTo(2);
        assertThat(account.getCompletedSubmissions()).isEqualTo(0);
        assertThat(account.getSolvedTasksCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should increment solved count only on first successful submission")
    void shouldIncrementSolvedCountOnlyOnFirstSuccess() {
        TaskSubmissionEvent firstEvent = TaskSubmissionEvent.builder()
                .solutionId(UUID.randomUUID())
                .taskId(taskId)
                .userId(userId)
                .completed(true)
                .timestamp(Instant.now())
                .points(100)
                .language(ProgrammingLanguage.JAVA)
                .difficulty(TaskDifficulty.MEDIUM)
                .build();
        userStatisticService.applySubmission(firstEvent);

        TaskSubmissionEvent secondEvent = TaskSubmissionEvent.builder()
                .solutionId(UUID.randomUUID())
                .taskId(taskId)
                .userId(userId)
                .completed(true)
                .timestamp(Instant.now())
                .points(100)
                .language(ProgrammingLanguage.JAVA)
                .difficulty(TaskDifficulty.MEDIUM)
                .build();
        userStatisticService.applySubmission(secondEvent);

        AccountEntity account = accountRepository.findByAccountId(userId).orElseThrow();
        assertThat(account.getTotalSubmissions()).isEqualTo(2);
        assertThat(account.getCompletedSubmissions()).isEqualTo(2);
        assertThat(account.getSolvedTasksCount()).isEqualTo(1);
        assertThat(account.getTotalPoints()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should update difficulty stats on first solve")
    void shouldUpdateDifficultyStatsOnFirstSolve() {
        TaskSubmissionEvent event = TaskSubmissionEvent.builder()
                .solutionId(UUID.randomUUID())
                .taskId(taskId)
                .userId(userId)
                .completed(true)
                .timestamp(Instant.now())
                .points(100)
                .language(ProgrammingLanguage.JAVA)
                .difficulty(TaskDifficulty.HARD)
                .build();

        userStatisticService.applySubmission(event);

        var difficultyStats = difficultyStatsRepository.findByAccountId(userId);
        assertThat(difficultyStats).hasSize(1);
        assertThat(difficultyStats.get(0).getDifficulty()).isEqualTo(TaskDifficulty.HARD);
        assertThat(difficultyStats.get(0).getSolvedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should update language stats on first solve")
    void shouldUpdateLanguageStatsOnFirstSolve() {
        TaskSubmissionEvent event = TaskSubmissionEvent.builder()
                .solutionId(UUID.randomUUID())
                .taskId(taskId)
                .userId(userId)
                .completed(true)
                .timestamp(Instant.now())
                .points(100)
                .language(ProgrammingLanguage.PYTHON)
                .difficulty(TaskDifficulty.EASY)
                .build();

        userStatisticService.applySubmission(event);

        var languageStats = languageStatsRepository.findByAccountId(userId);
        assertThat(languageStats).hasSize(1);
        assertThat(languageStats.get(0).getLanguage()).isEqualTo(ProgrammingLanguage.PYTHON);
        assertThat(languageStats.get(0).getSolvedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should not update difficulty/language stats on failed submission")
    void shouldNotUpdateStatsOnFailedSubmission() {
        TaskSubmissionEvent event = TaskSubmissionEvent.builder()
                .solutionId(UUID.randomUUID())
                .taskId(taskId)
                .userId(userId)
                .completed(false)
                .timestamp(Instant.now())
                .points(0)
                .language(ProgrammingLanguage.JAVA)
                .difficulty(TaskDifficulty.EASY)
                .build();

        userStatisticService.applySubmission(event);

        assertThat(difficultyStatsRepository.findByAccountId(userId)).isEmpty();
        assertThat(languageStatsRepository.findByAccountId(userId)).isEmpty();
    }

    @Test
    @DisplayName("Should increment difficulty count for second solve of same difficulty")
    void shouldIncrementDifficultyCountForSecondSolve() {
        UUID taskId2 = UUID.randomUUID();

        TaskSubmissionEvent firstEvent = TaskSubmissionEvent.builder()
                .solutionId(UUID.randomUUID())
                .taskId(taskId)
                .userId(userId)
                .completed(true)
                .timestamp(Instant.now())
                .points(100)
                .language(ProgrammingLanguage.JAVA)
                .difficulty(TaskDifficulty.HARD)
                .build();
        userStatisticService.applySubmission(firstEvent);

        TaskSubmissionEvent secondEvent = TaskSubmissionEvent.builder()
                .solutionId(UUID.randomUUID())
                .taskId(taskId2)
                .userId(userId)
                .completed(true)
                .timestamp(Instant.now())
                .points(150)
                .language(ProgrammingLanguage.JAVA)
                .difficulty(TaskDifficulty.HARD)
                .build();
        userStatisticService.applySubmission(secondEvent);

        var difficultyStats = difficultyStatsRepository.findByAccountId(userId);
        assertThat(difficultyStats).hasSize(1);
        assertThat(difficultyStats.get(0).getSolvedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("CRITICAL: Should not process duplicate events")
    void shouldNotProcessDuplicateEvents() {
        UUID solutionId = UUID.randomUUID();
        TaskSubmissionEvent event = TaskSubmissionEvent.builder()
                .solutionId(solutionId)
                .taskId(taskId)
                .userId(userId)
                .completed(true)
                .timestamp(Instant.now())
                .points(100)
                .language(ProgrammingLanguage.JAVA)
                .difficulty(TaskDifficulty.EASY)
                .build();

        userStatisticService.applySubmission(event);

        userStatisticService.applySubmission(event);

        AccountEntity account = accountRepository.findByAccountId(userId).orElseThrow();
        assertThat(account.getTotalSubmissions()).isEqualTo(1);
        assertThat(account.getSolvedTasksCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should update streak correctly")
    void shouldUpdateStreakCorrectly() {
        TaskSubmissionEvent event = TaskSubmissionEvent.builder()
                .solutionId(UUID.randomUUID())
                .taskId(taskId)
                .userId(userId)
                .completed(true)
                .timestamp(Instant.now())
                .points(100)
                .language(ProgrammingLanguage.JAVA)
                .difficulty(TaskDifficulty.EASY)
                .build();

        userStatisticService.applySubmission(event);

        AccountEntity account = accountRepository.findByAccountId(userId).orElseThrow();
        assertThat(account.getCurrentStreak()).isEqualTo(1);
        assertThat(account.getLastActive()).isNotNull();
    }
}