package com.technokratos.apigateway;

import com.technokratos.apigateway.configuration.KeycloakJwtAuthenticationConverter;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtAuthenticationConverterTest {

    private final KeycloakJwtAuthenticationConverter converter = new KeycloakJwtAuthenticationConverter();

    @Test
    void extractsRolesFromClaim() {
        Jwt jwt = jwtWithClaim("roles", List.of("stream_hub.user", "stream_hub.admin"));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("stream_hub.user", "stream_hub.admin");
    }

    @Test
    void returnsEmptyWhenRolesClaimMissing() {
        Jwt jwt = jwtWithClaim("scope", "openid");

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void returnsEmptyForEmptyRolesList() {
        Jwt jwt = jwtWithClaim("roles", List.of());

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    private Jwt jwtWithClaim(String name, Object value) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .subject("test-user")
                .claim(name, value)
                .build();
    }
}