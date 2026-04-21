package com.technokratos.userservice.controller;

import com.technokratos.userservice.dto.request.UserRegisterRequest;
import com.technokratos.userservice.dto.response.UserResponse;
import com.technokratos.userservice.api.UserApi;
import com.technokratos.userservice.services.UserService;
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
    public ResponseEntity<UserResponse> getUserById(UUID id) {
        return new ResponseEntity<>(userService.getOne(id), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<UserResponse> createUser(UserRegisterRequest dto) {
        return new ResponseEntity<>(userService.create(dto), HttpStatus.CREATED);
    }
}

