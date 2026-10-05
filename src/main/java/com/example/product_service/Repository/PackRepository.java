package com.example.product_service.Repository;

import com.example.product_service.Models.Pack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

@Repository
public interface PackRepository extends JpaRepository<Pack, Long>, JpaSpecificationExecutor<Pack> {

    List<Pack> findByActiveTrue();
    Optional<Pack> findByIdAndActiveTrue(Long id);
}
