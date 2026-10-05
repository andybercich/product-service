package com.example.product_service;

import com.example.product_service.Models.Product;
import com.example.product_service.Service.ProductService;
import org.junit.jupiter.api.Test;

import com.example.product_service.Exception.InsufficientStockException;
import com.example.product_service.Exception.InvalidStockOperationException;
import com.example.product_service.Exception.ProductNotFoundException;
import com.example.product_service.Repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private ProductService productService;

	private Product product;

	@BeforeEach
	void setUp() {

		product = new Product();

		product.setId(1L);
		product.setName("Mouse Logitech");
		product.setCod("MOUSE-001");
		product.setStock(10);
		product.setActive(true);
	}

	@Test
	void shouldFindProductById() {

		when(productRepository.findById(1L))
				.thenReturn(Optional.of(product));

		Product result = productService.findById(1L);

		assertNotNull(result);
		assertEquals(1L, result.getId());
		assertEquals("Mouse Logitech", result.getName());

		verify(productRepository).findById(1L);
	}

	@Test
	void shouldThrowExceptionWhenProductDoesNotExist() {

		when(productRepository.findById(999L))
				.thenReturn(Optional.empty());

		assertThrows(
				ProductNotFoundException.class,
				() -> productService.findById(999L)
		);

		verify(productRepository).findById(999L);
	}

	@Test
	void shouldDiscountStock() {

		when(productRepository.findById(1L))
				.thenReturn(Optional.of(product));

		productService.discountStock(1L, 3);

		assertEquals(7, product.getStock());

		verify(productRepository).save(product);
	}

	@Test
	void shouldIncreaseStock() {

		when(productRepository.findById(1L))
				.thenReturn(Optional.of(product));

		productService.increaseStock(1L, 5);

		assertEquals(15, product.getStock());

		verify(productRepository).save(product);
	}

	@Test
	void shouldNotDiscountMoreStockThanAvailable() {

		when(productRepository.findById(1L))
				.thenReturn(Optional.of(product));

		assertThrows(
				InsufficientStockException.class,
				() -> productService.discountStock(1L, 20)
		);

		assertEquals(10, product.getStock());

		verify(productRepository, never())
				.save(any(Product.class));
	}

	@Test
	void shouldRejectZeroQuantityWhenDiscountingStock() {

		when(productRepository.findById(1L))
				.thenReturn(Optional.of(product));

		assertThrows(
				InvalidStockOperationException.class,
				() -> productService.discountStock(1L, 0)
		);

		assertEquals(10, product.getStock());

		verify(productRepository, never())
				.save(any(Product.class));
	}

	@Test
	void shouldRejectNegativeQuantityWhenDiscountingStock() {

		when(productRepository.findById(1L))
				.thenReturn(Optional.of(product));

		assertThrows(
				InvalidStockOperationException.class,
				() -> productService.discountStock(1L, -5)
		);

		assertEquals(10, product.getStock());

		verify(productRepository, never())
				.save(any(Product.class));
	}

	@Test
	void shouldRejectZeroQuantityWhenIncreasingStock() {

		when(productRepository.findById(1L))
				.thenReturn(Optional.of(product));

		assertThrows(
				InvalidStockOperationException.class,
				() -> productService.increaseStock(1L, 0)
		);

		assertEquals(10, product.getStock());

		verify(productRepository, never())
				.save(any(Product.class));
	}

	@Test
	void shouldRejectNegativeQuantityWhenIncreasingStock() {

		when(productRepository.findById(1L))
				.thenReturn(Optional.of(product));

		assertThrows(
				InvalidStockOperationException.class,
				() -> productService.increaseStock(1L, -5)
		);

		assertEquals(10, product.getStock());

		verify(productRepository, never())
				.save(any(Product.class));
	}

	@Test
	void shouldThrowExceptionWhenDiscountingNonExistingProduct() {

		when(productRepository.findById(999L))
				.thenReturn(Optional.empty());

		assertThrows(
				ProductNotFoundException.class,
				() -> productService.discountStock(999L, 5)
		);

		verify(productRepository, never())
				.save(any(Product.class));
	}

	@Test
	void shouldThrowExceptionWhenIncreasingNonExistingProduct() {

		when(productRepository.findById(999L))
				.thenReturn(Optional.empty());

		assertThrows(
				ProductNotFoundException.class,
				() -> productService.increaseStock(999L, 5)
		);

		verify(productRepository, never())
				.save(any(Product.class));
	}
}
