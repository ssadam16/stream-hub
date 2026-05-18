package com.technokratos.taskservice.model;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.common.enums.TaskTestType;
import com.technokratos.taskservice.model.tests.ConsoleTestCase;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;

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

    @Column(name = "external_id", unique = true)
    private String externalId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskTestType type;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ConsoleTestCase> testCases = new ArrayList<>();

    private Integer timeLimit;

    private Integer memoryLimit;

    @Column(nullable = false)
    @ColumnDefault("0")
    private Short rating;
}
