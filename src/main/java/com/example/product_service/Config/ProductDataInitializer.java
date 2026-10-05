package com.example.product_service.Config;
import com.example.product_service.Models.Category;
import com.example.product_service.Models.Product;
import com.example.product_service.Repository.CategoryRepository;
import com.example.product_service.Repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class ProductDataInitializer {

    @Bean
    CommandLineRunner initProducts(
            ProductRepository productRepository,
            CategoryRepository categoryRepository
    ) {

        return args -> {

            if (productRepository.count() > 0) {
                System.out.println("Productos ya existentes.");
                return;
            }

            // Categorías
            Category bebidas = new Category();
            bebidas.setDenomination("Bebidas");
            bebidas.setActive(true);

            Category comidas = new Category();
            comidas.setDenomination("Comidas");
            comidas.setActive(true);

            categoryRepository.saveAll(List.of(bebidas, comidas));

            // Productos
            Product coca = new Product(
                    null,
                    "1001",
                    "Coca Cola 1L",
                    bebidas,
                    new BigDecimal("1200"),
                    new BigDecimal("700"),
                    10,
                    "Bebida gaseosa",
                    50,
                    1,
                    true,
                    null,
                    null,
                    List.of()
            );

            Product pepsi = new Product(
                    null,
                    "1002",
                    "Pepsi 1L",
                    bebidas,
                    new BigDecimal("1100"),
                    new BigDecimal("650"),
                    5,
                    "Bebida gaseosa",
                    30,
                    1,
                    true,
                    null,
                    null,
                    List.of()
            );

            Product pizza = new Product(
                    null,
                    "2001",
                    "Pizza Muzarella",
                    comidas,
                    new BigDecimal("3500"),
                    new BigDecimal("2000"),
                    3,
                    "Pizza clásica",
                    20,
                    1,
                    true,
                    null,
                    null,
                    List.of()
            );

            productRepository.saveAll(List.of(coca, pepsi, pizza));

            System.out.println("Productos iniciales cargados.");
        };
    }
}

