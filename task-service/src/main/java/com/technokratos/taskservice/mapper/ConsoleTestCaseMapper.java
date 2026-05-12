package com.technokratos.taskservice.mapper;

import com.technokratos.common.dto.tests.TestCasePayload;
import com.technokratos.taskservice.request.tests.ConsoleTestCaseCreatingRequest;
import com.technokratos.taskservice.model.tests.ConsoleTestCase;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface ConsoleTestCaseMapper {
    List<ConsoleTestCase> toConsoleTestCaseList(List<ConsoleTestCaseCreatingRequest> requests);
    ConsoleTestCase toConsoleTestCase(ConsoleTestCaseCreatingRequest request);
    List<TestCasePayload> toConsoleTestCaseResponse (List<ConsoleTestCase> requests);
}
