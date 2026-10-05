package com.example.product_service.Controller;

import com.example.product_service.DTO.PageResponse;
import com.example.product_service.DTO.ProductDTO;
import com.example.product_service.DTO.StockDTO;
import com.example.product_service.Models.Product;
import com.example.product_service.Service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
@RestController
@RequestMapping("/products")
public class ProductController extends BaseController{

    @Autowired
    private ProductService productService;

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> findById(@PathVariable Long id) {
        Product product = productService.findById(id);
        return ResponseEntity.ok(ProductDTO.fromEntity(product));
    }

    @GetMapping("/search")
    public PageResponse<ProductDTO> search(
            @RequestParam(required = false) String cod,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal priceSaleMin,
            @RequestParam(required = false) BigDecimal priceSaleMax,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean onlyParent,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection
    ) {

        Page<Product> result = productService.search(
                cod,
                name,
                categoryId,
                priceSaleMin,
                priceSaleMax,
                active,
                onlyParent,
                page,
                size,
                sortBy,
                sortDirection
        );

        Page<ProductDTO> dtoPage = result.map(ProductDTO::fromEntity);

        return PageResponse.from(dtoPage);

    }

    @PostMapping
    public ResponseEntity<Product> save(@RequestBody Product product) {
        Product savedProduct = productService.save(product);
        return ResponseEntity.ok(savedProduct);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @RequestBody Product product) {
        Product updatedProduct = productService.update(id, product);
        return updatedProduct != null ? ResponseEntity.ok(updatedProduct) : ResponseEntity.notFound().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteById(@RequestParam Long id) {
        productService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/hasStock")
    public ResponseEntity<Boolean> hasStock(@RequestBody StockDTO stockDTO) {
        boolean hasStock = productService.hasStock(stockDTO.getItemId(), stockDTO.getQuantity());
        return ResponseEntity.ok(hasStock);
    }

    @PostMapping("/{id}/stock/movement")
    public ResponseEntity<Boolean> changeStock(
        @PathVariable Long id,
        @RequestBody com.example.product_service.DTO.StockMovementRequest request) {

    boolean changed = productService.changeStock(
            id,
            request.getQuantity(),
            request.getMovementType()
    );

    return ResponseEntity.ok(changed);
}


}