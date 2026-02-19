package com.technokratos.userservice.api;

import com.technokratos.userservice.api.dto.UserRequest;
import com.technokratos.userservice.api.dto.UserResponse;
import com.technokratos.userservice.entities.UserEntity;
import com.technokratos.userservice.mappers.UserMapper;
import com.technokratos.userservice.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping
    public List<UserResponse> getAll() {
        List<UserEntity> users = userService.getAll();
        return userMapper.toUserResponse(users);
    }

    @PostMapping
    public UserResponse create(@RequestBody UserRequest dto) {
        return userMapper.toUserResponse(userService.create(dto));
    }
}

