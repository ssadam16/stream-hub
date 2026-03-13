package com.technokratos.userservice.services;

import com.technokratos.common.enums.Role;
import com.technokratos.userservice.api.dto.request.UserRegisterRequest;
import com.technokratos.userservice.api.dto.response.UserResponse;
import com.technokratos.userservice.entities.UserEntity;
import com.technokratos.userservice.handler.exceptions.UserNotFoundException;
import com.technokratos.userservice.mappers.UserMapper;
import com.technokratos.userservice.repositories.UserRepository;
import com.technokratos.userservice.security.details.CustomUserDetails;
import com.technokratos.userservice.security.details.CustomUserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponse> getAll() {
        return userMapper.toUserResponse(userRepository.findAll());
    }

    @Transactional
    public UserResponse create(UserRegisterRequest dto) {
        log.info("Add user to system");
        UserEntity userEntity = userMapper.toEntity(dto);

        userEntity.setHashPassword(passwordEncoder.encode(dto.password()));
        userEntity.setRole(Role.USER);

        return userMapper.toUserResponse(userRepository.save(userEntity));
    }

    public UserResponse getOne(UUID id) {
        log.info("Find user with id {}", id);
        Optional<UserEntity> userEntityOptional = userRepository.findById(id);
        return userMapper.toUserResponse(userEntityOptional.orElseThrow(() ->
                new UserNotFoundException(id)));
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User with email %s not found".formatted(email)));

        return new CustomUserDetailsImpl(user);
    }
}
