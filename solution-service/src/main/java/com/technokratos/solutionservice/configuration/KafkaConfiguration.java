package com.technokratos.solutionservice.configuration;

import com.technokratos.common.events.CodeExecutionRequest;
import com.technokratos.common.events.CodeExecutionResult;
import com.technokratos.common.events.SolutionCompletedEvent;
import com.technokratos.common.events.TaskSubmissionEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.apache.kafka.common.serialization.UUIDSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

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

    // ---------- producers ----------

    private Map<String, Object> producerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, UUIDSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);
        return props;
    }

    @Bean
    public KafkaTemplate<UUID, CodeExecutionRequest> kafkaExecution() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps()));
    }

    @Bean
    public KafkaTemplate<UUID, TaskSubmissionEvent> kafkaStats() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps()));
    }

    @Bean
    public KafkaTemplate<UUID, SolutionCompletedEvent> kafkaPlagiarism() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps()));
    }

    @Bean
    public KafkaTemplate<UUID, Object> kafkaTemplate() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps()));
    }

    // ---------- consumer for code-execution-results ----------

    @Bean
    public ConsumerFactory<UUID, CodeExecutionResult> resultConsumerFactory() {
        JsonDeserializer<CodeExecutionResult> json = new JsonDeserializer<>(CodeExecutionResult.class);
        json.addTrustedPackages("com.technokratos.common.events", "com.technokratos.common.dto");
        json.setUseTypeHeaders(false);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props, new UUIDDeserializer(), json);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<UUID, CodeExecutionResult> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, CodeExecutionResult> f =
                new ConcurrentKafkaListenerContainerFactory<>();
        f.setConsumerFactory(resultConsumerFactory());
        return f;
    }
}
