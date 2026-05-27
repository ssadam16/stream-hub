package com.technokratos.solutionservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.technokratos.common.dto.*;
import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.common.enums.SolutionStatus;
import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.common.events.CodeExecutionResult;
import com.technokratos.common.handler.exceptions.AccessDeniedException;
import com.technokratos.securityspringbootstarter.SecurityUtil;
import com.technokratos.solutionservice.client.TaskClient;
import com.technokratos.solutionservice.dto.SolutionRequest;
import com.technokratos.solutionservice.model.OutboxEvent;
import com.technokratos.solutionservice.model.SolutionEntity;
import com.technokratos.solutionservice.model.SolutionSource;
import com.technokratos.solutionservice.repository.MongoSolutionRepository;
import com.technokratos.solutionservice.repository.OutboxEventRepository;
import com.technokratos.solutionservice.repository.SolutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SolutionService Unit Test")
class SolutionServiceUnitTest {

    @Mock
    private SolutionRepository solutionRepository;
    @Mock
    private MongoSolutionRepository mongoSolutionRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private TaskClient taskClient;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private SolutionService solutionService;

    private UUID userId;
    private UUID taskId;
    private UUID solutionId;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        solutionId = UUID.randomUUID();
        jwt = mock(Jwt.class);
    }

    @Test
    @DisplayName("submit() happy path")
    void submit_HappyPath() throws Exception {
        SolutionRequest request = new SolutionRequest(ProgrammingLanguage.JAVA, "public class Solution {}");
        TaskAdminView task = createTaskAdminView();

        when(taskClient.getForExecution(taskId)).thenReturn(task);
        when(solutionRepository.save(any(SolutionEntity.class))).thenAnswer(inv -> {
            SolutionEntity entity = inv.getArgument(0);
            entity.setId(solutionId);
            return entity;
        });
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        UUID result = solutionService.submit(request, taskId, userId);

        assertThat(result).isEqualTo(solutionId);

        verify(solutionRepository).save(any());
        verify(outboxEventRepository).save(any(OutboxEvent.class));
        verify(mongoSolutionRepository).save(any(SolutionSource.class));
    }

    @Test
    @DisplayName("submit() when TaskClient fails")
    void submit_TaskClientFailure() {
        SolutionRequest request = new SolutionRequest(ProgrammingLanguage.JAVA, "code");

        when(taskClient.getForExecution(taskId)).thenThrow(new RuntimeException("Task service down"));

        assertThatThrownBy(() -> solutionService.submit(request, taskId, userId))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("onExecutionResult() updates solution and creates outbox events")
    void onExecutionResult_UpdatesAndPublishesEvents() throws Exception {
        SolutionEntity existing = SolutionEntity.builder()
                .id(solutionId)
                .status(SolutionStatus.PROCESSING)
                .userId(userId)
                .taskId(taskId)
                .language(ProgrammingLanguage.JAVA)
                .points(100)
                .difficulty(TaskDifficulty.MEDIUM)
                .build();

        CodeExecutionResult result = CodeExecutionResult.builder()
                .solutionId(solutionId)
                .status(SolutionStatus.COMPLETED)
                .passedTests(10L)
                .totalTests(10)
                .totalTimeMs(150L)
                .build();

        when(solutionRepository.findById(solutionId)).thenReturn(Optional.of(existing));
        when(solutionRepository.save(any(SolutionEntity.class))).thenReturn(existing);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        solutionService.onExecutionResult(result);

        verify(solutionRepository, times(1)).save(any());
        verify(outboxEventRepository, times(2)).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("onExecutionResult() skips already final status")
    void onExecutionResult_SkipsFinalStatus() {
        SolutionEntity existing = SolutionEntity.builder()
                .status(SolutionStatus.WRONG_ANSWER)
                .build();

        CodeExecutionResult result = CodeExecutionResult.builder()
                .solutionId(solutionId)
                .status(SolutionStatus.COMPLETED)
                .build();

        when(solutionRepository.findById(solutionId)).thenReturn(Optional.of(existing));

        solutionService.onExecutionResult(result);

        verify(solutionRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("getById() throws AccessDeniedException")
    void getById_AccessDenied() {
        SolutionEntity solution = SolutionEntity.builder().userId(UUID.randomUUID()).build();

        when(solutionRepository.findById(solutionId)).thenReturn(Optional.of(solution));
        when(securityUtil.isOwner(any(), any())).thenReturn(false);
        when(securityUtil.isAdmin(any())).thenReturn(false);

        assertThatThrownBy(() -> solutionService.getById(solutionId, jwt))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("delete() removes from Postgres and Mongo")
    void delete_Success() {
        SolutionEntity solution = SolutionEntity.builder().userId(userId).build();
        SolutionSource source = SolutionSource.builder()
                .solutionId(solutionId)
                .userId(userId)
                .build();

        when(solutionRepository.findById(solutionId)).thenReturn(Optional.of(solution));
        when(securityUtil.isOwner(any(), any())).thenReturn(true);
        when(mongoSolutionRepository.findBySolutionIdAndUserId(solutionId, userId))
                .thenReturn(Optional.of(source));

        solutionService.delete(solutionId, jwt);

        verify(solutionRepository).deleteById(solutionId);
        verify(mongoSolutionRepository).delete(source);
    }

    @Test
    @DisplayName("getMyCompletedTasks() handles missing task in batch response")
    void getMyCompletedTasks_HandlesMissingTaskInBatch() {
        UserTaskSummary summary = new UserTaskSummary(taskId, null, 5, 2);
        Page<UserTaskSummary> summaries = new PageImpl<>(List.of(summary));

        when(solutionRepository.findUserTaskSummaries(userId, PageRequest.of(0, 10)))
                .thenReturn(summaries);

        when(taskClient.getBatch(anyList())).thenReturn(Collections.emptyMap());

        PageResponse<CompletedTaskView> result = solutionService.getMyCompletedTasks(userId, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).task()).isNull();
    }

    private TaskAdminView createTaskAdminView() {
        return TaskAdminView.builder()
                .id(taskId)
                .difficulty(TaskDifficulty.MEDIUM)
                .points(100)
                .timeLimitMs(2000)
                .memoryLimitMb(256)
                .testCases(List.of())
                .build();
    }
}