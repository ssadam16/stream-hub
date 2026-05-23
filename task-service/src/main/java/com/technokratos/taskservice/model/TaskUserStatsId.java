package com.technokratos.taskservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TaskUserStatsId implements Serializable {
    @Column(name = "task_id")
    private UUID taskId;
    @Column(name = "user_id")
    private UUID userId;
}