package com.technokratos.securityspringbootstarter;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;


@Getter
@ConfigurationProperties(prefix = "security.starter")
@Setter
public class SecurityStarterProperties {

    private String rolesClaim = "roles";

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
