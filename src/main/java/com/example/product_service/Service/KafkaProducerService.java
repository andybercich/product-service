package com.example.product_service.Service;

import org.example.Events.StockMovementEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, Object>> sendStockMovementEvent(
            StockMovementEvent event) {

        String topic = "stock-movement-events";
        String key = event.getProductId().toString();
        return sendEvent(topic, key, event);

    }

    private CompletableFuture<SendResult<String, Object>> sendEvent(
            String topic,
            String key,
            Object event) {

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, exception) -> {

            if (exception != null) {

                log.error(
                        "Error publicando evento Kafka. topic={}, key={}, event={}",
                        topic,
                        key,
                        event.getClass().getSimpleName(),
                        exception
                );

                return;
            }

            log.info(
                    "Evento Kafka publicado correctamente. topic={}, key={}, partition={}, offset={}",
                    topic,
                    key,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        });

        return future;
    }
}