package com.technokratos.taskservice.service;

import com.technokratos.common.dto.TaskAdminView;
import com.technokratos.common.dto.TaskPublicView;
import com.technokratos.common.dto.TaskShortView;
import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.common.enums.TaskStatus;
import com.technokratos.common.enums.TaskTestType;
import com.technokratos.taskservice.dto.request.AdminTaskCreateRequest;
import com.technokratos.taskservice.dto.request.TestCaseRequest;
import com.technokratos.taskservice.exception.TaskNotFoundException;
import com.technokratos.taskservice.model.Tag;
import com.technokratos.taskservice.model.Task;
import com.technokratos.taskservice.repository.TagRepository;
import com.technokratos.taskservice.repository.TaskRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("Task Service Integration Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@org.junit.jupiter.api.Tag("integration")
@ContextConfiguration(initializers = TaskServiceIntegrationTest.Initializer.class)
class TaskServiceIntegrationTest {

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + postgres.getJdbcUrl(),
                    "spring.datasource.username=" + postgres.getUsername(),
                    "spring.datasource.password=" + postgres.getPassword(),
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.generate-ddl=true",
                    "spring.flyway.enabled=false"
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("taskdb")
            .withUsername("taskuser")
            .withPassword("taskpass");

    static {
        postgres.start();
    }

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TagRepository tagRepository;

    private UUID authorId;
    private Tag tagDp;
    private Tag tagArray;

    @BeforeAll
    void initTags() {
        taskRepository.deleteAll();
        tagRepository.deleteAll();

        tagDp = tagRepository.save(Tag.builder()
                .name("Dynamic Programming")
                .slug("dp")
                .category(com.technokratos.common.enums.TagCategory.TECHNIQUE)
                .build());

        tagArray = tagRepository.save(Tag.builder()
                .name("Array")
                .slug("array")
                .category(com.technokratos.common.enums.TagCategory.DATA_STRUCTURE)
                .build());
    }

    @BeforeEach
    void setUp() {
        authorId = UUID.randomUUID();
        taskRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create task with unique slug")
    void shouldCreateTaskWithUniqueSlug() {
        AdminTaskCreateRequest request = AdminTaskCreateRequest.builder()
                .title("My Amazing Task")
                .condition("Solve this problem")
                .difficulty(TaskDifficulty.MEDIUM)
                .type(TaskTestType.CONSOLE_IO)
                .points(100)
                .timeLimitMs(2000)
                .memoryLimitMb(256)
                .tagSlugs(Set.of("dp", "array"))
                .testCases(List.of(
                        new TestCaseRequest("1 2", "3", false, null),
                        new TestCaseRequest("5 7", "12", true, "Hidden test")
                ))
                .build();

        TaskAdminView result = taskService.create(request, authorId);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("My Amazing Task");
        assertThat(result.slug()).isEqualTo("my-amazing-task");
        assertThat(result.status()).isEqualTo(TaskStatus.DRAFT);
        assertThat(result.tags()).hasSize(2);
        assertThat(result.testCases()).hasSize(2);

        Task saved = taskRepository.findById(result.id()).orElseThrow();
        assertThat(saved.getTestCases()).hasSize(2);
        assertThat(saved.getTestCases().get(0).isHidden()).isFalse();
        assertThat(saved.getTestCases().get(1).isHidden()).isTrue();
    }

    @Test
    @DisplayName("Should handle duplicate slug by appending number")
    void shouldHandleDuplicateSlug() {
        AdminTaskCreateRequest request1 = AdminTaskCreateRequest.builder()
                .title("Duplicate Task")
                .condition("First")
                .difficulty(TaskDifficulty.EASY)
                .type(TaskTestType.CONSOLE_IO)
                .points(50)
                .timeLimitMs(1000)
                .memoryLimitMb(128)
                .tagSlugs(Set.of())
                .testCases(List.of())
                .build();

        AdminTaskCreateRequest request2 = AdminTaskCreateRequest.builder()
                .title("Duplicate Task")
                .condition("Second")
                .difficulty(TaskDifficulty.EASY)
                .type(TaskTestType.CONSOLE_IO)
                .points(50)
                .timeLimitMs(1000)
                .memoryLimitMb(128)
                .tagSlugs(Set.of())
                .testCases(List.of())
                .build();

        TaskAdminView first = taskService.create(request1, authorId);
        TaskAdminView second = taskService.create(request2, authorId);

        assertThat(first.slug()).isEqualTo("duplicate-task");
        assertThat(second.slug()).isEqualTo("duplicate-task-2");
    }

    @Test
    @DisplayName("Should return tasks that have ANY of the specified tags (OR logic)")
    void shouldReturnTasksWithAnyMatchingTag() {
        createAndSaveTask("Task Both", TaskStatus.PUBLISHED, Set.of(tagDp, tagArray));
        createAndSaveTask("Task Only DP", TaskStatus.PUBLISHED, Set.of(tagDp));
        createAndSaveTask("Task Only Array", TaskStatus.PUBLISHED, Set.of(tagArray));

        Page<TaskShortView> result = taskService.listPublic(
                null,
                List.of("dp", "array"),
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent())
                .extracting("title")
                .containsExactlyInAnyOrder("Task Both", "Task Only DP", "Task Only Array");

        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should get public view only for PUBLISHED tasks")
    void shouldGetPublicViewOnlyForPublished() {
        Task draftTask = createAndSaveTask("Draft Task", TaskStatus.DRAFT, Set.of());
        Task publishedTask = createAndSaveTask("Published Task", TaskStatus.PUBLISHED, Set.of());

        assertThatThrownBy(() -> taskService.getPublicById(draftTask.getId()))
                .isInstanceOf(TaskNotFoundException.class);

        TaskPublicView publicView = taskService.getPublicById(publishedTask.getId());
        assertThat(publicView.title()).isEqualTo("Published Task");
    }

    @Test
    @DisplayName("Should exclude hidden test cases from public view")
    void shouldExcludeHiddenTestCases() {
        AdminTaskCreateRequest request = AdminTaskCreateRequest.builder()
                .title("Task with Hidden Tests")
                .condition("Test")
                .difficulty(TaskDifficulty.EASY)
                .type(TaskTestType.CONSOLE_IO)
                .points(100)
                .timeLimitMs(2000)
                .memoryLimitMb(256)
                .tagSlugs(Set.of())
                .testCases(List.of(
                        new TestCaseRequest("1", "2", false, "Public"),
                        new TestCaseRequest("3", "4", true, "Hidden")
                ))
                .build();

        TaskAdminView adminView = taskService.create(request, authorId);
        taskService.publish(adminView.id(), true);

        TaskPublicView publicView = taskService.getPublicById(adminView.id());

        assertThat(publicView.examples()).hasSize(1);
        assertThat(publicView.examples().getFirst().input()).isEqualTo("1");
    }

    @Test
    @DisplayName("Should batch fetch tasks with their associations")
    void shouldBatchFetchWithAssociations() {
        Task task1 = createAndSaveTask("Batch 1", TaskStatus.PUBLISHED, Set.of(tagDp));
        Task task2 = createAndSaveTask("Batch 2", TaskStatus.PUBLISHED, Set.of(tagArray));

        var result = taskService.shortViewBatch(List.of(task1.getId(), task2.getId()));

        assertThat(result).hasSize(2);
        assertThat(result).containsKeys(task1.getId(), task2.getId());
        assertThat(result.get(task1.getId()).title()).isEqualTo("Batch 1");
        assertThat(result.get(task2.getId()).title()).isEqualTo("Batch 2");
    }

    @Test
    @DisplayName("Should return empty map for null or empty list in batch fetch")
    void shouldReturnEmptyForNullOrEmptyBatch() {
        assertThat(taskService.shortViewBatch(null)).isEmpty();
        assertThat(taskService.shortViewBatch(List.of())).isEmpty();
    }

    @Test
    @DisplayName("Should handle pagination correctly")
    void shouldHandlePagination() {
        for (int i = 0; i < 15; i++) {
            createAndSaveTask("Pagination Task " + i, TaskStatus.PUBLISHED, Set.of());
        }

        Page<TaskShortView> firstPage = taskService.listPublic(null, null, PageRequest.of(0, 10));
        Page<TaskShortView> secondPage = taskService.listPublic(null, null, PageRequest.of(1, 10));

        assertThat(firstPage.getTotalElements()).isGreaterThanOrEqualTo(15);
        assertThat(firstPage.getContent()).hasSize(10);
        assertThat(secondPage.getContent()).hasSize(5);
    }

    @Test
    @DisplayName("Should return empty list when no tasks match filters")
    void shouldReturnEmptyWhenNoTasksMatch() {
        Page<TaskShortView> result = taskService.listPublic(
                null,
                List.of("non-existent-tag-12345"),
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    private Task createAndSaveTask(String title, TaskStatus status, Set<Tag> tags) {
        String uniqueSlug = title.toLowerCase().replace(" ", "-") + "-" + UUID.randomUUID().toString().substring(0, 8);
        Task task = Task.builder()
                .title(title)
                .slug(uniqueSlug)
                .condition("Condition for " + title)
                .authorId(authorId)
                .difficulty(TaskDifficulty.EASY)
                .status(status)
                .type(TaskTestType.CONSOLE_IO)
                .points(100)
                .timeLimitMs(2000)
                .memoryLimitMb(256)
                .tags(tags)
                .build();
        return taskRepository.save(task);
    }
}