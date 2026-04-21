package com.technokratos.taskservice.mappers;

import com.technokratos.taskservice.api.dto.request.tests.ConsoleTestCaseCreatingRequest;
import com.technokratos.common.dto.tests.ConsoleTestCaseResponse;
import com.technokratos.taskservice.entities.tests.ConsoleTestCase;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface ConsoleTestCaseMapper {
    List<ConsoleTestCase> toConsoleTestCaseList(List<ConsoleTestCaseCreatingRequest> requests);
    ConsoleTestCase toConsoleTestCase(ConsoleTestCaseCreatingRequest request);
    List<ConsoleTestCaseResponse> toConsoleTestCaseResponse (List<ConsoleTestCase> requests);
}
