package com.example.product_service;

import com.example.product_service.Exception.ProductNotFoundException;
import com.example.product_service.Models.Category;
import com.example.product_service.Models.Product;
import com.example.product_service.Repository.ProductRepository;
import com.example.product_service.Service.KafkaProducerService;
import com.example.product_service.Service.ProductService;
import org.example.Events.StockMovementEvent;
import org.example.Events.StockMovementType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HasStockChangeStockTests {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private ProductService productService;


    // =========================================================
    // HAS STOCK
    // =========================================================

    @Test
    void shouldReturnTrueWhenProductHasEnoughStock() {

        Product product = new Product();
        product.setId(1L);
        product.setStock(10);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        boolean result = productService.hasStock(1L, 5);

        assertTrue(result);

        verify(productRepository).findById(1L);
    }

    @Test
    void shouldReturnFalseWhenProductDoesNotHaveEnoughStock() {

        Product product = new Product();
        product.setId(1L);
        product.setStock(10);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        boolean result = productService.hasStock(1L, 20);

        assertFalse(result);

        verify(productRepository).findById(1L);
    }

    @Test
    void shouldReturnFalseWhenRequestedQuantityIsZero() {

        Product product = new Product();
        product.setId(1L);
        product.setStock(10);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        boolean result = productService.hasStock(1L, 0);

        assertFalse(result);
    }

    @Test
    void shouldReturnFalseWhenRequestedQuantityIsNegative() {

        Product product = new Product();
        product.setId(1L);
        product.setStock(10);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        boolean result = productService.hasStock(1L, -5);

        assertFalse(result);
    }

    @Test
    void shouldThrowExceptionWhenProductDoesNotExistForHasStock() {

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> productService.hasStock(999L, 5)
        );

        verify(productRepository).findById(999L);
    }


    // =========================================================
    // CHANGE STOCK - RESTOCK
    // =========================================================

    @Test
    void shouldIncreaseStockWhenRestocking() {

        Product product = createProduct(1L, 10);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        boolean result = productService.changeStock(
                1L,
                5,
                StockMovementType.RESTOCK
        );

        assertTrue(result);
        assertEquals(15, product.getStock());

        verify(productRepository).save(product);
        verify(kafkaProducerService).sendStockMovementEvent(any());
    }


    // =========================================================
    // CHANGE STOCK - RETURN
    // =========================================================

    @Test
    void shouldIncreaseStockWhenReturningProduct() {

        Product product = createProduct(1L, 10);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        boolean result = productService.changeStock(
                1L,
                3,
                StockMovementType.RETURN
        );

        assertTrue(result);
        assertEquals(13, product.getStock());

        verify(productRepository).save(product);
        verify(kafkaProducerService).sendStockMovementEvent(any());
    }


    // =========================================================
    // CHANGE STOCK - LOSS
    // =========================================================

    @Test
    void shouldDecreaseStockWhenRegisteringLoss() {

        Product product = createProduct(1L, 10);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        boolean result = productService.changeStock(
                1L,
                4,
                StockMovementType.LOSS
        );

        assertTrue(result);
        assertEquals(6, product.getStock());

        verify(productRepository).save(product);
        verify(kafkaProducerService).sendStockMovementEvent(any());
    }


    @Test
    void shouldRejectLossWhenThereIsNotEnoughStock() {

        Product product = createProduct(1L, 5);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.changeStock(
                        1L,
                        10,
                        StockMovementType.LOSS
                )
        );

        assertEquals(5, product.getStock());

        verify(productRepository, never()).save(any());
        verify(kafkaProducerService, never())
                .sendStockMovementEvent(any());
    }


    // =========================================================
    // CHANGE STOCK - VALIDACIONES
    // =========================================================


    @Test
    void shouldReturnFalseWhenQuantityIsZero() {

        boolean result = productService.changeStock(
                1L,
                0,
                StockMovementType.RESTOCK
        );

        assertFalse(result);

        verify(productRepository, never()).findById(any());
        verify(productRepository, never()).save(any());
        verify(kafkaProducerService, never())
                .sendStockMovementEvent(any());
    }


    @Test
    void shouldReturnFalseWhenQuantityIsNegative() {


        boolean result = productService.changeStock(
                1L,
                -5,
                StockMovementType.RESTOCK
        );

        assertFalse(result);

        verify(productRepository, never()).findById(any());
        verify(productRepository, never()).save(any());
        verify(kafkaProducerService, never())
                .sendStockMovementEvent(any());

    }


    @Test
    void shouldReturnFalseWhenMovementTypeIsNull() {


        boolean result = productService.changeStock(
                1L,
                5,
                null
        );

        assertFalse(result);

        verify(productRepository, never()).save(any());
        verify(kafkaProducerService, never())
                .sendStockMovementEvent(any());
    }


    @Test
    void shouldThrowExceptionWhenProductDoesNotExistForChangeStock() {

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> productService.changeStock(
                        999L,
                        5,
                        StockMovementType.RESTOCK
                )
        );

        verify(productRepository).findById(999L);

        verify(productRepository, never()).save(any());

        verify(kafkaProducerService, never())
                .sendStockMovementEvent(any());
    }


    // =========================================================
    // EVENTO KAFKA
    // =========================================================

    @Test
    void shouldPublishStockMovementEventAfterChangingStock() {

        Product product = createProduct(1L, 10);
        Category category = new Category();
        category.setDenomination("Hardware");
        product.setCategory(category);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        boolean result = productService.changeStock(
                1L,
                5,
                StockMovementType.RESTOCK
        );

        assertTrue(result);

        verify(kafkaProducerService)
                .sendStockMovementEvent(any(StockMovementEvent.class));
    }


    // =========================================================
    // HELPER
    // =========================================================

    private Product createProduct(Long id, Integer stock) {

        Product product = new Product();

        product.setId(id);
        product.setName("Producto de prueba");
        product.setStock(stock);

        Category category = new Category();
        category.setDenomination("Hardware");

        product.setCategory(category);

        return product;
    }
}
