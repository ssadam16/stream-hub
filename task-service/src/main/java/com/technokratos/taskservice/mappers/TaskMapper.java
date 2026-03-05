package com.technokratos.taskservice.mappers;

import com.technokratos.taskservice.api.dto.request.TaskCreatingRequest;
import com.technokratos.taskservice.api.dto.response.TaskResponse;
import com.technokratos.taskservice.entities.Task;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface TaskMapper {
    Task toTask(TaskCreatingRequest request);
    TaskResponse toTaskResponse(Task task);
}
