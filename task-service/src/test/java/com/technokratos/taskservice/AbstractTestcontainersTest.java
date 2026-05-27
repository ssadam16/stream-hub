package com.technokratos.taskservice;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractTestcontainersTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine")  // Используй конкретную версию
    )
            .withDatabaseName("task-db")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)));

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0")  // Правильное имя образа
    )
            .withReuse(true)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // Проверяем, что контейнеры запущены
        if (!postgres.isRunning()) {
            throw new RuntimeException("PostgreSQL container failed to start!");
        }
        if (!kafka.isRunning()) {
            throw new RuntimeException("Kafka container failed to start!");
        }

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        // Дополнительные настройки Hibernate
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.show-sql", () -> "false");
    }

    @BeforeAll
    static void logContainersInfo() {
        System.out.println("=== Testcontainers Info ===");
        System.out.println("PostgreSQL JDBC URL: " + postgres.getJdbcUrl());
        System.out.println("PostgreSQL is running: " + postgres.isRunning());
        System.out.println("Kafka Bootstrap Servers: " + kafka.getBootstrapServers());
        System.out.println("Kafka is running: " + kafka.isRunning());
        System.out.println("==========================");
    }
}