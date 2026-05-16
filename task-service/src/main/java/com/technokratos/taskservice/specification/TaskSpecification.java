package com.technokratos.taskservice.specification;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.common.enums.TaskTestType;
import com.technokratos.taskservice.model.Task;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TaskSpecification {

    public static Specification<Task> filterBy(Set<TaskDifficulty> difficulties,
                                               Set<ProgrammingLanguage> languages,
                                               UUID authorId,
                                               Set<TaskTestType> types,
                                               Short minRating,
                                               Short maxRating,
                                               String searchText) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (difficulties != null && !difficulties.isEmpty()) {
                predicates.add(root.get("difficulty").in(difficulties));
            }

            if (languages != null && !languages.isEmpty()) {
                predicates.add(root.get("language").in(languages));
            }

            if (authorId != null) {
                predicates.add(criteriaBuilder.equal(root.get("authorId"), authorId));
            }

            if (types != null && !types.isEmpty()) {
                predicates.add(root.get("type").in(types));
            }

            if (minRating != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("rating"), minRating));
            }

            if (maxRating != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("rating"), maxRating));
            }

            if (searchText != null && !searchText.isBlank()) {
                String pattern = "%" + searchText.toLowerCase() + "%";
                Predicate titleMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern);
                Predicate conditionMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("condition")), pattern);
                predicates.add(criteriaBuilder.or(titleMatch, conditionMatch));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
