package com.technokratos.solutionservice.repository;

import com.technokratos.common.enums.OutboxEventStatus;
import com.technokratos.solutionservice.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("""
    SELECT e FROM OutboxEvent e
    WHERE e.status = :status AND e.nextRetryAt <= :now
    ORDER BY e.createdAt ASC
    LIMIT 100
    """)
    List<OutboxEvent> findTop100ByStatusOrderByCreatedAt(
            @Param("status") OutboxEventStatus status,
            @Param("now") Instant now
    );

    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = :status AND e.publishedAt < :threshold")
    int deleteByStatusAndPublishedAtBefore(
            @Param("status") OutboxEventStatus status,
            @Param("threshold") Instant threshold
    );
}
