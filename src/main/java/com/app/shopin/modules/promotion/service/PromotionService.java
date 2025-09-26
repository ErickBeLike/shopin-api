package com.app.shopin.modules.promotion.service;

import com.app.shopin.modules.exception.CustomException;
import com.app.shopin.modules.product.entity.Product;
import com.app.shopin.modules.product.repository.ProductRepository;
import com.app.shopin.modules.promotion.dto.PromotionDTO;
import com.app.shopin.modules.promotion.entity.Promotion;
import com.app.shopin.modules.promotion.repository.PromotionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromotionService {

    @Autowired
    private PromotionRepository promotionRepository;
    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public PromotionDTO createPromotion(PromotionDTO promotionDTO) {
        Promotion promotion = new Promotion();
        mapDtoToEntity(promotionDTO, promotion);

        Promotion savedPromotion = promotionRepository.save(promotion);
        return mapEntityToDto(savedPromotion);
    }

    @Transactional
    public PromotionDTO updatePromotion(Long promotionId, PromotionDTO promotionDTO) {
        Promotion existingPromotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Promoción no encontrada."));

        mapDtoToEntity(promotionDTO, existingPromotion);
        Promotion updatedPromotion = promotionRepository.save(existingPromotion);
        return mapEntityToDto(updatedPromotion);
    }

    @Transactional
    public void deletePromotion(Long promotionId) {
        if (!promotionRepository.existsById(promotionId)) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Promoción no encontrada.");
        }
        promotionRepository.deleteById(promotionId);
    }

    @Transactional(readOnly = true)
    public Page<PromotionDTO> getAllPromotions(Pageable pageable) {
        return promotionRepository.findAll(pageable).map(this::mapEntityToDto);
    }

    @Transactional(readOnly = true)
    public PromotionDTO getPromotionById(Long promotionId) {
        return promotionRepository.findById(promotionId)
                .map(this::mapEntityToDto)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Promoción no encontrada."));
    }

    // --- MÉTODOS DE AYUDA (Mappers) ---

    private void mapDtoToEntity(PromotionDTO dto, Promotion entity) {
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setDiscountPercent(dto.discountPercent());
        entity.setStartDate(dto.startDate());
        entity.setEndDate(dto.endDate());
        entity.setActive(dto.isActive());

        // Aquí está la lógica para vincular los productos
        if (dto.productIds() != null && !dto.productIds().isEmpty()) {
            List<Product> products = productRepository.findAllById(dto.productIds());
            entity.setProducts(new HashSet<>(products));
        } else {
            // Si la lista de IDs está vacía, se eliminan todas las asociaciones
            entity.getProducts().clear();
        }
    }

    private PromotionDTO mapEntityToDto(Promotion entity) {
        // Al devolver, convertimos la colección de Productos a una simple lista de IDs
        List<Long> productIds = entity.getProducts().stream()
                .map(Product::getId)
                .collect(Collectors.toList());

        return new PromotionDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getDiscountPercent(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.isActive(),
                productIds
        );
    }
}
