package com.example.product_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.Events.StockMovementType;

@Data
@AllArgsConstructor
public class StockMovementRequest {

    private int quantity;
    private StockMovementType movementType;
}