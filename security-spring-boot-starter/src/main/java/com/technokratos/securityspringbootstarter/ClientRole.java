package com.technokratos.securityspringbootstarter;

import lombok.Getter;

@Getter
public enum ClientRole {
    ADMIN("ROLE_stream_hub.admin"),
    USER("ROLE_stream_hub.user"),
    INTERNAL("ROLE_stream_hub.internal");

    private final String roleName;

    ClientRole(String roleName) {
        this.roleName = roleName;
    }
}
