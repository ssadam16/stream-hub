package com.technokratos.taskservice.api.dto.response.tests;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@SuperBuilder
@Getter
public class TestCaseResponse {
    protected UUID taskId;
    protected boolean isExample;
    protected short order;
}
