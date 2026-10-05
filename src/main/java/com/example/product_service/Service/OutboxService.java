package com.example.product_service.Service;


import com.example.product_service.Enums.OutboxEventStatus;
import com.example.product_service.Models.OutboxEvent;
import com.example.product_service.Repository.OutBoxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OutboxService {
    @Autowired
    private OutBoxEventRepository outboxEventRepository;

    @Autowired
    private ObjectMapper objectMapper;


    public void saveEvent(
            UUID eventId,
            String eventType,
            String topic,
            Object event) throws JsonProcessingException {

        String payload = objectMapper.writeValueAsString(event);

        OutboxEvent outboxEvent = new OutboxEvent();

        outboxEvent.setEventId(eventId);
        outboxEvent.setTopic(topic);
        outboxEvent.setPayload(payload);
        outboxEvent.setEventType(eventType);
        outboxEvent.setStatus(OutboxEventStatus.PENDING);
        outboxEvent.setRetryCount(0);
        outboxEvent.setCreatedAt(LocalDateTime.now());
        outboxEvent.setLastAttemptAt(null);
        outboxEvent.setNextAttemptAt(null);
        outboxEventRepository.save(outboxEvent);
    }
}
