package com.technokratos.taskservice.repository;

import com.technokratos.taskservice.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    Optional<Task> findByExternalId(String externalId);

    @EntityGraph(attributePaths = {"testCases", "tags"})
    Optional<Task> findById(UUID id);

    @EntityGraph(attributePaths = {"tags"})
    Optional<Task> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Override
    @EntityGraph(attributePaths = {"tags"})
    Page<Task> findAll(Pageable pageable);

    @Query("select distinct t from Task t left join fetch t.tags left join fetch t.testCases where t.id in :ids")
    List<Task> findAllByIdWithTagsAndTestCases(@Param("ids") List<UUID> ids);

}
