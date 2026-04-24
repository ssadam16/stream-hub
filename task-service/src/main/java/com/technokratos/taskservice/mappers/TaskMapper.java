package com.technokratos.taskservice.mappers;

import com.technokratos.taskservice.api.dto.request.TaskCreatingRequest;
import com.technokratos.common.dto.TaskResponse;
import com.technokratos.taskservice.entities.Task;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring",
        uses = {ConsoleTestCaseMapper.class}
)
public interface TaskMapper {
    Task toTask(TaskCreatingRequest request);
    TaskResponse toTaskResponse(Task task);
    List<TaskResponse> toTaskResponseList(List<Task> tasks);
}
