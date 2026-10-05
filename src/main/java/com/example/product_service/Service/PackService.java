package com.example.product_service.Service;

import com.example.product_service.Models.Pack;
import com.example.product_service.Models.PackItem;
import com.example.product_service.Repository.PackRepository;
import com.example.product_service.Repository.Specification.PackSpecification;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PackService implements BaseService<Pack, Long> {


    @Autowired
    private PackRepository packRepository;

    @Autowired
    private ProductService productService;


    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    @Override
    public Pack findById(Long id) {
        return packRepository.findById(id).orElseThrow(() -> new RuntimeException("Pack no encontrado"));
    }

    @Override
    public List<Pack> findAll() {
        return packRepository.findAll();
    }

    @Override
    public Pack save(Pack entity) {
        Pack pack = packRepository.save(entity);
        log.info("Pack creado. packId={}, name={}", pack.getId(), pack.getDenomination());

        return pack;
    }

    @Override
    public Pack update(Long id, Pack entity) {
        return packRepository.findById(id).map(pack -> {
            pack.setDenomination(entity.getDenomination());
            pack.setActive(entity.isActive());
            pack.setPrice(entity.getPrice());
            pack.setItems(entity.getItems());
            log.info("Pack actualizado. packId={}, name={}", pack.getId(), pack.getDenomination());
            return packRepository.save(pack);
        }).orElseThrow(() -> new RuntimeException("Pack no encontrado"));
    }

    @Override
    public void deleteById(Long id) {

        if (packRepository.existsById(id)) {
            Pack pack = packRepository.findById(id).orElse(null);
            assert pack != null;
            pack.setActive(false);
            log.info("Pack con id {} desactivado", id);
        }else{
            log.error("Pack con id {} no encontrado para eliminar", id);
            throw new RuntimeException("Pack no encontrado");
        }
    }

    public Page<Pack> search(  String denomination,
                               Boolean active,
                               String productName,
                               String productCode,
                               String sortDirection,
                               Integer page, Integer size) {

        Specification<Pack> spec = PackSpecification.filter(denomination, active, productName, productCode);
        Pageable pagePack = getPageable(sortDirection, page, size);

        return packRepository.findAll(spec, pagePack);



    }

    private Pageable getPageable(String sortDirection,Integer page, Integer size) {
        Sort sort;
        if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
            sort = Sort.by("price").ascending();
        }else if (sortDirection != null && sortDirection.equalsIgnoreCase("desc")) {
            sort = Sort.by("price").descending();
        } else {
            sort = Sort.by("id").ascending();
        }

        return PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 10,
                sort
        );

    }

    @Transactional
    public void discountStock(Long packId, int packQuantity) {

        log.info("Descontando stock del pack. packId={}, cantidad={}",
                packId, packQuantity);

        Pack pack = packRepository.findById(packId).orElseThrow(() -> new IllegalArgumentException("Pack no encontrado"));

        if (packQuantity <= 0) {
            log.warn("Cantidad de packs inválida. packId={}, cantidad={}", packId, packQuantity);

            throw new IllegalArgumentException("La cantidad de packs debe ser mayor a 0");
        }

        for (PackItem item : pack.getItems()) {

            int totalQuantity = item.getQuantity() * packQuantity;

            log.debug("Descontando productos del pack. packId={}, productId={}, cantidad={}", packId,
                    item.getProduct().getId(), totalQuantity
            );

            productService.discountStock(item.getProduct().getId(), totalQuantity);
        }

        log.info("Stock del pack descontado correctamente. packId={}, cantidad={}", packId, packQuantity);
    }

    @Transactional
    public void increaseStock(Long packId, int packQuantity) {

        log.info("Aumentando stock del pack. packId={}, cantidad={}", packId, packQuantity);

        Pack pack = packRepository.findById(packId).orElseThrow(() ->
                new IllegalArgumentException("Pack no encontrado"));

        if (packQuantity <= 0) {
            log.warn("Cantidad de packs inválida. packId={}, cantidad={}", packId, packQuantity);

            throw new IllegalArgumentException("La cantidad de packs debe ser mayor a 0");
        }

        for (PackItem item : pack.getItems()) {

            int totalQuantity = item.getQuantity() * packQuantity;

            log.debug("Aumentando stock de producto por pack. packId={}, productId={}, cantidad={}", packId,
                    item.getProduct().getId(), totalQuantity);

            productService.increaseStock(item.getProduct().getId(), totalQuantity);
        }

        log.info("Stock del pack aumentado correctamente. packId={}, cantidad={}", packId, packQuantity);
    }

    @Transactional
    public boolean hasStock(Long packId, int packQuantity) {
        Pack pack = packRepository.findById(packId).orElseThrow(() ->
                new IllegalArgumentException("Pack no encontrado"));
        if (packQuantity <= 0) {
            return false;
        }

        for (PackItem item : pack.getItems()) {

            int totalQuantity = item.getQuantity() * packQuantity;

            boolean hasStock = productService.hasStock(
                    item.getProduct().getId(),
                    totalQuantity
            );

            if (!hasStock) {
                return false;
            }
        }

        return true;
    }


}
