package com.technokratos.taskservice.mapper;

import com.technokratos.common.dto.TagDto;
import com.technokratos.common.dto.TaskAdminView;
import com.technokratos.common.dto.TaskPublicView;
import com.technokratos.common.dto.TaskShortView;
import com.technokratos.common.dto.TaskStats;
import com.technokratos.common.enums.TagCategory;
import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.common.enums.TaskStatus;
import com.technokratos.common.enums.TaskTestType;
import com.technokratos.taskservice.model.Tag;
import com.technokratos.taskservice.model.Task;
import com.technokratos.taskservice.model.TestCase;
import com.technokratos.taskservice.repository.TaskUserStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task Mapper Unit Tests")
class TaskMapperTest {

    @Mock
    private TagMapper tagMapper;

    @Mock
    private TaskUserStatsRepository statsRepository;

    @InjectMocks
    private TaskMapper taskMapper;

    private UUID taskId;
    private UUID authorId;
    private Task task;
    private TaskStats mockStats;
    private TagDto tagDto;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        authorId = UUID.randomUUID();
        mockStats = new TaskStats(10L, 7L);

        Tag tag = Tag.builder()
                .id(UUID.randomUUID())
                .name("Dynamic Programming")
                .slug("dp")
                .category(TagCategory.TECHNIQUE)
                .build();

        tagDto = new TagDto(tag.getId(), tag.getName(), tag.getSlug(), tag.getCategory());

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
                .tags(Set.of(tag))
                .testCases(List.of(
                        TestCase.builder()
                                .id(UUID.randomUUID())
                                .input("1 2")
                                .expectedOutput("3")
                                .isHidden(false)
                                .build(),
                        TestCase.builder()
                                .id(UUID.randomUUID())
                                .input("5 7")
                                .expectedOutput("12")
                                .isHidden(true)
                                .explanation("Hidden test")
                                .build()
                ))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        lenient().when(statsRepository.getTaskStatsByTaskId(taskId)).thenReturn(mockStats);
        lenient().when(tagMapper.toDto(tag)).thenReturn(tagDto);
    }

    @Test
    @DisplayName("Should map Task to AdminView with all test cases")
    void shouldMapToAdminViewWithAllTestCases() {
        TaskAdminView view = taskMapper.toAdminView(task);

        assertThat(view.id()).isEqualTo(taskId);
        assertThat(view.title()).isEqualTo("Test Task");
        assertThat(view.status()).isEqualTo(TaskStatus.PUBLISHED);
        assertThat(view.testCases()).hasSize(2);
        assertThat(view.testCases().get(0).isHidden()).isFalse();
        assertThat(view.testCases().get(1).isHidden()).isTrue();
        assertThat(view.testCases().get(1).explanation()).isEqualTo("Hidden test");
        assertThat(view.stats()).isEqualTo(mockStats);
        assertThat(view.tags()).hasSize(1);
    }

    @Test
    @DisplayName("Should map Task to PublicView with only public test cases")
    void shouldMapToPublicViewWithOnlyPublicTestCases() {
        TaskPublicView view = taskMapper.toPublicView(task);

        assertThat(view.id()).isEqualTo(taskId);
        assertThat(view.examples()).hasSize(1);
        assertThat(view.examples().getFirst().input()).isEqualTo("1 2");
        assertThat(view.examples().getFirst().expectedOutput()).isEqualTo("3");
        assertThat(view.tags()).hasSize(1);
    }

    @Test
    @DisplayName("Should handle null test cases gracefully")
    void shouldHandleNullTestCases() {
        task.setTestCases(null);

        TaskAdminView adminView = taskMapper.toAdminView(task);
        TaskPublicView publicView = taskMapper.toPublicView(task);

        assertThat(adminView.testCases()).isEmpty();
        assertThat(publicView.examples()).isEmpty();
    }

    @Test
    @DisplayName("Should handle empty tags set")
    void shouldHandleEmptyTags() {
        task.setTags(Set.of());

        TaskShortView view = taskMapper.toShortView(task);

        assertThat(view.tags()).isEmpty();
    }
}