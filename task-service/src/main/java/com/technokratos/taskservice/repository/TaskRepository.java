package com.technokratos.taskservice.repository;

import com.technokratos.taskservice.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    Optional<Task> findByExternalId(String externalId);

    @EntityGraph(attributePaths = {"testCases"})
    Optional<Task> findById(UUID id);

    Page<Task> findAll(Specification<Task> spec, Pageable pageable);
}
