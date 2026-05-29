package com.technokratos.codeexecutorservice;

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

import static org.mockito.Mockito.mock;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "spring.kafka.consumer.auto-startup=false",
        "spring.kafka.listener.auto-startup=false"
})
@ActiveProfiles("test")
@Import(CodeExecutorServiceApplicationTests.TestKafkaConfig.class)
class CodeExecutorServiceApplicationTests {

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