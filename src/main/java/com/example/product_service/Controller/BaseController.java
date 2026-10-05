package com.example.product_service.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api-products")
public class BaseController {
    @GetMapping
    public String getProducts() {
        return "Lista de productos";
    }
}
