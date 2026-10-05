package com.example.product_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
public class StockDTO {
    private Long itemId;
    private int quantity;
}
