package com.example.product_service.Models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pack {

    @Id
    @GeneratedValue
    private Long id;

    private String denomination;

    @NotNull(message = "El precio del pack no puede ser nulo")
    private BigDecimal price;

    @NotNull(message = "El precio de compra del pack no puede ser nulo")
    private boolean active = true;

    @OneToMany(mappedBy = "pack", cascade = CascadeType.ALL)
    private List<PackItem> items = new ArrayList<>();

}
