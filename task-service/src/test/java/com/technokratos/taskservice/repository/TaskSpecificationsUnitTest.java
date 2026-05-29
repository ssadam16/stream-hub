package com.technokratos.taskservice.repository;

import com.technokratos.taskservice.model.Task;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task Specifications Unit Tests")
class TaskSpecificationsUnitTest {

    @Mock
    private Root<Task> root;
    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder cb;
    @Mock
    private Join<Object, Object> join;
    @Mock
    private Path<Object> path;
    @Mock
    private Predicate predicate;

    @BeforeEach
    void setUp() {
        lenient().when(root.join("tags", JoinType.INNER)).thenReturn(join);
        lenient().when(join.get("slug")).thenReturn(path);
        lenient().when(cb.equal(any(), any())).thenReturn(predicate);
        lenient().when(path.in(any(Collection.class))).thenReturn(predicate);
    }

    @Test
    @DisplayName("hasAllTags should match tasks having ANY of the specified tags (OR logic)")
    void hasAllTagsShouldMatchAnyTag() {
        Collection<String> tagSlugs = List.of("dp", "array");
        Specification<Task> spec = TaskSpecifications.hasAllTags(tagSlugs);

        spec.toPredicate(root, query, cb);

        ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
        verify(path).in(captor.capture());

        Collection captured = captor.getValue();
        assertThat(captured).containsExactlyInAnyOrder("dp", "array");
    }

    @Test
    @DisplayName("hasAllTags should return null for null tagSlugs")
    void hasAllTagsShouldReturnNullForNull() {
        Specification<Task> spec = TaskSpecifications.hasAllTags(null);
        assertThat(spec).isNull();
    }

    @Test
    @DisplayName("hasAllTags should return null for empty collection")
    void hasAllTagsShouldReturnNullForEmpty() {
        Specification<Task> spec = TaskSpecifications.hasAllTags(List.of());
        assertThat(spec).isNull();
    }

    @Test
    @DisplayName("hasStatus should return null for null status")
    void hasStatusReturnsNullForNull() {
        assertThat(TaskSpecifications.hasStatus(null)).isNull();
    }

    @Test
    @DisplayName("hasStatus should create predicate for valid status")
    void hasStatusCreatesPredicateForValidStatus() {
        Specification<Task> spec = TaskSpecifications.hasStatus(com.technokratos.common.enums.TaskStatus.PUBLISHED);
        assertThat(spec).isNotNull();

        spec.toPredicate(root, query, cb);
        verify(cb).equal(root.get("status"), com.technokratos.common.enums.TaskStatus.PUBLISHED);
    }

    @Test
    @DisplayName("hasDifficulty should return null for null difficulty")
    void hasDifficultyReturnsNullForNull() {
        assertThat(TaskSpecifications.hasDifficulty(null)).isNull();
    }

    @Test
    @DisplayName("hasDifficulty should create predicate for valid difficulty")
    void hasDifficultyCreatesPredicateForValidDifficulty() {
        Specification<Task> spec = TaskSpecifications.hasDifficulty(com.technokratos.common.enums.TaskDifficulty.MEDIUM);
        assertThat(spec).isNotNull();

        spec.toPredicate(root, query, cb);
        verify(cb).equal(root.get("difficulty"), com.technokratos.common.enums.TaskDifficulty.MEDIUM);
    }

    @Test
    @DisplayName("titleContains should return null for null or blank")
    void titleContainsReturnsNullForInvalidInput() {
        assertThat(TaskSpecifications.titleContains(null)).isNull();
        assertThat(TaskSpecifications.titleContains("")).isNull();
        assertThat(TaskSpecifications.titleContains("   ")).isNull();
    }

    @Test
    @DisplayName("titleContains should create like predicate for valid input")
    void titleContainsCreatesLikePredicate() {
        Specification<Task> spec = TaskSpecifications.titleContains("test");
        assertThat(spec).isNotNull();

        spec.toPredicate(root, query, cb);
        verify(cb).like(any(), eq("%test%"));
    }

    @Test
    @DisplayName("hasAuthor should return null for null authorId")
    void hasAuthorReturnsNullForNull() {
        assertThat(TaskSpecifications.hasAuthor(null)).isNull();
    }

    @Test
    @DisplayName("hasAuthor should create predicate for valid authorId")
    void hasAuthorCreatesPredicateForValidAuthorId() {
        UUID authorId = UUID.randomUUID();
        Specification<Task> spec = TaskSpecifications.hasAuthor(authorId);
        assertThat(spec).isNotNull();

        spec.toPredicate(root, query, cb);
        verify(cb).equal(root.get("authorId"), authorId);
    }
}