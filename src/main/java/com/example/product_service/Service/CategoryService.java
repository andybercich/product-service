package com.example.product_service.Service;

import com.example.product_service.Models.Category;
import com.example.product_service.Repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService implements BaseService<Category, Long> {

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElse(null);
    }

    @Override
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    public Category save(Category entity) {

        Category category = categoryRepository.save(entity);

        logger.info("Category created successfully. categoryId={}, denomination={}", category.getId(),
                category.getDenomination());

        return category;
    }

    @Override
    public Category update(Long id, Category entity) {

        return categoryRepository.findById(id)
                .map(category -> {
                    category.setDenomination(entity.getDenomination());
                    Category updated = categoryRepository.save(category);
                    logger.info("Category updated successfully. categoryId={}", id);
                    return updated;
                })
                .orElse(null);
    }

    @Override
    public void deleteById(Long id) {

        if (categoryRepository.existsById(id)) {

            Category category = categoryRepository.findById(id).orElse(null);

            category.setActive(false);

            categoryRepository.save(category);

            logger.info("Category deactivated successfully. categoryId={}", id);

        } else {
            logger.warn("Attempt to deactivate non-existent category. categoryId={}", id);
        }
    }
}