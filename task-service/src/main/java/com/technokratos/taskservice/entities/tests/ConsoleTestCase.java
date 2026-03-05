package com.technokratos.taskservice.entities.tests;

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
@Table(name = "console_test_cases")
public class ConsoleTestCase extends TestCase {

    @Column(nullable = false, length = 1023)
    private String input;

    @Column(nullable = false, length = 1023)
    private String expectedOutput;
}