package com.technokratos.securityspringbootstarter;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class SecurityUtil {
    private final SecurityStarterProperties properties;

    public boolean isAdmin(Jwt jwt) {
        return getRoles(jwt).contains(ClientRole.ADMIN.getRoleName());
    }

    public boolean isUser(Jwt jwt) {
        return getRoles(jwt).contains(ClientRole.USER.getRoleName());
    }

    public boolean isOwner(Jwt jwt, UUID userId) {
        return UUID.fromString(jwt.getSubject()).equals(userId);
    }

    private List<String> getRoles(Jwt jwt) {
        return jwt.getClaimAsStringList(properties.getRolesClaim());
    }
}
