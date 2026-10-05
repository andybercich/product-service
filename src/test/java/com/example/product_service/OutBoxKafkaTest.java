package com.example.product_service;

import com.example.product_service.Enums.OutboxEventStatus;
import com.example.product_service.Models.Category;
import com.example.product_service.Models.OutboxEvent;
import com.example.product_service.Models.Product;
import com.example.product_service.Repository.CategoryRepository;
import com.example.product_service.Repository.OutBoxEventRepository;
import com.example.product_service.Repository.ProductRepository;
import com.example.product_service.Service.KafkaProducerService;
import com.example.product_service.Service.OutboxPublisher;
import com.example.product_service.Service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.Events.StockMovementEvent;
import org.example.Events.StockMovementType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutBoxKafkaTest {

    @Mock
    private OutBoxEventRepository outboxEventRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CompletableFuture<SendResult<String, Object>> future;

    @InjectMocks
    private OutboxPublisher outboxPublisher;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductService productService;

    @Test
    void shouldPublishPendingEventSuccessfully() throws Exception {

        UUID eventId = UUID.randomUUID();

        StockMovementEvent movementEvent = new StockMovementEvent(
                eventId,
                1L,
                "Coca Cola",
                StockMovementType.RESTOCK,
                10,
                15,
                5,
                "Bebidas",
                LocalDateTime.now()
        );

        OutboxEvent outboxEvent = new OutboxEvent();

        outboxEvent.setId(1L);
        outboxEvent.setEventId(eventId);
        outboxEvent.setEventType("STOCK_MOVEMENT");
        outboxEvent.setTopic("stock-movement-events");
        outboxEvent.setPayload("{}");
        outboxEvent.setStatus(OutboxEventStatus.PENDING);
        outboxEvent.setRetryCount(0);
        outboxEvent.setCreatedAt(LocalDateTime.now());
        outboxEvent.setNextAttemptAt(null);

        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.PENDING
        )).thenReturn(List.of(outboxEvent));

        when(objectMapper.readValue(
                outboxEvent.getPayload(),
                StockMovementEvent.class
        )).thenReturn(movementEvent);

        when(kafkaProducerService.sendStockMovementEvent(movementEvent))
                .thenReturn(future);

        when(future.get()).thenReturn(null);

        outboxPublisher.publishPendingEvents();

        verify(kafkaProducerService)
                .sendStockMovementEvent(movementEvent);

        verify(outboxEventRepository)
                .save(outboxEvent);

        assertEquals(
                OutboxEventStatus.PUBLISHED,
                outboxEvent.getStatus()
        );

        assertNotNull(outboxEvent.getPublishedAt());

        assertNull(outboxEvent.getNextAttemptAt());

        assertEquals(1, outboxEvent.getRetryCount());
    }

    @Test
    void shouldKeepEventPendingWhenKafkaFails() throws Exception {

        UUID eventId = UUID.randomUUID();

        StockMovementEvent movementEvent = new StockMovementEvent(
                eventId,
                1L,
                "Coca Cola",
                StockMovementType.RESTOCK,
                10,
                15,
                5,
                "Bebidas",
                LocalDateTime.now()
        );

        OutboxEvent outboxEvent = new OutboxEvent();

        outboxEvent.setId(1L);
        outboxEvent.setEventId(eventId);
        outboxEvent.setEventType("STOCK_MOVEMENT");
        outboxEvent.setTopic("stock-movement-events");
        outboxEvent.setPayload("{}");
        outboxEvent.setStatus(OutboxEventStatus.PENDING);
        outboxEvent.setRetryCount(0);
        outboxEvent.setCreatedAt(LocalDateTime.now());
        outboxEvent.setNextAttemptAt(null);

        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.PENDING
        )).thenReturn(List.of(outboxEvent));

        when(objectMapper.readValue(
                outboxEvent.getPayload(),
                StockMovementEvent.class
        )).thenReturn(movementEvent);

        when(kafkaProducerService.sendStockMovementEvent(movementEvent))
                .thenReturn(future);

        when(future.get())
                .thenThrow(new ExecutionException(
                        new RuntimeException("Kafka unavailable")
                ));

        outboxPublisher.publishPendingEvents();

        verify(kafkaProducerService)
                .sendStockMovementEvent(movementEvent);

        verify(outboxEventRepository)
                .save(outboxEvent);

        assertEquals(
                OutboxEventStatus.PENDING,
                outboxEvent.getStatus()
        );

        assertEquals(1, outboxEvent.getRetryCount());

        assertNotNull(outboxEvent.getNextAttemptAt());

        assertNull(outboxEvent.getPublishedAt());
    }

    @Test
    void shouldSkipEventWhenNextAttemptIsInTheFuture() {

        OutboxEvent outboxEvent = new OutboxEvent();

        outboxEvent.setId(1L);
        outboxEvent.setEventId(UUID.randomUUID());
        outboxEvent.setEventType("STOCK_MOVEMENT");
        outboxEvent.setTopic("stock-movement-events");
        outboxEvent.setPayload("{}");
        outboxEvent.setStatus(OutboxEventStatus.PENDING);
        outboxEvent.setRetryCount(2);
        outboxEvent.setCreatedAt(LocalDateTime.now());

        outboxEvent.setNextAttemptAt(
                LocalDateTime.now().plusMinutes(5)
        );

        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.PENDING
        )).thenReturn(List.of(outboxEvent));

        outboxPublisher.publishPendingEvents();

        verifyNoInteractions(kafkaProducerService);

        verify(outboxEventRepository, never())
                .save(any());
    }


}