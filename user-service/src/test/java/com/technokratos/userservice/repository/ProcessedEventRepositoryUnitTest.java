package com.technokratos.userservice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Testcontainers
@Tag("integration")
@DisplayName("Processed Event Repository Tests")
class ProcessedEventRepositoryUnitTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ProcessedEventRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ProcessedEventRepository(jdbcTemplate);
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS processed_events (" +
                "event_id UUID PRIMARY KEY, " +
                "processed_at TIMESTAMP NOT NULL" +
                ")");
        jdbcTemplate.execute("DELETE FROM processed_events");
    }

    @Test
    @DisplayName("Should return true for first time event")
    void shouldReturnTrueForFirstTime() {
        UUID eventId = UUID.randomUUID();

        boolean result = repository.tryMarkProcessed(eventId);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false for duplicate event")
    void shouldReturnFalseForDuplicate() {
        UUID eventId = UUID.randomUUID();

        boolean first = repository.tryMarkProcessed(eventId);
        boolean second = repository.tryMarkProcessed(eventId);

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    @Test
    @DisplayName("Should handle multiple different events")
    void shouldHandleMultipleDifferentEvents() {
        UUID event1 = UUID.randomUUID();
        UUID event2 = UUID.randomUUID();
        UUID event3 = UUID.randomUUID();

        assertThat(repository.tryMarkProcessed(event1)).isTrue();
        assertThat(repository.tryMarkProcessed(event2)).isTrue();
        assertThat(repository.tryMarkProcessed(event3)).isTrue();
        assertThat(repository.tryMarkProcessed(event1)).isFalse();
        assertThat(repository.tryMarkProcessed(event2)).isFalse();
    }
}