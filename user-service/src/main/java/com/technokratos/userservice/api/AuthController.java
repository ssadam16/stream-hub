package com.technokratos.userservice.api;

import com.technokratos.userservice.api.dto.request.TokenValidationRequest;
import com.technokratos.userservice.api.dto.request.UserAuthRequest;
import com.technokratos.userservice.api.dto.response.TokenValidationResponse;
import com.technokratos.userservice.api.dto.response.UserAuthResponse;
import com.technokratos.userservice.security.details.CustomUserDetails;
import com.technokratos.userservice.services.JwtService;
import com.technokratos.userservice.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    public ResponseEntity<UserAuthResponse> getToken(UserAuthRequest request) {
        log.info("The user with email {} is trying to authenticate", request.email());

        Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        log.info("The user is authenticated: {}", auth.isAuthenticated());

        CustomUserDetails userDetails = (CustomUserDetails) userService.loadUserByUsername(request.email());

        String jwt = jwtService.generateToken(userDetails);

        log.info("Generated jwt={}", jwt);

        return ResponseEntity.ok(
                UserAuthResponse.builder()
                        .token(jwt)
                        .build()
        );
    }

    @Override
    public ResponseEntity<TokenValidationResponse> validateToken(TokenValidationRequest request) {
        log.info("Validate token {}", request.token());

        TokenValidationResponse tokenValidationResponse = jwtService.validateToken(request);

        if (tokenValidationResponse.isValid()) {
            log.info("Token={} is valid", request.token());
            return ResponseEntity.ok(tokenValidationResponse);
        }

        log.warn("Token={} is invalid", request.token());
        return ResponseEntity.status(401).body(tokenValidationResponse);
    }

}
