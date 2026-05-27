package com.technokratos.solutionservice.service;

import com.technokratos.common.dto.TaskAdminView;
import com.technokratos.common.dto.TaskShortView;
import com.technokratos.common.dto.tests.TestCaseFull;
import com.technokratos.common.enums.*;
import com.technokratos.securityspringbootstarter.SecurityUtil;
import com.technokratos.solutionservice.client.TaskClient;
import com.technokratos.solutionservice.dto.SolutionRequest;
import com.technokratos.solutionservice.model.OutboxEvent;
import com.technokratos.solutionservice.model.SolutionEntity;
import com.technokratos.solutionservice.repository.MongoSolutionRepository;
import com.technokratos.solutionservice.repository.OutboxEventRepository;
import com.technokratos.solutionservice.repository.SolutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@Transactional
@Tag("integration")
@DisplayName("Solution Service Integration Tests")
class SolutionServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("solutionsdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:6.0"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration");
    }

    @MockBean
    private TaskClient taskClient;

    @MockBean
    private SecurityUtil securityUtil;

    @Autowired
    private SolutionService solutionService;

    @Autowired
    private SolutionRepository solutionRepository;

    @Autowired
    private MongoSolutionRepository mongoSolutionRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private UUID userId;
    private UUID taskId;
    private TaskAdminView taskView;
    private Jwt mockJwt;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        mockJwt = createMockJwt(userId.toString());

        taskView = TaskAdminView.builder()
                .id(taskId)
                .title("Test Task")
                .difficulty(TaskDifficulty.MEDIUM)
                .points(100)
                .timeLimitMs(2000)
                .memoryLimitMb(256)
                .type(TaskTestType.CONSOLE_IO)
                .testCases(List.of(
                        TestCaseFull.builder()
                                .id(UUID.randomUUID())
                                .input("1 2")
                                .expectedOutput("3")
                                .isHidden(false)
                                .build(),
                        TestCaseFull.builder()
                                .id(UUID.randomUUID())
                                .input("5 7")
                                .expectedOutput("12")
                                .isHidden(true)
                                .build()
                ))
                .build();

        when(taskClient.getForExecution(taskId)).thenReturn(taskView);
        when(taskClient.getBatch(any())).thenReturn(Map.of(
                taskId, TaskShortView.builder()
                        .id(taskId)
                        .title("Test Task")
                        .difficulty(TaskDifficulty.MEDIUM)
                        .points(100)
                        .build()
        ));

        when(securityUtil.isOwner(any(Jwt.class), eq(userId))).thenReturn(true);
        when(securityUtil.isAdmin(any(Jwt.class))).thenReturn(false);
    }

    @Test
    @DisplayName("Should submit solution and create outbox event")
    void shouldSubmitSolutionAndCreateOutboxEvent() {
        SolutionRequest request = new SolutionRequest(ProgrammingLanguage.JAVA, "class Solution { }");

        UUID solutionId = solutionService.submit(request, taskId, userId);

        assertThat(solutionId).isNotNull();

        SolutionEntity saved = solutionRepository.findById(solutionId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(SolutionStatus.PROCESSING);
        assertThat(saved.getTaskId()).isEqualTo(taskId);
        assertThat(saved.getUserId()).isEqualTo(userId);

        var source = mongoSolutionRepository.findBySolutionIdAndUserId(solutionId, userId);
        assertThat(source).isPresent();
        assertThat(source.get().getCode()).isEqualTo("class Solution { }");

        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
        assertThat(outboxEvents).hasSize(1);
        assertThat(outboxEvents.get(0).getTopic()).isEqualTo("code.execution.requests");
    }

    @Test
    @DisplayName("Should return solution by ID for owner")
    void shouldReturnSolutionByIdForOwner() {
        SolutionRequest request = new SolutionRequest(ProgrammingLanguage.JAVA, "secret code");
        UUID solutionId = solutionService.submit(request, taskId, userId);

        var result = solutionService.getById(solutionId, mockJwt);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(solutionId);
        assertThat(result.code()).isEqualTo("secret code");
    }

    @Test
    @DisplayName("Should list user solutions with pagination")
    void shouldListUserSolutions() {
        for (int i = 0; i < 5; i++) {
            SolutionRequest request = new SolutionRequest(ProgrammingLanguage.JAVA, "code " + i);
            solutionService.submit(request, taskId, userId);
        }

        var result = solutionService.listMine(userId, PageRequest.of(0, 3));

        assertThat(result.content()).hasSize(3);
        assertThat(result.totalElements()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should delete solution and clean both databases")
    void shouldDeleteSolutionAndCleanBothDatabases() {
        SolutionRequest request = new SolutionRequest(ProgrammingLanguage.JAVA, "code to delete");
        UUID solutionId = solutionService.submit(request, taskId, userId);

        assertThat(solutionRepository.existsById(solutionId)).isTrue();
        assertThat(mongoSolutionRepository.findBySolutionIdAndUserId(solutionId, userId)).isPresent();

        solutionService.delete(solutionId, mockJwt);

        assertThat(solutionRepository.existsById(solutionId)).isFalse();
        assertThat(mongoSolutionRepository.findBySolutionIdAndUserId(solutionId, userId)).isEmpty();
    }

    @Test
    @DisplayName("Should return completed tasks with solutions")
    void shouldReturnCompletedTasks() {
        SolutionRequest request = new SolutionRequest(ProgrammingLanguage.JAVA, "code");
        UUID solutionId = solutionService.submit(request, taskId, userId);

        SolutionEntity solution = solutionRepository.findById(solutionId).orElseThrow();
        solution.setStatus(SolutionStatus.COMPLETED);
        solution.setUpdatedAt(Instant.now());
        solutionRepository.save(solution);

        var result = solutionService.getMyCompletedTasks(userId, PageRequest.of(0, 10));

        assertThat(result.content()).isNotEmpty();
        assertThat(result.content().get(0).task().id()).isEqualTo(taskId);
        assertThat(result.content().get(0).attemptsCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should detect duplicate submissions via normalized code")
    void shouldDetectDuplicateSubmissions() {
        String code = "public class Solution { public static void main(String[] args) { System.out.println(\"test\"); } }";
        SolutionRequest request = new SolutionRequest(ProgrammingLanguage.JAVA, code);

        UUID firstId = solutionService.submit(request, taskId, userId);
        UUID secondId = solutionService.submit(request, taskId, userId);

        assertThat(firstId).isNotNull();
        assertThat(secondId).isNotNull();

        boolean exists = mongoSolutionRepository.existsByUserIdAndNormalizedCodeAndTaskIdAndLanguage(
                userId,
                "public class Solution { public static void main(String[] args) { System.out.println(\"test\"); } }",
                taskId,
                ProgrammingLanguage.JAVA
        );
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when getting non-existent solution")
    void shouldThrowWhenSolutionNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        assertThatThrownBy(() -> solutionService.getById(nonExistentId, mockJwt))
                .isInstanceOf(com.technokratos.common.handler.exceptions.SolutionNotFound.class);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent solution")
    void shouldThrowWhenDeletingNonExistent() {
        UUID nonExistentId = UUID.randomUUID();

        assertThatThrownBy(() -> solutionService.delete(nonExistentId, mockJwt))
                .isInstanceOf(com.technokratos.common.handler.exceptions.SolutionNotFound.class);
    }

    private Jwt createMockJwt(String userId) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", userId)
                .claim("realm_access", Map.of("roles", List.of()))
                .build();
    }
}