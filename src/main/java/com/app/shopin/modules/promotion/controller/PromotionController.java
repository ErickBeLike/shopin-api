package com.app.shopin.modules.promotion.controller;

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
