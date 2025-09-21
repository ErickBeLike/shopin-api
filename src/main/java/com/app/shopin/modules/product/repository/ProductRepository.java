package com.app.shopin.modules.product.repository;

import com.app.shopin.modules.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByNameContainingIgnoreCase(String name);

    boolean existsBySku(String sku);

    @Query(value = "SELECT * FROM products", nativeQuery = true)
    Page<Product> findAllWithDeleted(Pageable pageable);

    // Busca un producto por ID, incluyendo si está borrado lógicamente
    @Query(value = "SELECT * FROM products WHERE id = ?1", nativeQuery = true)
    Optional<Product> findWithDeletedById(Long id);

    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    @Query("SELECT p FROM Product p WHERE p.category.id IN (SELECT c.id FROM Category c WHERE c.id = :categoryId OR c.parent.id = :categoryId)")
    List<Product> findByCategoryIdWithSubcategories(@Param("categoryId") Long categoryId);
}
