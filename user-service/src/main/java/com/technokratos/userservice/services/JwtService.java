package com.technokratos.userservice.services;

import com.technokratos.userservice.api.dto.request.TokenValidationRequest;
import com.technokratos.userservice.api.dto.response.TokenValidationResponse;
import com.technokratos.userservice.security.details.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private Duration jwtExpiration;

    public String generateToken(CustomUserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        claims.put("roles", roles);
        claims.put("sub", userDetails.getUsername());

        Date issuedDate = new Date();
        Date expiredDate = new Date(issuedDate.getTime() + jwtExpiration.toMillis());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(issuedDate)
                .setExpiration(expiredDate)
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String getEmailFromToken(String token) {
        Claims allClaimsFromToken = getAllClaimsFromToken(token);
        String email = allClaimsFromToken.getSubject();
        log.info("Parse email={} from token={}", email, token);
        return email;
    }

    public List<String> getRolesFromToken(String token) {
        List<String> roles = getAllClaimsFromToken(token).get("roles", List.class);
        log.info("Parse roles={} from token={}", roles, token);
        return roles;
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public TokenValidationResponse validateToken(TokenValidationRequest request) {

        String token = request.token();

        try {
            return TokenValidationResponse.builder()
                    .isValid(true)
                    .timestamp(LocalDateTime.now())
                    .roles(getRolesFromToken(token))
                    .email(getEmailFromToken(token))
                    .build();
        } catch (Exception e) {
            return TokenValidationResponse.builder()
                    .isValid(false)
                    .error(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

}
