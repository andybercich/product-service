package com.example.product_service.Service;

import com.example.product_service.Models.PackItem;
import com.example.product_service.Repository.PackItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PackItemService implements BaseService<PackItem, Long> {

    @Autowired
    private PackItemRepository packItemRepository;

    @Override
    public PackItem findById(Long aLong) {
        return packItemRepository.findById(aLong).orElse(null);
    }

    @Override
    public List<PackItem> findAll() {
        return packItemRepository.findAll();
    }

    @Override
    public PackItem save(PackItem entity) {
        return packItemRepository.save(entity);
    }

    @Override
    public PackItem update(Long aLong, PackItem entity) {
        return packItemRepository.findById(aLong).map(packItem -> {
            packItem.setPack(entity.getPack());
            packItem.setProduct(entity.getProduct());
            packItem.setQuantity(entity.getQuantity());
            return packItemRepository.save(packItem);
        }).orElse(null);
    }

    @Override
    public void deleteById(Long aLong) {
        if (packItemRepository.existsById(aLong)) {
            packItemRepository.deleteById(aLong);
        }
    }
}
