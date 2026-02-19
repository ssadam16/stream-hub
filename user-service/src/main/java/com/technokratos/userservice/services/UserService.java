package com.technokratos.userservice.services;

import com.technokratos.userservice.api.dto.UserRequest;
import com.technokratos.userservice.entities.UserEntity;
import com.technokratos.userservice.mappers.UserMapper;
import com.technokratos.userservice.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<UserEntity> getAll() {
        return userRepository.findAll();
    }

    public UserEntity create(UserRequest dto) {
        UserEntity userEntity = userMapper.toEntity(dto);

        return userRepository.save(userEntity);
    }
}
