package com.example.product_service.Models;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class PackItem {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @NotNull(message = "El pack al que pertenece el item no puede ser nulo")
    private Pack pack;

    @ManyToOne
    @NotNull(message = "El producto del item no puede ser nulo")
    private Product product;

    @NotNull(message = "La cantidad del item no puede ser nula")
    private int quantity;
}

