package com.example.product_service.DTO;

import com.example.product_service.Models.Pack;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
@AllArgsConstructor
public class PackDTO {

    private Long id;
    private String name;
    private BigDecimal price;
    private BigDecimal cost;
    private String description;

    public static PackDTO fromEntity(Pack pack) {

        BigDecimal cost = pack.getItems().stream()
                .map(item -> item.getProduct().getPriceBuy()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String description = pack.getItems().stream()
                .map(item -> item.getProduct().getName())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        return new PackDTO(
                pack.getId(),
                pack.getDenomination(),
                pack.getPrice(),
                cost,
                description
        );
    }
}