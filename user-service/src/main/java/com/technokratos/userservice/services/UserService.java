package com.technokratos.userservice.services;

import com.technokratos.userservice.api.dto.UserRequest;
import com.technokratos.userservice.api.dto.UserResponse;
import com.technokratos.userservice.entities.UserEntity;
import com.technokratos.userservice.handler.exceptions.UserNotFoundException;
import com.technokratos.userservice.mappers.UserMapper;
import com.technokratos.userservice.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@Transactional
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userMapper.toUserResponse(userRepository.findAll());
    }

    public UserResponse create(UserRequest dto) {
        UserEntity userEntity = userMapper.toEntity(dto);

        userEntity.setHashPassword(passwordEncoder.encode(dto.password()));

        return userMapper.toUserResponse(userRepository.save(userEntity));
    }

    @Transactional(readOnly = true)
    public UserResponse getOne(UUID id) {
        Optional<UserEntity> userEntityOptional = userRepository.findById(id);
        return userMapper.toUserResponse(userEntityOptional.orElseThrow(() ->
                new UserNotFoundException(id)));
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new UsernameNotFoundException(email)
        );
    }
}
