package com.app.shopin.modules.product.controller;

import com.app.shopin.modules.product.dto.ProductDTO;
import com.app.shopin.modules.product.dto.RestockDTO;
import com.app.shopin.modules.product.dto.UpdatePriceDTO;
import com.app.shopin.modules.product.dto.UpdateStockDTO;
import com.app.shopin.modules.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    // --- ENDPOINTS PÚBLICOS (PARA CLIENTES Y VISITANTES) ---

    @GetMapping
    public ResponseEntity<Page<ProductDTO>> getAllProducts(Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProductById(productId));
    }

    @GetMapping("/on-sale")
    public ResponseEntity<List<ProductDTO>> getProductsOnSale() {
        return ResponseEntity.ok(productService.getProductsOnSale());
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductDTO>> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer minDiscount,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "false") boolean includeSubcategories) {

        // Esta lógica se puede expandir a un método de servicio único y más complejo
        if (name != null) {
            return ResponseEntity.ok(productService.searchProductsByName(name));
        }
        if (categoryId != null) {
            if (includeSubcategories) {
                // Si el frontend nos pide incluir subcategorías, llamamos al método "amplio".
                return ResponseEntity.ok(productService.getProductsByCategoryAndSubcategories(categoryId));
            } else {
                // Si no, llamamos al método "estricto".
                return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
            }
        }
        if (minDiscount != null) {
            return ResponseEntity.ok(productService.getProductsByMinimumDiscount(minDiscount));
        }
        if (minPrice != null && maxPrice != null) {
            return ResponseEntity.ok(productService.getProductsByPriceRange(minPrice, maxPrice));
        }

        return ResponseEntity.badRequest().build();
    }

    // --- ENDPOINTS DE GESTIÓN (PARA ROLES CON PERMISOS) ---

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDTO> createProduct(
            @RequestPart("product") @Valid ProductDTO productDTO,
            @RequestPart("images") List<MultipartFile> images,
            @RequestPart(value = "video", required = false) MultipartFile video) {

        ProductDTO newProduct = productService.createProduct(productDTO, images, video);
        return new ResponseEntity<>(newProduct, HttpStatus.CREATED);
    }

    @PutMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable Long productId,
            @RequestPart("product") @Valid ProductDTO productDTO,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "video", required = false) MultipartFile video) {

        ProductDTO updatedProduct = productService.updateProduct(productId, productDTO, images, video);
        return ResponseEntity.ok(updatedProduct);
    }

    @PatchMapping("/{productId}/stock")
    public ResponseEntity<ProductDTO> updateStock(@PathVariable Long productId, @RequestBody @Valid UpdateStockDTO stockDTO) {
        return ResponseEntity.ok(productService.updateStock(productId, stockDTO));
    }

    @PostMapping("/{productId}/restock")
    public ResponseEntity<ProductDTO> restockProduct(
            @PathVariable Long productId,
            @RequestBody @Valid RestockDTO restockDTO) {
        return ResponseEntity.ok(productService.restockProduct(productId, restockDTO));
    }

    @PatchMapping("/{productId}/price")
    public ResponseEntity<ProductDTO> updatePrice(@PathVariable Long productId, @RequestBody @Valid UpdatePriceDTO priceDTO) {
        return ResponseEntity.ok(productService.updatePrice(productId, priceDTO));
    }

    @PatchMapping("/{productId}/discount")
    public ResponseEntity<ProductDTO> updateDiscount(@PathVariable Long productId, @RequestBody @Valid ProductService.UpdateDiscountDTO discountDTO) {
        return ResponseEntity.ok(productService.updateProductDiscount(productId, discountDTO));
    }

    @DeleteMapping("/{productId}/discount")
    public ResponseEntity<ProductDTO> removeDiscount(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.removeProductDiscount(productId));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> softDeleteProduct(@PathVariable Long productId) {
        productService.softDeleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    // --- ENDPOINTS DE GESTIÓN MULTIMEDIA ---

    @PostMapping(value = "/{productId}/media", consumes = "multipart/form-data")
    public ResponseEntity<ProductDTO> addMedia(@PathVariable Long productId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(productService.addMediaToProduct(productId, file));
    }

    @DeleteMapping("/{productId}/media/{mediaId}")
    public ResponseEntity<Void> deleteMedia(@PathVariable Long productId, @PathVariable Long mediaId) {
        productService.deleteMediaFromProduct(productId, mediaId);
        return ResponseEntity.noContent().build();
    }

    // --- ENDPOINTS DE SUPER-ADMIN ---

    @GetMapping("/all-with-deleted")
    public ResponseEntity<Page<ProductDTO>> getAllProductsIncludingDeleted(Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProductsIncludingDeleted(pageable));
    }

    @PostMapping("/{productId}/reactivate")
    public ResponseEntity<ProductDTO> reactivateProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.reactivateProduct(productId));
    }
}
