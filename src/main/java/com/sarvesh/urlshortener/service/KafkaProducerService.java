package com.sarvesh.urlshortener.service;

import com.sarvesh.urlshortener.dto.ClickEventMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, ClickEventMessage> kafkaTemplate;

    private static final String TOPIC = "click-events";

    public void publishClickEvent(ClickEventMessage event) {
        kafkaTemplate.send(TOPIC, event.getShortCode(), event);
    }
}