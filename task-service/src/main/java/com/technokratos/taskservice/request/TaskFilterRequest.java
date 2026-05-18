package com.technokratos.taskservice.request;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.common.enums.TaskTestType;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
public class TaskFilterRequest {
    private Set<TaskDifficulty> difficulties;
    private UUID authorId;
    private Set<TaskTestType> types;
    private Short minRating;
    private Short maxRating;
    private String searchText;
    private Integer page;
    private Integer size;
    private String sortBy;

    @Pattern(regexp = "asc|desc", message = "sortType must be 'asc' or 'desc'")
    private String sortType;
}