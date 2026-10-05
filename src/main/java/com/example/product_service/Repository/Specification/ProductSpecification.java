package com.example.product_service.Repository.Specification;

import com.example.product_service.Models.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class ProductSpecification {

    private ProductSpecification() {}

    public static Specification<Product> filter(
            String cod,
            String name,
            Long categoryId,
            BigDecimal priceSaleMin,
            BigDecimal priceSaleMax,
            Boolean active,
            Boolean onlyParent
    ) {
        return (root, query, cb) -> {

            var predicate = cb.conjunction();

            // Filtro para código exacto
            if (cod != null && !cod.trim().isEmpty()) {
                predicate = cb.and(predicate,
                        cb.equal(
                                cb.lower(root.get("cod")),
                                cod.trim().toLowerCase()
                        ));
            }

            // Filtro para nombre con búsqueda de tokens (AND entre tokens)
            if (name != null && !name.trim().isEmpty()) {
                String[] tokens = name.toLowerCase().split("\\s+");

                for (String token : tokens) {
                    predicate = cb.and(predicate,
                            cb.like(cb.lower(root.get("name")),
                                    "%" + token + "%"));
                }
            }


            // Filtro para categoría exacta
            if (categoryId != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("category").get("id"), categoryId));
            }

            // Filtros para rango de precio de venta
            if (priceSaleMin != null) {
                predicate = cb.and(predicate,
                        cb.ge(root.get("priceSale"), priceSaleMin));
            }

            if (priceSaleMax != null) {
                predicate = cb.and(predicate,
                        cb.le(root.get("priceSale"), priceSaleMax));
            }

            // Filtro para productos activos/inactivos
            if (active != null) {
                predicate = cb.and(predicate,
                        active
                                ? cb.isTrue(root.get("active"))
                                : cb.isFalse(root.get("active")));
            }


            // Filtro para obtener solo productos sin padre (productos principales)
            if (onlyParent != null && onlyParent) {
                predicate = cb.and(predicate,
                        cb.isNull(root.get("parent")));
            }

            return predicate;
        };
    }
}
