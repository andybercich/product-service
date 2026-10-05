package com.example.product_service.Service;


import com.example.product_service.Enums.OutboxEventStatus;
import com.example.product_service.Models.OutboxEvent;
import com.example.product_service.Repository.OutBoxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.Events.OrderCreatedEvent;
import org.example.Events.OrderDeletedEvent;
import org.example.Events.StockMovementEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class OutboxPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutBoxEventRepository outboxEventRepository;
    private final KafkaProducerService kafkaProducerService;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutBoxEventRepository outboxEventRepository, KafkaProducerService kafkaProducerService,
                           ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

        LocalDateTime now = LocalDateTime.now();
        List<OutboxEvent> events = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.PENDING
        );

        if (events.isEmpty()) {
            return;
        }
        log.info("Found {} pending outbox events", events.size());

        for (OutboxEvent event : events) {

            if (!shouldAttempt(event, now)) {
                continue;
            }
            publishEvent(event);
        }
    }

    private boolean shouldAttempt(OutboxEvent event, LocalDateTime now) {

        return event.getNextAttemptAt() == null || !event.getNextAttemptAt().isAfter(now);

    }

    private void publishEvent(OutboxEvent event) {

        LocalDateTime attemptTime = LocalDateTime.now();

        event.setLastAttemptAt(attemptTime);
        event.setRetryCount(event.getRetryCount() + 1);

        try {

            log.info(
                    "START publishing outbox event. eventId={}, topic={}, payload={}",
                    event.getEventId(),
                    event.getTopic(),
                    event.getPayload()
            );

            StockMovementEvent movementEvent =
                    objectMapper.readValue(
                            event.getPayload(),
                            StockMovementEvent.class
                    );

            log.info(
                    "Payload deserialized. productId={}, eventId={}",
                    movementEvent.getProductId(),
                    movementEvent.getEventId()
            );

            CompletableFuture<?> future =
                    kafkaProducerService.sendStockMovementEvent(movementEvent);

            log.info(
                    "Kafka send invoked. eventId={}",
                    event.getEventId()
            );

            future.get();

            log.info(
                    "Kafka future completed. eventId={}",
                    event.getEventId()
            );

            markAsPublished(event);

            log.info(
                    "Outbox event marked as published. eventId={}",
                    event.getEventId()
            );

        } catch (Exception exception) {

            log.error(
                    "Error publishing outbox event. eventId={}",
                    event.getEventId(),
                    exception
            );

            handlePublishFailure(event, attemptTime, exception);
        }
    }

    private void markAsPublished(OutboxEvent event) {
        event.setStatus(OutboxEventStatus.PUBLISHED);
        event.setPublishedAt(LocalDateTime.now());
        event.setNextAttemptAt(null);

        outboxEventRepository.save(event);

        log.info(
                "Outbox event published successfully. eventId={}, retries={}",
                event.getEventId(),
                event.getRetryCount()
        );
    }

    private void handlePublishFailure(OutboxEvent event, LocalDateTime attemptTime,
                                      Exception exception) {

        LocalDateTime nextAttempt = calculateNextAttempt(event.getRetryCount());

        event.setNextAttemptAt(nextAttempt);
        outboxEventRepository.save(event);
        log.error(
                "Error publishing outbox event. eventId={}, retryCount={}, nextAttemptAt={}",
                event.getEventId(),
                event.getRetryCount(),
                nextAttempt,
                exception
        );
    }

    private LocalDateTime calculateNextAttempt(int retryCount) {
        long delaySeconds = Math.min(300, (long) Math.pow(2, Math.min(retryCount, 8)));

        return LocalDateTime.now().plusSeconds(delaySeconds);
    }
}
