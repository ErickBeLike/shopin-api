package com.app.shopin.modules.promotion.service;

import com.app.shopin.modules.exception.CustomException;
import com.app.shopin.modules.product.entity.Category;
import com.app.shopin.modules.product.entity.Product;
import com.app.shopin.modules.product.repository.CategoryRepository;
import com.app.shopin.modules.product.repository.ProductRepository;
import com.app.shopin.modules.promotion.dto.PromotionDTO;
import com.app.shopin.modules.promotion.dto.UpdatePromotionStatusDTO;
import com.app.shopin.modules.promotion.entity.Promotion;
import com.app.shopin.modules.promotion.repository.PromotionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PromotionService {

    @Autowired
    private PromotionRepository promotionRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;

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
    public PromotionDTO updatePromotionStatus(Long promotionId, UpdatePromotionStatusDTO dto) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Promoción no encontrada."));

        promotion.setActive(dto.isActive());
        promotionRepository.save(promotion);
        return mapEntityToDto(promotion);
    }

    // PRODUCT PROMOTION SECTION
    @Transactional
    public PromotionDTO addProductToPromotion(Long promotionId, Long productId) {
        Promotion promotion = promotionRepository.findById(promotionId).orElseThrow(/* ... */);
        Product product = productRepository.findById(productId).orElseThrow(/* ... */);
        promotion.getProducts().add(product);
        promotionRepository.save(promotion);
        return mapEntityToDto(promotion);
    }

    @Transactional
    public PromotionDTO removeProductFromPromotion(Long promotionId, Long productId) {
        Promotion promotion = promotionRepository.findById(promotionId).orElseThrow(/* ... */);
        Product product = productRepository.findById(productId).orElseThrow(/* ... */);
        promotion.getProducts().remove(product);
        promotionRepository.save(promotion);
        return mapEntityToDto(promotion);
    }

    // CATEGORY PROMOTION SECTION
    @Transactional
    public PromotionDTO addCategoryToPromotion(Long promotionId, Long categoryId) {
        Promotion promotion = promotionRepository.findById(promotionId).orElseThrow(/* ... */);
        Category category = categoryRepository.findById(categoryId).orElseThrow(/* ... */);

        // TU REGLA DE NEGOCIO: Evitar añadir una subcategoría si el padre ya está.
        if (isAncestorInCategorySet(category, promotion.getCategories())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La categoría padre de esta subcategoría ya está incluida en la promoción.");
        }

        promotion.getCategories().add(category);
        promotionRepository.save(promotion);
        return mapEntityToDto(promotion);
    }

    @Transactional
    public PromotionDTO removeCategoryFromPromotion(Long promotionId, Long categoryId) {
        Promotion promotion = promotionRepository.findById(promotionId).orElseThrow(/* ... */);
        Category category = categoryRepository.findById(categoryId).orElseThrow(/* ... */);
        promotion.getCategories().remove(category);
        promotionRepository.save(promotion);
        return mapEntityToDto(promotion);
    }

    // GRANULAR CLEAN SECTION
    @Transactional
    public PromotionDTO clearProductsFromPromotion(Long promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Promoción no encontrada."));
        promotion.getProducts().clear();
        promotionRepository.save(promotion);
        return mapEntityToDto(promotion);
    }

    @Transactional
    public PromotionDTO clearCategoriesFromPromotion(Long promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Promoción no encontrada."));
        promotion.getCategories().clear();
        promotionRepository.save(promotion);
        return mapEntityToDto(promotion);
    }

    // GENERAL CLEAN SECTION
    @Transactional
    public PromotionDTO clearAllAssociations(Long promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Promoción no encontrada."));

        promotion.getProducts().clear();
        promotion.getCategories().clear();

        promotionRepository.save(promotion);
        return mapEntityToDto(promotion);
    }

    @Transactional
    public void deletePromotion(Long promotionId) {
        if (!promotionRepository.existsById(promotionId)) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Promoción no encontrada.");
        }
        promotionRepository.deleteById(promotionId);
    }

    @Transactional
    public PromotionDTO reactivatePromotion(Long promotionId) {
        Promotion promotion = promotionRepository.findWithDeletedById(promotionId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Promoción no encontrada."));

        if (promotion.getDeletedAt() == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La promoción ya está activa.");
        }

        promotion.setDeletedAt(null);
        Promotion reactivatedPromotion = promotionRepository.save(promotion);

        return mapEntityToDto(reactivatedPromotion);
    }

    @Transactional(readOnly = true)
    public List<PromotionDTO> getActivePromotions() {
        return promotionRepository.findAllActivePromotions(LocalDateTime.now()).stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
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

    private boolean isAncestorInCategorySet(Category category, Set<Category> categorySet) {
        Category parent = category.getParent();
        while (parent != null) {
            if (categorySet.contains(parent)) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    // --- MÉTODOS DE AYUDA (Mappers) ---

    private void mapDtoToEntity(PromotionDTO dto, Promotion entity) {
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setDiscountPercent(dto.discountPercent());
        entity.setStartDate(dto.startDate());
        entity.setEndDate(dto.endDate());
        entity.setActive(dto.isActive());

        // 1. Lógica para productos individuales
        if (dto.productIds() != null && !dto.productIds().isEmpty()) {
            List<Product> products = productRepository.findAllById(dto.productIds());
            entity.setProducts(new HashSet<>(products));
        } else {
            entity.getProducts().clear();
        }

        // 2. NUEVA LÓGICA para categorías completas
        if (dto.categoryIds() != null && !dto.categoryIds().isEmpty()) {
            List<Category> categories = categoryRepository.findAllById(dto.categoryIds());
            entity.setCategories(new HashSet<>(categories));
        } else {
            entity.getCategories().clear();
        }
    }

    private PromotionDTO mapEntityToDto(Promotion entity) {
        // Al devolver, convertimos la colección de Productos a una simple lista de IDs
        List<Long> productIds = entity.getProducts().stream()
                .map(Product::getId)
                .collect(Collectors.toList());
        List<Long> categoryIds = entity.getCategories().stream()
                .map(Category::getId)
                .toList();


        return new PromotionDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getDiscountPercent(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.isActive(),
                productIds,
                categoryIds
        );
    }
}
