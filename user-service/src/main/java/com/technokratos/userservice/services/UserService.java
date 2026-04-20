package com.technokratos.userservice.services;

import com.technokratos.common.enums.Role;
import com.technokratos.userservice.dto.request.UserRegisterRequest;
import com.technokratos.userservice.dto.response.UserResponse;
import com.technokratos.userservice.entities.UserEntity;
import com.technokratos.userservice.handler.exceptions.UserNotFoundException;
import com.technokratos.userservice.mappers.UserMapper;
import com.technokratos.userservice.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public List<UserResponse> getAll() {
        return userMapper.toUserResponse(userRepository.findAll());
    }

    @Transactional
    public UserResponse create(UserRegisterRequest request) {

        UserEntity user = UserEntity.builder()
                .username(request.username())
                .email(request.email())
                .keycloakId(request.keycloakId())
                .role(Role.USER)
                .build();

        return userMapper.toUserResponse(userRepository.save(user));
    }

    public UserResponse getOne(UUID id) {
        log.info("Find user with id {}", id);
        Optional<UserEntity> userEntityOptional = userRepository.findById(id);
        return userMapper.toUserResponse(userEntityOptional.orElseThrow(() ->
                new UserNotFoundException(id)));
    }
}
