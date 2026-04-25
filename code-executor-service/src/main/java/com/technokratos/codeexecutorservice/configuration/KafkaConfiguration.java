package com.technokratos.codeexecutorservice.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.List;

@Configuration
@EnableKafka
public class KafkaConfiguration {

    @Bean
    public NewTopic codeExecutionResultTopic() {
        return TopicBuilder.name("code-execution-results")
                .partitions(3)
                .replicas(3)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .build();
    }

    @Bean
    public RetryTopicConfiguration retryTopicConfiguration(KafkaTemplate<Object, Object> template) {
        return RetryTopicConfigurationBuilder
                .newInstance()
                .maxAttempts(4)
                .exponentialBackoff(1000, 5, 30000)
                .dltHandlerMethod("codeExecutionService", "handleDlqMessage")
                .includeTopics(List.of("code-execution-topic"))
                .dltSuffix("-dlq")
                .retryTopicSuffix("-retry")
                .create(template);
    }

    //Стоит автогенерация и настройка топиков из аннотации.
    //То, что внизу может пригодится, если retry и dlq будут использоваться в других местах.
    //А еще (возможно) нужна будет фабрика для обработки ошибок, хотя должна быть настройка через проперти.
//    @Bean
//    public NewTopic codeExecutionRequestsRetryTopic() {
//        return TopicBuilder.name("code-execution-requests-retry")
//                .partitions(3)
//                .replicas(3)
//                .build();
//    }
//
//    @Bean
//    public NewTopic codeExecutionRequestsDlqTopic() {
//        return TopicBuilder.name("code-execution-requests-dlq")
//                .partitions(3)
//                .replicas(3)
//                .build();
//    }
}
