package com.technokratos.taskservice.repository;

import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.common.enums.TaskStatus;
import com.technokratos.taskservice.model.Tag;
import com.technokratos.taskservice.model.Task;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.Set;

public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> hasStatus(TaskStatus status) {
        if (status == null) {
            return null;
        }
        return (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Task> hasDifficulty(TaskDifficulty difficulty) {
        if (difficulty == null) {
            return null;
        }
        return (root, q, cb) -> cb.equal(root.get("difficulty"), difficulty);
    }

    public static Specification<Task> titleContains(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, q, cb) -> cb.like(cb.lower(root.get("title")), pattern);
    }

    /**
     * Filter tasks containing ALL of the given tag slugs.
     * Implemented via subquery to avoid Cartesian-product blowup on joins.
     */
    public static Specification<Task> hasAllTags(Collection<String> tagSlugs) {
        if (tagSlugs == null || tagSlugs.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> {
            assert query != null;
            query.distinct(true);
            Join<Task, Tag> tagJoin = root.join("tags", JoinType.INNER);
            return tagJoin.get("slug").in(tagSlugs);
        };
    }

    public static Specification<Task> hasAuthor(java.util.UUID authorId) {
        if (authorId == null) {
            return null;
        }
        return (root, q, cb) -> cb.equal(root.get("authorId"), authorId);
    }
}
