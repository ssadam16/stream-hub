package com.technokratos.taskservice.configuration;

import com.technokratos.common.events.TaskSubmissionEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
@EnableKafka
public class KafkaConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<UUID, TaskSubmissionEvent> taskSubmissionConsumerFactory() {
        JsonDeserializer<TaskSubmissionEvent> json = new JsonDeserializer<>(TaskSubmissionEvent.class);
        json.addTrustedPackages("com.technokratos.common.events", "com.technokratos.common.dto");
        json.setUseTypeHeaders(false);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(props, new UUIDDeserializer(), json);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<UUID, TaskSubmissionEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, TaskSubmissionEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(taskSubmissionConsumerFactory());
        return factory;
    }
}
