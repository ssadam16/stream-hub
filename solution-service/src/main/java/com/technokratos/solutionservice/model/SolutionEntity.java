package com.technokratos.solutionservice.model;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.common.enums.SolutionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.sql.Timestamp;
import java.util.UUID;

@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "solutions")
public class SolutionEntity extends BaseEntity {

    @Enumerated(value = EnumType.STRING)
    @Column(name = "language")
    private ProgrammingLanguage language;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "status", length = 50)
    private SolutionStatus status;

    @Column(name = "code", nullable = false, length = Integer.MAX_VALUE)
    private String code;
}