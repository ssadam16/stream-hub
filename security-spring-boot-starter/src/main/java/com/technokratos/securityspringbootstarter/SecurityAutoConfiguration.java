package com.technokratos.securityspringbootstarter;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.stream.Collectors;

@AutoConfiguration
@EnableConfigurationProperties(SecurityStarterProperties.class)
@RequiredArgsConstructor
@EnableWebSecurity
@ConditionalOnClass(HttpSecurity.class)
@ConditionalOnProperty(prefix = "security.starter", matchIfMissing = true, havingValue = "true", name = "enabled")
public class SecurityAutoConfiguration {

    private final SecurityStarterProperties properties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(properties.getPermitAll()).permitAll()
                        .requestMatchers(properties.getInternalRoleEndpoint()).hasAuthority(ClientRole.INTERNAL.getRoleName())
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                );
        return http.build();
    }

    /*
        ROLE_ префикс не добавляем, потому что кейклок сам добавляет.
        Либо так requestMatchers("/api/v1/admin/**").hasRole("stream_hub.admin")
        Либо так requestMatchers("/api/v1/admin/**").hasAuthority("ROLE_stream_hub.admin")
     */


    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();

        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> roles = jwt.getClaimAsStringList(properties.getRolesClaim());

            return roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        });

        return jwtAuthenticationConverter;
    }

    @Bean
    public SecurityUtil securityUtils() {
        return new SecurityUtil(properties);
    }
}
