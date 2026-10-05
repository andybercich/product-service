package com.example.product_service.Service;

import com.example.product_service.Exception.InsufficientStockException;
import com.example.product_service.Exception.InvalidStockOperationException;
import com.example.product_service.Exception.ProductNotFoundException;
import com.example.product_service.Models.Product;
import com.example.product_service.Repository.ProductRepository;
import com.example.product_service.Repository.Specification.ProductSpecification;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import org.example.Events.StockMovementEvent;
import org.example.Events.StockMovementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService implements BaseService<Product, Long> {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OutboxService outboxService;

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    @Override
    public Product findById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public Product save(Product entity) {
        Product product = productRepository.save(entity);
        log.info("Producto creado. productId={}, name={}", product.getId(), product.getName());
        return product;
    }

    @Override
    public void deleteById(Long id) {
        if (productRepository.existsById(id)) {
            Product product = productRepository.findById(id).orElse(null);
            product.setActive(false);
            productRepository.save(product);
            log.info("Producto con id {} desactivado", id);
        }else {
            log.error("Producto con id {} no encontrado para eliminar", id);
            throw new ProductNotFoundException(id);
        }
    }

    @Override
    public Product update(Long id, Product entity) {
        return productRepository.findById(id).map(product -> {
            product.setName(entity.getName());
            product.setDescription(entity.getDescription());
            product.setCategory(entity.getCategory());
            product.setChildren(entity.getChildren());
            product.setParent(entity.getParent());
            product.setPriceSale(entity.getPriceSale());
            product.setPriceBuy(entity.getPriceBuy());
            product.setStock(entity.getStock());
            product.setStockMin(entity.getStockMin());
            product.setActive(entity.isActive());
            product.setImage(entity.getImage());
            product.setCod(entity.getCod());
            log.info("Producto actualizado. productId={}", id);
            return productRepository.save(product);
        }).orElseThrow(() -> new ProductNotFoundException(id));
    }


    public Page<Product> search(
            String cod,
            String name,
            Long categoryId,
            BigDecimal priceSaleMin,
            BigDecimal priceSaleMax,
            Boolean active,
            Boolean onlyParent,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection
    ) {

        Specification<Product> spec =
                ProductSpecification.filter(
                        cod,
                        name,
                        categoryId,
                        priceSaleMin,
                        priceSaleMax,
                        active,
                        onlyParent
                );

        Pageable pageable = buildPageable(page, size, sortBy, sortDirection);

        return productRepository.findAll(spec, pageable);
    }

    private Pageable buildPageable(
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection
    ) {

        Sort sort;

        boolean desc = "desc".equalsIgnoreCase(sortDirection);

        if ("stock".equalsIgnoreCase(sortBy)) {
            sort = desc
                    ? Sort.by("stock").descending()
                    : Sort.by("stock").ascending();

        } else if ("price".equalsIgnoreCase(sortBy)) {
            sort = desc
                    ? Sort.by("priceSale").descending()
                    : Sort.by("priceSale").ascending();

        } else {
            sort = Sort.by("id").ascending();
        }

        return PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 10,
                sort
        );
    }

    private Product resolveBaseProduct(Product product) {
        return product.getParent() != null
                ? product.getParent()
                : product;
    }

    @Transactional
    public void discountStock(Long productId, int quantity) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        Product baseProduct = resolveBaseProduct(product);

        int unitsToDiscount = product.getParent() != null
                ? quantity * product.getUnitsPerPack()
                : quantity;

        if (quantity <= 0) {
            log.warn("Intento de descontar cantidad inválida. productId={}, quantity={}", productId, quantity);

            throw new InvalidStockOperationException("Cantidad a descontar debe ser mayor a 0");
        }

        if (baseProduct.getStock() < unitsToDiscount) {
            log.warn("Stock insuficiente. productId={}, stock={}, requested={}", baseProduct.getId(),
                    baseProduct.getStock(), unitsToDiscount);
            throw new InsufficientStockException("Stock insuficiente del producto base");
        }


        baseProduct.setStock(baseProduct.getStock() - unitsToDiscount);
        save(baseProduct);
    }

    @Transactional
    public void increaseStock(Long productId, int quantity) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        Product baseProduct = resolveBaseProduct(product);

        if (quantity <= 0) {
            log.error("Cantidad a aumentar debe ser mayor a 0. Cantidad recibida: {}", quantity);
            throw new InvalidStockOperationException("Cantidad a aumentar debe ser mayor a 0");
        }

        int unitsToIncrease = product.getParent() != null
                ? quantity * product.getUnitsPerPack()
                : quantity;

        baseProduct.setStock(baseProduct.getStock() + unitsToIncrease);
        save(baseProduct);
    }

    @Transactional
    public boolean hasStock(Long productId, int quantity) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        Product baseProduct = resolveBaseProduct(product);

        if (quantity <= 0) {
            log.warn("Cantidad solicitada debe ser mayor a 0. Cantidad recibida: {}", quantity);
            return false;
        }

        int requiredUnits = product.getParent() != null
                ? quantity * product.getUnitsPerPack()
                : quantity;

        return baseProduct.getStock() >= requiredUnits;
    }


    @Transactional
    public boolean changeStock(
            Long productId,
            int quantity,
            StockMovementType movementType
    ) {

        if (quantity <= 0 || movementType == null) {

            log.warn(
                    "Cantidad debe ser mayor a 0 y el tipo de movimiento no puede ser nulo. " +
                            "Cantidad: {}, Tipo de movimiento: {}",
                    quantity,
                    movementType
            );

            return false;
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        Integer stockBefore = product.getStock();

        if (movementType == StockMovementType.LOSS) {

            if (stockBefore < quantity) {
                throw new IllegalArgumentException(
                        "No hay stock suficiente para registrar la pérdida"
                );
            }

            product.setStock(stockBefore - quantity);

        } else if (
                movementType == StockMovementType.RESTOCK ||
                        movementType == StockMovementType.RETURN
        ) {

            product.setStock(stockBefore + quantity);
        }


        Integer stockAfter = product.getStock();
        productRepository.save(product);

        UUID eventId = UUID.randomUUID();


        StockMovementEvent event = new StockMovementEvent(
                eventId,
                productId,
                product.getName(),
                movementType,
                stockBefore,
                stockAfter,
                quantity,
                product.getCategory().getDenomination(),
                LocalDateTime.now()
        );



        try {

            outboxService.saveEvent(
                    event.getEventId(),
                    "STOCK_MOVEMENT",
                    "stock-movement-events",
                    event);


            log.info(
                    "Stock movement event saved to outbox. productId={}, eventId={}",
                    productId,
                    event.getEventId()
            );

            return true;

        } catch (JsonProcessingException exception) {

            log.error(
                    "Failed to save stock movement event to outbox. productId={}, eventId={}",
                    productId,
                    event.getEventId(),
                    exception
            );

            throw new RuntimeException(
                    "Failed to save stock movement event to outbox",
                    exception
            );
        }
    }

}