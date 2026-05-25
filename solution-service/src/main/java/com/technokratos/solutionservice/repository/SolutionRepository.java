package com.technokratos.solutionservice.repository;

import com.technokratos.common.dto.UserTaskSummary;
import com.technokratos.common.enums.SolutionStatus;
import com.technokratos.solutionservice.model.SolutionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SolutionRepository extends JpaRepository<SolutionEntity, UUID> {

    Page<SolutionEntity> findAllByUserId(UUID userId, Pageable pageable);

    List<SolutionEntity> findAllByUserIdAndTaskIdIn(UUID userId, List<UUID> taskIds);

    @Query("""
            SELECT new com.technokratos.common.dto.UserTaskSummary(
                s.taskId,
                MAX(s.updatedAt) as lastSolvedAt,
                COUNT(s.id),
                SUM(CASE WHEN s.status = com.technokratos.common.enums.SolutionStatus.COMPLETED THEN 1 ELSE 0 END)
            )
            FROM SolutionEntity s
            WHERE s.userId = :userId
            GROUP BY s.taskId
            ORDER BY MAX(s.updatedAt) DESC
            """)
    Page<UserTaskSummary> findUserTaskSummaries(@Param("userId") UUID userId, Pageable pageable);

    List<SolutionEntity> findByStatusAndCreatedAtBefore(SolutionStatus status, Instant threshold);
}
