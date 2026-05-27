package com.technokratos.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestSecurityConfig.class)
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}