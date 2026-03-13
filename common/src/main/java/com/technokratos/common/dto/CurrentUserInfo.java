package com.technokratos.common.dto;

import com.technokratos.common.enums.Role;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record CurrentUserInfo(
        String email,
        List<Role> roles,
        UUID id
) {
    public boolean isAuthenticated() {
        return id != null;
    }

    public boolean hasRole(Role role) {
        return roles != null && roles.contains(role);
    }
}