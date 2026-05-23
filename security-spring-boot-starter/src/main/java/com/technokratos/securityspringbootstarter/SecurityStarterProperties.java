package com.technokratos.securityspringbootstarter;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;


@Getter
@ConfigurationProperties(prefix = "security.starter")
@Setter
public class SecurityStarterProperties {

    /**
     * Имя claim в JWT, который содержит роли пользователя
     */
    private String rolesClaim = "roles";

    /**
     * Все permit all эндпоинты для security
     */
    private String[] permitAll = new String[]{
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/webjars/**",
            "/swagger-resources/**",
            "/actuator/**"
    };

    private String[] internalRoleEndpoint = new String[]{
            "/internal/**"
    };
}
