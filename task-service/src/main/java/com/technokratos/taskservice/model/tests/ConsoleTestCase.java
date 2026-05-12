package com.technokratos.taskservice.model.tests;

import com.technokratos.taskservice.model.BaseEntity;
import com.technokratos.taskservice.model.Task;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "test_cases")
public class ConsoleTestCase extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", referencedColumnName = "id", nullable = false)
    private Task task;

    @Column(nullable = false, length = 1023)
    private String input;

    @Column(nullable = false, length = 1023)
    private String expectedOutput;

    @Column(nullable = false)
    private boolean isHidden;
}