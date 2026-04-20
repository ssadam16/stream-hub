package com.technokratos.userservice.mappers;

import com.technokratos.userservice.dto.request.UserRegisterRequest;
import com.technokratos.userservice.dto.response.UserResponse;
import com.technokratos.userservice.entities.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    UserEntity toEntity(UserResponse userResponse);

    List<UserResponse> toUserResponse(List<UserEntity> userEntity);

    UserResponse toUserResponse(UserEntity userEntity);

    UserEntity toEntity(UserRegisterRequest userRegisterRequest);
}