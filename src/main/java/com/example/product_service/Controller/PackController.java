package com.example.product_service.Controller;


import com.example.product_service.DTO.PackDTO;
import com.example.product_service.DTO.StockDTO;
import com.example.product_service.Models.Pack;
import com.example.product_service.Service.PackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/packs")
public class PackController {

    @Autowired
    private PackService packService;

    @GetMapping("/{id}")
    public ResponseEntity<PackDTO> findById(@PathVariable Long id) {
        Pack pack = packService.findById(id);
        return ResponseEntity.ok(PackDTO.fromEntity(pack));
    }

    @GetMapping("/search")
    public Page<Pack> search(
            @RequestParam(required = false) String denomination,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String productCode,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return packService.search(denomination, active, productName, productCode, sortDirection, page, size);
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody Pack pack) {
        return ResponseEntity.ok(packService.save(pack));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Pack pack) {
        Pack updatedPack = packService.update(id, pack);
        return updatedPack != null ? ResponseEntity.ok(updatedPack) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteById(@PathVariable Long id) {
        packService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/hasStock")
    public ResponseEntity<?> hasStock(@RequestBody StockDTO stockDTO) {
        boolean hasStock = packService.hasStock(stockDTO.getItemId(), stockDTO.getQuantity());
        return ResponseEntity.ok(hasStock);
    }
}