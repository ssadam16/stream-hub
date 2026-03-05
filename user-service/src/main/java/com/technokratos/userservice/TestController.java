package com.technokratos.userservice;

import com.technokratos.userservice.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/users")
public class TestController {

    private final UserService userService;

    public TestController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, String>> getProfile(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Roles") String roles,
            @RequestHeader(value = "X-User-Token-Valid", required = false) String tokenValid) {

        log.info("Request from user: {}", email);
        log.info("Token is valid: {}", tokenValid);
        log.info("User roles: {}", roles);

        UserDetails userDetails = userService.loadUserByUsername(email);

        return ResponseEntity.ok(Map.of(userDetails.getUsername(), userDetails.getPassword()));
    }



}
