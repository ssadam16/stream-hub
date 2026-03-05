package com.technokratos.taskservice.api.dto.response.tests;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public class ConsoleTestCaseResponse extends TestCaseResponse {
    private String input;
    private String expectedOutput;
}
