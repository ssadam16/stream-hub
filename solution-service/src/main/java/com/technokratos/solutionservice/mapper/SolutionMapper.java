package com.technokratos.solutionservice.mapper;

import com.technokratos.common.events.SolutionCreationEvent;
import com.technokratos.solutionservice.dto.SolutionRequest;
import com.technokratos.solutionservice.dto.SolutionResponse;
import com.technokratos.solutionservice.model.SolutionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface SolutionMapper {
    SolutionEntity toEntity(SolutionRequest solutionRequest);

    SolutionRequest toSolutionRequest(SolutionEntity solutionEntity);

    SolutionEntity toEntity(SolutionResponse solutionResponse);

    SolutionResponse toSolutionResponse(SolutionEntity solutionEntity);

    List<SolutionResponse> toSolutionResponse(List<SolutionEntity> solutionEntities);

    SolutionCreationEvent toSolutionCreationEvent(SolutionEntity entity);

    default Page<SolutionResponse> toResponsePage(Page<SolutionEntity> page) {
        return page.map(this::toSolutionResponse);
    }
}