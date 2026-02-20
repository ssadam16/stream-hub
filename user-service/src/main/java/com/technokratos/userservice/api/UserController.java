package com.technokratos.userservice.api;

import com.technokratos.userservice.api.dto.UserRequest;
import com.technokratos.userservice.api.dto.UserResponse;
import com.technokratos.userservice.entities.UserEntity;
import com.technokratos.userservice.mappers.UserMapper;
import com.technokratos.userservice.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    @Override
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return new ResponseEntity<>(userService.getAll(), HttpStatus.OK);
    }


    @Override
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return new ResponseEntity<>(userService.getOne(id), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<UserResponse> createUser(@RequestBody @Valid UserRequest dto) {
        return new ResponseEntity<>(userService.create(dto), HttpStatus.CREATED);
    }
}

