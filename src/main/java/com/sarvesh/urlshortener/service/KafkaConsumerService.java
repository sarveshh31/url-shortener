package com.sarvesh.urlshortener.service;

import com.sarvesh.urlshortener.dto.ClickEventMessage;
import com.sarvesh.urlshortener.entity.ClickEvent;
import com.sarvesh.urlshortener.repository.ClickEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final ClickEventRepository clickEventRepository;

    @KafkaListener(
            topics = "click-events",
            groupId = "click-consumer-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ClickEventMessage message) {
        ClickEvent event = ClickEvent.builder()
                .shortCode(message.getShortCode())
                .clickedAt(message.getClickedAt())
                .ipAddress(message.getIpAddress())
                .userAgent(message.getUserAgent())
                .referer(message.getReferer())
                .build();
        clickEventRepository.save(event);
    }
}