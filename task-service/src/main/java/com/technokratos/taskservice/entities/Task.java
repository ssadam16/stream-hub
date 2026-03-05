package com.technokratos.taskservice.entities;

import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.common.enums.TaskTestType;
import com.technokratos.taskservice.entities.tests.ConsoleTestCase;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tasks")
public class Task extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String condition;

    @Column(nullable = false)
    private UUID authorId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskDifficulty difficulty;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskTestType type;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ConsoleTestCase> testCases = new ArrayList<>();

    private Integer timeLimit;

    private Integer memoryLimit;


}
