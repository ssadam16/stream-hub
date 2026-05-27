package com.technokratos.taskservice.service;

import com.technokratos.common.dto.TaskAdminView;
import com.technokratos.common.dto.TaskPublicView;
import com.technokratos.common.dto.TaskShortView;
import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.common.enums.TaskStatus;
import com.technokratos.common.enums.TaskTestType;
import com.technokratos.taskservice.dto.request.AdminTaskCreateRequest;
import com.technokratos.taskservice.dto.request.AdminTaskUpdateRequest;
import com.technokratos.taskservice.dto.request.TestCaseRequest;
import com.technokratos.taskservice.exception.TaskNotFoundException;
import com.technokratos.taskservice.mapper.TaskMapper;
import com.technokratos.taskservice.model.Tag;
import com.technokratos.taskservice.model.Task;
import com.technokratos.taskservice.repository.TagRepository;
import com.technokratos.taskservice.repository.TaskRepository;
import com.technokratos.taskservice.repository.TaskUserStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task Service Unit Tests")
class TaskServiceUnitTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private TaskUserStatsRepository statsRepository;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    private UUID taskId;
    private UUID authorId;
    private Task task;
    private TaskAdminView adminView;
    private TaskPublicView publicView;
    private TaskShortView shortView;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        authorId = UUID.randomUUID();

        task = Task.builder()
                .id(taskId)
                .title("Test Task")
                .slug("test-task")
                .condition("Test condition")
                .authorId(authorId)
                .difficulty(TaskDifficulty.MEDIUM)
                .status(TaskStatus.PUBLISHED)
                .type(TaskTestType.CONSOLE_IO)
                .points(100)
                .timeLimitMs(2000)
                .memoryLimitMb(256)
                .build();

        adminView = mock(TaskAdminView.class);
        publicView = mock(TaskPublicView.class);
        shortView = mock(TaskShortView.class);
    }

    @Nested
    @DisplayName("getPublicById - Should enforce PUBLISHED status")
    class GetPublicById {

        @Test
        @DisplayName("Should return public view when task is PUBLISHED")
        void shouldReturnPublicViewForPublishedTask() {
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
            when(taskMapper.toPublicView(task)).thenReturn(publicView);

            var result = taskService.getPublicById(taskId);

            assertThat(result).isEqualTo(publicView);
            verify(taskMapper).toPublicView(task);
        }

        @Test
        @DisplayName("Should throw NotFound when task is DRAFT")
        void shouldThrowNotFoundForDraftTask() {
            task.setStatus(TaskStatus.DRAFT);
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> taskService.getPublicById(taskId))
                    .isInstanceOf(TaskNotFoundException.class)
                    .hasMessageContaining(taskId.toString());

            verify(taskMapper, never()).toPublicView(any());
        }

        @Test
        @DisplayName("Should throw NotFound when task is ARCHIVED")
        void shouldThrowNotFoundForArchivedTask() {
            task.setStatus(TaskStatus.ARCHIVED);
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> taskService.getPublicById(taskId))
                    .isInstanceOf(TaskNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw NotFound when task doesn't exist")
        void shouldThrowNotFoundWhenTaskMissing() {
            when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.getPublicById(taskId))
                    .isInstanceOf(TaskNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAdminById")
    class GetAdminById {

        @Test
        @DisplayName("Should return admin view for any task regardless of status")
        void shouldReturnAdminViewForAnyTask() {
            task.setStatus(TaskStatus.DRAFT);
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
            when(taskMapper.toAdminView(task)).thenReturn(adminView);

            var result = taskService.getAdminById(taskId);

            assertThat(result).isEqualTo(adminView);
        }
    }

    @Nested
    @DisplayName("listPublic - Edge cases")
    class ListPublic {

        @Test
        @DisplayName("Should handle null tagSlugs without NPE")
        void shouldHandleNullTagSlugs() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Task> taskPage = new PageImpl<>(List.of(task));

            when(taskRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(taskPage);
            when(taskMapper.toShortView(task)).thenReturn(shortView);

            assertThatCode(() -> taskService.listPublic(TaskDifficulty.EASY, null, pageable))
                    .doesNotThrowAnyException();

            verify(taskRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Should handle empty tagSlugs")
        void shouldHandleEmptyTagSlugs() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Task> taskPage = new PageImpl<>(List.of(task));

            when(taskRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(taskPage);
            when(taskMapper.toShortView(task)).thenReturn(shortView);

            var result = taskService.listPublic(null, List.of(), pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("create - Slug generation and validation")
    class Create {

        @Test
        @DisplayName("Should create task with unique slug")
        void shouldCreateWithUniqueSlug() {
            AdminTaskCreateRequest request = AdminTaskCreateRequest.builder()
                    .title("My Great Task")
                    .condition("Condition")
                    .difficulty(TaskDifficulty.EASY)
                    .type(TaskTestType.CONSOLE_IO)
                    .points(100)
                    .timeLimitMs(2000)
                    .memoryLimitMb(256)
                    .tagSlugs(Set.of())
                    .testCases(List.of())
                    .build();

            when(taskRepository.existsBySlug(anyString())).thenReturn(false);
            when(taskRepository.saveAndFlush(any(Task.class))).thenReturn(task);
            when(taskMapper.toAdminView(task)).thenReturn(adminView);

            var result = taskService.create(request, authorId);

            assertThat(result).isEqualTo(adminView);
            verify(taskRepository).saveAndFlush(argThat(t ->
                    t.getSlug().equals("my-great-task")
            ));
        }

        @Test
        @DisplayName("Should handle duplicate slug by appending number")
        void shouldHandleDuplicateSlug() {
            AdminTaskCreateRequest request = AdminTaskCreateRequest.builder()
                    .title("Task")
                    .condition("Condition")
                    .difficulty(TaskDifficulty.EASY)
                    .type(TaskTestType.CONSOLE_IO)
                    .points(100)
                    .timeLimitMs(2000)
                    .memoryLimitMb(256)
                    .tagSlugs(Set.of())
                    .testCases(List.of())
                    .build();

            when(taskRepository.existsBySlug("task")).thenReturn(true);
            when(taskRepository.existsBySlug("task-2")).thenReturn(false);
            when(taskRepository.saveAndFlush(any(Task.class))).thenReturn(task);
            when(taskMapper.toAdminView(task)).thenReturn(adminView);

            taskService.create(request, authorId);

            verify(taskRepository).saveAndFlush(argThat(t ->
                    t.getSlug().equals("task-2")
            ));
        }

        @Test
        @DisplayName("Should throw when unknown tags provided")
        void shouldThrowForUnknownTags() {
            AdminTaskCreateRequest request = AdminTaskCreateRequest.builder()
                    .title("Task")
                    .condition("Condition")
                    .difficulty(TaskDifficulty.EASY)
                    .type(TaskTestType.CONSOLE_IO)
                    .points(100)
                    .timeLimitMs(2000)
                    .memoryLimitMb(256)
                    .tagSlugs(Set.of("unknown-tag-1", "unknown-tag-2"))
                    .testCases(List.of())
                    .build();

            when(tagRepository.findAllBySlugIn(Set.of("unknown-tag-1", "unknown-tag-2")))
                    .thenReturn(Set.of());

            assertThatThrownBy(() -> taskService.create(request, authorId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown tag slugs")
                    .hasMessageContaining("unknown-tag-1");
        }

        @Test
        @DisplayName("Should resolve existing tags correctly")
        void shouldResolveExistingTags() {
            Tag existingTag = Tag.builder().slug("dp").build();
            AdminTaskCreateRequest request = AdminTaskCreateRequest.builder()
                    .title("Task")
                    .condition("Condition")
                    .difficulty(TaskDifficulty.EASY)
                    .type(TaskTestType.CONSOLE_IO)
                    .points(100)
                    .timeLimitMs(2000)
                    .memoryLimitMb(256)
                    .tagSlugs(Set.of("dp"))
                    .testCases(List.of())
                    .build();

            when(tagRepository.findAllBySlugIn(Set.of("dp")))
                    .thenReturn(Set.of(existingTag));
            when(taskRepository.existsBySlug(anyString())).thenReturn(false);
            when(taskRepository.saveAndFlush(any(Task.class))).thenReturn(task);
            when(taskMapper.toAdminView(task)).thenReturn(adminView);

            taskService.create(request, authorId);

            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
            verify(taskRepository).saveAndFlush(taskCaptor.capture());
            assertThat(taskCaptor.getValue().getTags()).contains(existingTag);
        }
    }

    @Nested
    @DisplayName("update - Partial updates and validation")
    class Update {

        @Test
        @DisplayName("Should update only provided fields")
        void shouldUpdateOnlyProvidedFields() {
            AdminTaskUpdateRequest updateRequest = AdminTaskUpdateRequest.builder()
                    .title("New Title")
                    .build();

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
            when(taskMapper.toAdminView(task)).thenReturn(adminView);

            taskService.update(taskId, updateRequest);

            assertThat(task.getTitle()).isEqualTo("New Title");
            assertThat(task.getCondition()).isEqualTo("Test condition");
            assertThat(task.getDifficulty()).isEqualTo(TaskDifficulty.MEDIUM);
            assertThat(task.getPoints()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should update multiple fields")
        void shouldUpdateMultipleFields() {
            AdminTaskUpdateRequest updateRequest = AdminTaskUpdateRequest.builder()
                    .title("New Title")
                    .condition("New Condition")
                    .difficulty(TaskDifficulty.HARD)
                    .points(200)
                    .timeLimitMs(5000)
                    .memoryLimitMb(512)
                    .build();

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
            when(taskMapper.toAdminView(task)).thenReturn(adminView);

            taskService.update(taskId, updateRequest);

            assertThat(task.getTitle()).isEqualTo("New Title");
            assertThat(task.getCondition()).isEqualTo("New Condition");
            assertThat(task.getDifficulty()).isEqualTo(TaskDifficulty.HARD);
            assertThat(task.getPoints()).isEqualTo(200);
            assertThat(task.getTimeLimitMs()).isEqualTo(5000);
            assertThat(task.getMemoryLimitMb()).isEqualTo(512);
        }

        @Test
        @DisplayName("Should resolve tags when tagSlugs provided")
        void shouldResolveTagsOnUpdate() {
            Set<String> tagSlugs = Set.of("dp", "array");
            Tag tagDp = Tag.builder().slug("dp").build();
            Tag tagArray = Tag.builder().slug("array").build();

            AdminTaskUpdateRequest updateRequest = AdminTaskUpdateRequest.builder()
                    .tagSlugs(tagSlugs)
                    .build();

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
            when(tagRepository.findAllBySlugIn(tagSlugs)).thenReturn(Set.of(tagDp, tagArray));
            when(taskMapper.toAdminView(task)).thenReturn(adminView);

            taskService.update(taskId, updateRequest);

            assertThat(task.getTags()).containsExactlyInAnyOrder(tagDp, tagArray);
        }

        @Test
        @DisplayName("Should throw when partial tagSlugs unknown")
        void shouldThrowForPartialUnknownTags() {
            Set<String> tagSlugs = Set.of("dp", "unknown");
            Tag tagDp = Tag.builder().slug("dp").build();

            AdminTaskUpdateRequest updateRequest = AdminTaskUpdateRequest.builder()
                    .tagSlugs(tagSlugs)
                    .build();

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
            when(tagRepository.findAllBySlugIn(tagSlugs)).thenReturn(Set.of(tagDp));

            assertThatThrownBy(() -> taskService.update(taskId, updateRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown tag slugs")
                    .hasMessageContaining("unknown");
        }

        @Test
        @DisplayName("Should throw when task not found")
        void shouldThrowWhenTaskNotFound() {
            AdminTaskUpdateRequest updateRequest = AdminTaskUpdateRequest.builder()
                    .title("New Title")
                    .build();

            when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.update(taskId, updateRequest))
                    .isInstanceOf(TaskNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("publish - Change status")
    class Publish {

        @Test
        @DisplayName("Should publish task")
        void shouldPublishTask() {
            task.setStatus(TaskStatus.DRAFT);

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
            when(taskMapper.toAdminView(task)).thenReturn(adminView);

            var result = taskService.publish(taskId, true);

            assertThat(result).isEqualTo(adminView);
            assertThat(task.getStatus()).isEqualTo(TaskStatus.PUBLISHED);
        }

        @Test
        @DisplayName("Should unpublish task (set to DRAFT)")
        void shouldUnpublishTask() {
            task.setStatus(TaskStatus.PUBLISHED);

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
            when(taskMapper.toAdminView(task)).thenReturn(adminView);

            var result = taskService.publish(taskId, false);

            assertThat(task.getStatus()).isEqualTo(TaskStatus.DRAFT);
        }
    }

    @Nested
    @DisplayName("delete - Existence check")
    class Delete {

        @Test
        @DisplayName("Should delete existing task")
        void shouldDeleteExistingTask() {
            when(taskRepository.existsById(taskId)).thenReturn(true);
            doNothing().when(taskRepository).deleteById(taskId);

            taskService.delete(taskId);

            verify(taskRepository).deleteById(taskId);
        }

        @Test
        @DisplayName("Should throw when task not found")
        void shouldThrowWhenTaskNotFound() {
            when(taskRepository.existsById(taskId)).thenReturn(false);

            assertThatThrownBy(() -> taskService.delete(taskId))
                    .isInstanceOf(TaskNotFoundException.class);

            verify(taskRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("shortViewBatch - Batch fetching")
    class ShortViewBatch {

        @Test
        @DisplayName("Should return empty map for null list")
        void shouldReturnEmptyForNull() {
            var result = taskService.shortViewBatch(null);
            assertThat(result).isEmpty();
            verify(taskRepository, never()).findAllByIdWithTagsAndTestCases(any());
        }

        @Test
        @DisplayName("Should return empty map for empty list")
        void shouldReturnEmptyForEmpty() {
            var result = taskService.shortViewBatch(List.of());
            assertThat(result).isEmpty();
            verify(taskRepository, never()).findAllByIdWithTagsAndTestCases(any());
        }

        @Test
        @DisplayName("Should fetch and map tasks by IDs")
        void shouldFetchAndMapTasks() {
            UUID id2 = UUID.randomUUID();
            Task task2 = Task.builder().id(id2).build();
            List<Task> tasks = List.of(task, task2);
            TaskShortView shortView2 = mock(TaskShortView.class);

            when(taskRepository.findAllByIdWithTagsAndTestCases(List.of(taskId, id2)))
                    .thenReturn(tasks);
            when(taskMapper.toShortView(task)).thenReturn(shortView);
            when(taskMapper.toShortView(task2)).thenReturn(shortView2);

            var result = taskService.shortViewBatch(List.of(taskId, id2));

            assertThat(result).hasSize(2);
            assertThat(result.get(taskId)).isEqualTo(shortView);
            assertThat(result.get(id2)).isEqualTo(shortView2);
        }
    }
}