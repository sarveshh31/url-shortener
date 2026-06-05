package com.sarvesh.urlshortener.config;

import org.apache.kafka.common.serialization.StringDeserializer;
import com.sarvesh.urlshortener.dto.ClickEventMessage;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public ConsumerFactory<String, ClickEventMessage> consumerFactory(
            KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ClickEventMessage.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(),
                new JsonDeserializer<>(ClickEventMessage.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ClickEventMessage>
    kafkaListenerContainerFactory(
            ConsumerFactory<String, ClickEventMessage> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, ClickEventMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(false);
        return factory;
    }
}