package com.example.product_service.Models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String cod;

    @NotBlank(message = "Ingresa un nombre para el producto")
    @Column(length = 100, nullable = false)
    private String name;

    @NotNull(message = "Ingresa una categoria para el producto")
    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Category category;

    @NotNull(message = "Ingresa un precio de venta")
    @Min(value = 0, message = "El precio no puede ser negativo")
    private BigDecimal priceSale;

    @NotNull(message = "Ingresa un precio de compra")
    @Min(value = 0, message = "El precio no puede ser negativo")
    private BigDecimal priceBuy;

    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    private Integer stockMin;


    @Column(length = 200)
    private String description;

    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock;


    @Min(value = 1, message = "Debe ser mayor a cero")
    private Integer unitsPerPack;

    private boolean active = true;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Product parent;

    private String image;

    @OneToMany(mappedBy = "parent")
    private List<Product> children = new ArrayList<>();
}
