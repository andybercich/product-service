package com.example.product_service.Models;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @NotNull(message = "Ingresa una denominacion para la categoria")
    @NotBlank(message = "Ingresa una denominacion para la categoria")
    private String denomination;

    @NotNull(message = "El estado de la categoria no puede ser nulo")
    private boolean active = true;


}

