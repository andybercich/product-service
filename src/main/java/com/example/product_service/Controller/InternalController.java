package com.example.product_service.Controller;

import com.example.product_service.DTO.PackDTO;
import com.example.product_service.DTO.ProductDTO;
import com.example.product_service.DTO.StockDTO;
import com.example.product_service.Models.Pack;
import com.example.product_service.Models.Product;
import com.example.product_service.Service.PackService;
import com.example.product_service.Service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/stock")
public class InternalController {

    @Autowired
    private ProductService productService;

    @Autowired
    private PackService packService;


    @PutMapping("/product/discount")
    public ResponseEntity<?> discountProductStock(@RequestBody StockDTO stockDTO) {
            productService.discountStock(stockDTO.getItemId(), stockDTO.getQuantity());
            return ResponseEntity.ok().build();

    }

    @PutMapping("/product/increase")
    public ResponseEntity<?> increaseProductStock(@RequestBody StockDTO stockDTO) {

            productService.increaseStock(stockDTO.getItemId(), stockDTO.getQuantity());
            return ResponseEntity.ok().build();

    }


    @PutMapping("/pack/discount")
    public ResponseEntity<?> discountPackStock (@RequestBody StockDTO stockDTO){
            packService.discountStock(stockDTO.getItemId(), stockDTO.getQuantity());
            return ResponseEntity.ok().build();

    }

    @PutMapping("/pack/increase")
    public ResponseEntity<?> increasePackStock (@RequestBody StockDTO stockDTO){

            packService.increaseStock(stockDTO.getItemId(), stockDTO.getQuantity());
            return ResponseEntity.ok().build();
    }

    @GetMapping("/packs/{id}")
    public ResponseEntity<PackDTO> findByIdPack(@PathVariable Long id) {
        Pack pack = packService.findById(id);
        return ResponseEntity.ok(PackDTO.fromEntity(pack));
    }


    @GetMapping("/products/{id}")
    public ResponseEntity<ProductDTO> findByIdProduct(@PathVariable Long id) {
        Product product = productService.findById(id);
        return ResponseEntity.ok(ProductDTO.fromEntity(product));
    }



}
