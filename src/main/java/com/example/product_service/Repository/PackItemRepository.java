package com.example.product_service.Repository;

import com.example.product_service.Models.PackItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface PackItemRepository extends JpaRepository<PackItem, Long> {
}
