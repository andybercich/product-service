package com.example.product_service.Repository.Specification;
import com.example.product_service.Models.Pack;
import org.springframework.data.jpa.domain.Specification;

public class PackSpecification {

    private PackSpecification() {}

    public static Specification<Pack> filter(
            String denomination,
            Boolean active,
            String productName,
            String productCode
    ) {
        return (root, query, cb) -> {

            var predicate = cb.conjunction();

            // Denominación con búsqueda de tokens (AND entre tokens)
            if (denomination != null && !denomination.trim().isEmpty()) {
                String[] tokens = denomination.toLowerCase().split("\\s+");
                for (String token : tokens) {
                    predicate = cb.and(predicate,
                            cb.like(cb.lower(root.get("denomination")),
                                    "%" + token + "%"));
                }
            }

            // Pack activo/inactivo
            if (active != null) {
                predicate = cb.and(predicate,
                        active
                                ? cb.isTrue(root.get("active"))
                                : cb.isFalse(root.get("active")));
            }

            // Filtros para productos asociados (nombre con tokens AND y código exacto)
            if ((productName != null && !productName.trim().isEmpty())
                    || productCode != null) {

                var itemJoin = root.join("items");
                var productJoin = itemJoin.join("product");

                // Busqueda por nombre de producto con tokens (AND entre tokens)
                if (productName != null && !productName.trim().isEmpty()) {
                    String[] tokens = productName.toLowerCase().split("\\s+");
                    for (String token : tokens) {
                        predicate = cb.and(predicate,
                                cb.like(cb.lower(productJoin.get("name")),
                                        "%" + token + "%"));
                    }
                }

                // Busqueda por Codigo de productos exacto
                if (productCode != null && !productCode.trim().isEmpty()) {
                    predicate = cb.and(predicate,
                            cb.equal(productJoin.get("cod"), productCode.trim()));
                }

                query.distinct(true);
            }

            return predicate;
        };
    }
}


