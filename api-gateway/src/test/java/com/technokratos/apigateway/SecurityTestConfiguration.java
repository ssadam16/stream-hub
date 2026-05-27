package com.technokratos.apigateway;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@TestConfiguration
public class SecurityTestConfiguration {

    @Bean
    @Primary
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        ReactiveJwtDecoder mock = Mockito.mock(ReactiveJwtDecoder.class);

        // На любой токен кидаем BadJwtException
        when(mock.decode(anyString()))
                .thenReturn(Mono.error(new BadJwtException("Invalid token")));

        return mock;
    }

    @Bean
    @Primary
    public ReactiveClientRegistrationRepository reactiveClientRegistrationRepository() {
        ClientRegistration keycloak = ClientRegistration.withRegistrationId("keycloak")
                .clientId("test-client")
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/keycloak")
                .scope("openid", "profile", "email")
                .authorizationUri("http://localhost:8090/realms/stream-hub/protocol/openid-connect/auth")
                .tokenUri("http://localhost:8090/realms/stream-hub/protocol/openid-connect/token")
                .userInfoUri("http://localhost:8090/realms/stream-hub/protocol/openid-connect/userinfo")
                .jwkSetUri("http://localhost:8090/realms/stream-hub/protocol/openid-connect/certs")
                .issuerUri("http://localhost:8090/realms/stream-hub")
                .userNameAttributeName("sub")
                .build();

        return new InMemoryReactiveClientRegistrationRepository(keycloak);
    }
}