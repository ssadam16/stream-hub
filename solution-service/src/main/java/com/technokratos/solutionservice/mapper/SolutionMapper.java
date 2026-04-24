package com.technokratos.solutionservice.mapper;

import com.technokratos.solutionservice.dto.SolutionRequest;
import com.technokratos.solutionservice.dto.SolutionResponse;
import com.technokratos.solutionservice.dto.SolutionResultResponse;
import com.technokratos.solutionservice.model.SolutionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface SolutionMapper {
    SolutionEntity toEntity(SolutionRequest solutionRequest);

    SolutionResponse toSolutionResponse(SolutionEntity solutionEntity);

    default Page<SolutionResponse> toResponsePage(Page<SolutionEntity> page) {
        return page.map(this::toSolutionResponse);
    }

    @Mapping(target = "solutionId", source = "id")
    SolutionResultResponse toResponse(SolutionEntity entity);
}