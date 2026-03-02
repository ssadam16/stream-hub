package com.technokratos.userservice.mappers;

import com.technokratos.userservice.api.dto.request.UserRegisterRequest;
import com.technokratos.userservice.api.dto.response.UserResponse;
import com.technokratos.userservice.entities.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface UserMapper {
    UserEntity toEntity(UserResponse userResponse);

    List<UserResponse> toUserResponse(List<UserEntity> userEntity);

    UserResponse toUserResponse(UserEntity userEntity);

    List<UserEntity> toEntity(List<UserResponse> userResponse);

    UserEntity toEntity(UserRegisterRequest userRegisterRequest);

    UserRegisterRequest toUserRequest(UserEntity userEntity);
}