package com.technokratos.solutionservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.mockito.Mockito.mock;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Import(SolutionServiceApplicationTests.TestKafkaConfig.class)
class SolutionServiceApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("solutionsdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:6.0"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.kafka.consumer.auto-startup", () -> "false");
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
    }

    @Test
    void contextLoads() {
    }

    @Configuration
    static class TestKafkaConfig {

        @Bean
        @Primary
        public ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory() {
            ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                    new ConcurrentKafkaListenerContainerFactory<>();
            factory.setConsumerFactory(mock(ConsumerFactory.class));
            factory.setAutoStartup(false);
            return factory;
        }

        @Bean
        @Primary
        @SuppressWarnings("unchecked")
        public KafkaTemplate<Object, Object> kafkaTemplate() {
            return mock(KafkaTemplate.class);
        }
    }
}