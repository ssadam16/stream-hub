package com.technokratos.taskservice.entities.tests;

import com.technokratos.taskservice.entities.BaseEntity;
import com.technokratos.taskservice.entities.Task;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@SuperBuilder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TestCase extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", referencedColumnName = "id", nullable = false)
    private Task task;

    @Column(nullable = false)
    private boolean isExample;

    @Column(nullable = false)
    private boolean isPassed;

    @Column(nullable = false)
    private short orderNum;
}