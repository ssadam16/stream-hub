package com.technokratos.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Import(SecurityTestConfiguration.class)
class ApiGatewayApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @Test
    void contextLoads() {
        assertThat(reactiveJwtDecoder).isNotNull();
    }

    @Test
    void actuatorHealthIsPublic() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void apiDocsPathIsPublic() {
        webTestClient.get()
                .uri("/v3/api-docs/swagger-config")
                .exchange()
                .expectStatus().value(status -> assertThat(status).isNotEqualTo(401));
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() {
        webTestClient.get()
                .uri("/api/v1/tasks/123")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith("application/json")
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.error").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.code").isEqualTo("MISSING_OR_INVALID_TOKEN")
                .jsonPath("$.path").isEqualTo("/api/v1/tasks/123");
    }

    @Test
    void adminEndpointWithoutTokenReturns401() {
        webTestClient.get()
                .uri("/api/v1/admin/tasks")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("MISSING_OR_INVALID_TOKEN");
    }

    @Test
    void invalidTokenReturns401() {
        webTestClient.get()
                .uri("/api/v1/tasks/1")
                .header("Authorization", "Bearer not-a-real-jwt")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error").isEqualTo("UNAUTHORIZED");
    }
}