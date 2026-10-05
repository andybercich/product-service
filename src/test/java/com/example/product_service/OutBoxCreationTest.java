package com.example.product_service;

import com.example.product_service.Enums.OutboxEventStatus;
import com.example.product_service.Models.Category;
import com.example.product_service.Models.OutboxEvent;
import com.example.product_service.Models.Product;
import com.example.product_service.Repository.CategoryRepository;
import com.example.product_service.Repository.OutBoxEventRepository;
import com.example.product_service.Repository.ProductRepository;
import com.example.product_service.Service.ProductService;
import org.example.Events.StockMovementType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;


@SpringBootTest(
        properties = {
                // Internal
                "internal.secret=test-internal-secret",

                // JWT
                "jwt.secret=test-jwt-secret-daaweda454f87845",
                "jwt.expiration=3600000",

                // MySQL
                "spring.datasource.url=${MYSQL_URL}",
                "spring.datasource.username=${MYSQL_USERNAME}",
                "spring.datasource.password=${MYSQL_PASSWORD}",

                // Eureka
                "eureka.client.service-url.defaultZone=${EUREKA_URL}",
                "eureka.client.register-with-eureka=false",
                "eureka.client.fetch-registry=false",

                // Kafka
                "spring.kafka.bootstrap-servers=${KAFKA_SERVER}",

        }
)
@Transactional
class OutBoxCreationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OutBoxEventRepository outboxEventRepository;

    @Test
    void changeStock_shouldSaveProductAndOutboxEvent() {

        Category category = new Category();
        category.setDenomination("Bebidas");

        Product product = new Product();
        product.setName("Coca Cola");
        product.setStock(10);
        product.setPriceSale(new BigDecimal("1.50"));
        product.setPriceBuy(new BigDecimal("1.00"));
        product.setCategory(category);

        category = categoryRepository.save(category);
        product.setCategory(category);

        product = productRepository.save(product);

        boolean result = productService.changeStock(
                product.getId(),
                5,
                StockMovementType.RESTOCK
        );

        assertTrue(result);

        Product updatedProduct = productRepository
                .findById(product.getId())
                .orElseThrow();

        assertEquals(15, updatedProduct.getStock());

        List<OutboxEvent> events =
                outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                        OutboxEventStatus.PENDING
                );

        assertEquals(1, events.size());

        OutboxEvent event = events.get(0);

        assertNotNull(event.getEventId());
        assertEquals("STOCK_MOVEMENT", event.getEventType());
        assertEquals("stock-movement-events", event.getTopic());
        assertEquals(OutboxEventStatus.PENDING, event.getStatus());
        assertEquals(0, event.getRetryCount());
        assertNotNull(event.getPayload());
    }


    @Test
    void changeStock_loss_shouldDecreaseStockAndCreateOutboxEvent() {

        Category category = new Category();
        category.setDenomination("Bebidas");
        category = categoryRepository.save(category);

        Product product = new Product();
        product.setName("Coca Cola");
        product.setCod("COKE-001");
        product.setCategory(category);
        product.setPriceBuy(new BigDecimal("1.00"));
        product.setPriceSale(new BigDecimal("1.50"));
        product.setStock(5);
        product.setDescription("Refreshing beverage");
        product.setStock(10);
        product.setStockMin(6);



        Product productSave = productRepository.save(product);

        boolean result = productService.changeStock(
                productSave.getId(),
                3,
                StockMovementType.LOSS
        );

        assertTrue(result);

        Product updatedProduct =
                productRepository.findById(product.getId())
                        .orElseThrow();

        assertEquals(7, updatedProduct.getStock());

        List<OutboxEvent> events =
                outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                        OutboxEventStatus.PENDING
                );

        assertEquals(1, events.size());

        assertEquals(
                "STOCK_MOVEMENT",
                events.get(0).getEventType()
        );
    }
    @Test
    void changeStock_loss_shouldRejectWhenStockIsInsufficient() {

        Category category = new Category();
        category.setDenomination("Bebidas");
        category = categoryRepository.save(category);

        Product product = new Product();
        product.setName("Coca Cola");
        product.setCod("COKE-001");
        product.setCategory(category);
        product.setPriceBuy(new BigDecimal("1.00"));
        product.setPriceSale(new BigDecimal("1.50"));
        product.setStock(2);
        product.setDescription("Refreshing beverage");
        product.setStockMin(6);

        Product productSave = productRepository.save(product);

        long eventsBefore = outboxEventRepository.count();

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.changeStock(
                        productSave.getId(),
                        5,
                        StockMovementType.LOSS
                )
        );

        Product unchanged = productRepository.findById(productSave.getId())
                .orElseThrow();

        assertEquals(2, unchanged.getStock());

        assertEquals(
                eventsBefore,
                outboxEventRepository.count()
        );
    }

}

