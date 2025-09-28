package com.app.shopin.modules.promotion.controller;

import com.app.shopin.modules.promotion.dto.ManageCategoryInPromotionDTO;
import com.app.shopin.modules.promotion.dto.ManageProductInPromotionDTO;
import com.app.shopin.modules.promotion.dto.PromotionDTO;
import com.app.shopin.modules.promotion.dto.UpdatePromotionStatusDTO;
import com.app.shopin.modules.promotion.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
// Protegemos toda la clase. Solo usuarios con este permiso pueden gestionar promociones.
public class PromotionController {

    @Autowired
    private PromotionService promotionService;

    @GetMapping("/active")
    public ResponseEntity<List<PromotionDTO>> getActivePromotions() {
        return ResponseEntity.ok(promotionService.getActivePromotions());
    }

    @GetMapping
    public ResponseEntity<Page<PromotionDTO>> getAllPromotions(Pageable pageable) {
        return ResponseEntity.ok(promotionService.getAllPromotions(pageable));
    }

    @GetMapping("/{promotionId}")
    public ResponseEntity<PromotionDTO> getPromotionById(@PathVariable Long promotionId) {
        return ResponseEntity.ok(promotionService.getPromotionById(promotionId));
    }

    @PostMapping
    public ResponseEntity<PromotionDTO> createPromotion(@Valid @RequestBody PromotionDTO promotionDTO) {
        PromotionDTO createdPromotion = promotionService.createPromotion(promotionDTO);
        return new ResponseEntity<>(createdPromotion, HttpStatus.CREATED);
    }

    @PutMapping("/{promotionId}")
    public ResponseEntity<PromotionDTO> updatePromotion(
            @PathVariable Long promotionId,
            @Valid @RequestBody PromotionDTO promotionDTO) {
        PromotionDTO updatedPromotion = promotionService.updatePromotion(promotionId, promotionDTO);
        return ResponseEntity.ok(updatedPromotion);
    }

    @PatchMapping("/{promotionId}/status")
    public ResponseEntity<PromotionDTO> updateStatus(@PathVariable Long promotionId, @RequestBody UpdatePromotionStatusDTO dto) {
        return ResponseEntity.ok(promotionService.updatePromotionStatus(promotionId, dto));
    }

    // PRODUCT PROMOTION SECTION
    @PostMapping("/{promotionId}/products")
    public ResponseEntity<PromotionDTO> addProductToPromotion(
            @PathVariable Long promotionId,
            @RequestBody ManageProductInPromotionDTO dto) {
        return ResponseEntity.ok(promotionService.addProductToPromotion(promotionId, dto.productId()));
    }

    @DeleteMapping("/{promotionId}/products/{productId}")
    public ResponseEntity<PromotionDTO> removeProductFromPromotion(
            @PathVariable Long promotionId,
            @PathVariable Long productId) {
        return ResponseEntity.ok(promotionService.removeProductFromPromotion(promotionId, productId));
    }

    // CATEGORY PROMOTION SECTION
    @PostMapping("/{promotionId}/categories")
    public ResponseEntity<PromotionDTO> addCategoryToPromotion(
            @PathVariable Long promotionId,
            @RequestBody ManageCategoryInPromotionDTO dto) {
        return ResponseEntity.ok(promotionService.addCategoryToPromotion(promotionId, dto.categoryId()));
    }

    @DeleteMapping("/{promotionId}/categories/{categoryId}")
    public ResponseEntity<PromotionDTO> removeCategoryFromPromotion(
            @PathVariable Long promotionId,
            @PathVariable Long categoryId) {
        return ResponseEntity.ok(promotionService.removeCategoryFromPromotion(promotionId, categoryId));
    }

    // GRANULAR CLEAN SECTION
    @DeleteMapping("/{promotionId}/products")
    public ResponseEntity<PromotionDTO> clearProducts(@PathVariable Long promotionId) {
        return ResponseEntity.ok(promotionService.clearProductsFromPromotion(promotionId));
    }

    @DeleteMapping("/{promotionId}/categories")
    public ResponseEntity<PromotionDTO> clearCategories(@PathVariable Long promotionId) {
        return ResponseEntity.ok(promotionService.clearCategoriesFromPromotion(promotionId));
    }

    // GENERAL CLEAN SECTION
    @DeleteMapping("/{promotionId}/associations")
    public ResponseEntity<PromotionDTO> clearAssociations(@PathVariable Long promotionId) {
        return ResponseEntity.ok(promotionService.clearAllAssociations(promotionId));
    }

    @DeleteMapping("/{promotionId}")
    public ResponseEntity<Void> deletePromotion(@PathVariable Long promotionId) {
        promotionService.deletePromotion(promotionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{promotionId}/reactivate")
    public ResponseEntity<PromotionDTO> reactivatePromotion(@PathVariable Long promotionId) {
        PromotionDTO reactivatedPromotion = promotionService.reactivatePromotion(promotionId);
        return ResponseEntity.ok(reactivatedPromotion);
    }
}
