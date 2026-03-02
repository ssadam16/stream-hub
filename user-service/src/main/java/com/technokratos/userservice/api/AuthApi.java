package com.technokratos.userservice.api;

import com.technokratos.userservice.api.dto.request.TokenValidationRequest;
import com.technokratos.userservice.api.dto.request.UserAuthRequest;
import com.technokratos.userservice.api.dto.response.TokenValidationResponse;
import com.technokratos.userservice.api.dto.response.UserAuthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/auth/token")
public interface AuthApi {

    @PostMapping
    ResponseEntity<UserAuthResponse> getToken(@RequestBody UserAuthRequest request);

    @PostMapping("/validate")
    ResponseEntity<TokenValidationResponse> validateToken(@RequestBody TokenValidationRequest request);
}
