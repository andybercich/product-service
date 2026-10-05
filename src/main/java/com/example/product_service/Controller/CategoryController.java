package com.example.product_service.Controller;

import com.example.product_service.Models.Category;
import com.example.product_service.Service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/categories")
public class CategoryController {

    @Autowired
    private CategoryService caregoryService;

    @GetMapping
    public ResponseEntity<?> findAll() {
        return caregoryService.findAll().isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(caregoryService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@RequestParam Long id) {
        Category category = caregoryService.findById(id);
        return category == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(category);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@RequestParam Long id, @RequestBody Category category) {
        Category updatedCategory = caregoryService.update(id, category);
        return updatedCategory == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updatedCategory);
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody Category category) {
        return ResponseEntity.ok(caregoryService.save(category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteById(@RequestParam Long id) {
        caregoryService.deleteById(id);
        return ResponseEntity.ok().build();
    }
}