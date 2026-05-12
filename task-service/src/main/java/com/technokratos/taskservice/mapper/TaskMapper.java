package com.technokratos.taskservice.mapper;

import com.technokratos.taskservice.parser.TaskImportRequest;
import com.technokratos.taskservice.request.TaskCreatingRequest;
import com.technokratos.common.dto.TaskResponse;
import com.technokratos.taskservice.model.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring",
        uses = {ConsoleTestCaseMapper.class}
)
public interface TaskMapper {
    Task toEntity(TaskCreatingRequest request);
    Task toEntity(TaskImportRequest request);
    TaskResponse toTaskResponse(Task task);
    List<TaskResponse> toTaskResponseList(List<Task> tasks);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromRequest(TaskImportRequest request, @MappingTarget Task entity);
}
