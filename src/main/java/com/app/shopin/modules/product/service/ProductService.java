package com.app.shopin.modules.product.service;

import com.app.shopin.modules.exception.CustomException;
import com.app.shopin.modules.product.dto.ProductDTO;
import com.app.shopin.modules.product.dto.UpdatePriceDTO;
import com.app.shopin.modules.product.dto.UpdateStockDTO;
import com.app.shopin.modules.product.entity.Category;
import com.app.shopin.modules.product.entity.Product;
import com.app.shopin.modules.product.repository.CategoryRepository;
import com.app.shopin.modules.product.repository.ProductRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    public record UpdateDiscountDTO(@Min(0) @Max(100) Integer discountPercent) {}

    @Transactional
    public ProductDTO createProduct(ProductDTO productDTO) {
        if (productRepository.existsBySku(productDTO.sku())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El SKU ya existe.");
        }
        Category category = categoryRepository.findById(productDTO.categoryId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Categoría no encontrada."));

        Product product = new Product();
        mapDtoToEntity(productDTO, product, category);

        Product savedProduct = productRepository.save(product);
        return mapEntityToDto(savedProduct);
    }

    @Transactional
    public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Producto no encontrado."));

        Category category = categoryRepository.findById(productDTO.categoryId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Categoría no encontrada."));

        mapDtoToEntity(productDTO, product, category);
        Product updatedProduct = productRepository.save(product);
        return mapEntityToDto(updatedProduct);
    }

    @Transactional
    public ProductDTO updateProductDiscount(Long productId, UpdateDiscountDTO discountDTO) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Producto no encontrado."));

        product.setDiscountPercent(discountDTO.discountPercent());

        productRepository.save(product);
        return mapEntityToDto(product);
    }

    @Transactional
    public ProductDTO removeProductDiscount(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Producto no encontrado."));

        // Simplemente ponemos el porcentaje en null (o 0).
        product.setDiscountPercent(null);

        productRepository.save(product);
        return mapEntityToDto(product);
    }

    @Transactional
    public ProductDTO updateStock(Long productId, UpdateStockDTO stockDTO) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Producto no encontrado."));

        product.setStockQuantity(stockDTO.newStock());
        productRepository.save(product);
        return mapEntityToDto(product);
    }

    @Transactional
    public ProductDTO updatePrice(Long productId, UpdatePriceDTO priceDTO) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Producto no encontrado."));

        product.setPrice(priceDTO.newPrice());
        productRepository.save(product);
        return mapEntityToDto(product);
    }

    @Transactional
    public void softDeleteProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Producto no encontrado.");
        }
        productRepository.deleteById(productId);
    }

    // SUPER-ADMIN/DEVELOPER SECTION
    @Transactional
    public ProductDTO reactivateProduct(Long productId) {
        Product product = productRepository.findWithDeletedById(productId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Producto no encontrado."));

        if (product.getDeletedAt() == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El producto ya está activo.");
        }

        product.setDeletedAt(null);
        Product savedProduct = productRepository.save(product);
        return mapEntityToDto(savedProduct);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> getAllProductsIncludingDeleted(Pageable pageable) {
        return productRepository.findAllWithDeleted(pageable).map(this::mapEntityToDto);
    }

    // SEARCHING SECTION
    @Transactional(readOnly = true)
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::mapEntityToDto);
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long productId) {
        return productRepository.findById(productId)
                .map(this::mapEntityToDto)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> searchProductsByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name).stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId).stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsBySubcategory(Long categoryId) {
        // Este método ahora buscará en la categoría principal Y en sus hijas directas.
        return productRepository.findByCategoryIdWithSubcategories(categoryId).stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return productRepository.findByPriceBetween(minPrice, maxPrice).stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsOnSale() {
        // Ahora busca por productos que tengan un descuento mayor a 0.
        return productRepository.findByDiscountPercentGreaterThan(0).stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByMinimumDiscount(Integer minDiscount) {
        // Busca productos con un descuento igual o mayor al que pide el usuario.
        return productRepository.findByDiscountPercentGreaterThanEqual(minDiscount).stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    private ProductDTO mapEntityToDto(Product product) {
        return new ProductDTO(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getDiscountPercent(),
                product.getEffectivePrice(),
                product.getStockQuantity(),
                product.getCategory().getId()
        );
    }

    private void mapDtoToEntity(ProductDTO dto, Product entity, Category category) {
        entity.setSku(dto.sku());
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setPrice(dto.price());
        entity.setDiscountPercent(dto.discountPercent());
        entity.setStockQuantity(dto.stockQuantity());
        entity.setCategory(category);
    }

}
