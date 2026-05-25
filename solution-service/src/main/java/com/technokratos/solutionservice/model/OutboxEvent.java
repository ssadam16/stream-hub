package com.technokratos.solutionservice.model;

import com.technokratos.common.enums.OutboxEventStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_event",
        indexes = {
                @Index(name = "idx_outbox_event_status_created", columnList = "status, created_at"),
                @Index(name = "idx_outbox_event_aggregate_id", columnList = "aggregate_id")
        }
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(nullable = false, length = 200)
    private String topic;

    @Column(name = "message_key")
    private UUID messageKey;

    @Column(columnDefinition = "jsonb", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventStatus status = OutboxEventStatus.NEW;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Builder.Default
    @Column(name = "next_retry_at", nullable = false)
    private Instant nextRetryAt = Instant.now();

    @Builder.Default
    @Column(name = "retry_count")
    private Integer retryCount = 0;
}