package com.technokratos.taskservice.repository;

import com.technokratos.common.dto.TaskStats;
import com.technokratos.taskservice.model.TaskUserStats;
import com.technokratos.taskservice.model.TaskUserStatsId;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TaskUserStatsRepository extends JpaRepository<TaskUserStats, TaskUserStatsId> {

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO task_user_stats (task_id, user_id, accepted)
            VALUES (:taskId, :userId, :accepted)
            ON CONFLICT (task_id, user_id)
            DO UPDATE SET
                accepted = task_user_stats.accepted OR EXCLUDED.accepted
            """, nativeQuery = true)
    void upsert(@Param("taskId") UUID taskId,
                @Param("userId") UUID userId,
                @Param("accepted") boolean accepted);

    @Query("""
        SELECT new com.technokratos.common.dto.TaskStats(
            COUNT(t),
            COUNT(CASE WHEN t.accepted = true THEN 1 END)
        )
        FROM TaskUserStats t
        WHERE t.id.taskId = :taskId
        """)
    TaskStats getTaskStatsByTaskId(@Param("taskId") UUID taskId);
}