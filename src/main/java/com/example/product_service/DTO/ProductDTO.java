package com.example.product_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal priceSale;
    private BigDecimal priceBuy;
    public static ProductDTO fromEntity(com.example.product_service.Models.Product product) {
        return new ProductDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPriceSale(),
                product.getPriceBuy()
        );
    }
}
