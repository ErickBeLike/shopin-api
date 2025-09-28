package com.app.shopin.modules.promotion.repository;

import com.app.shopin.modules.promotion.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    @Query("SELECT p FROM Promotion p WHERE p.isActive = true AND (p.startDate IS NULL OR p.startDate <= :now) AND (p.endDate IS NULL OR p.endDate >= :now)")
    List<Promotion> findAllActivePromotions(@Param("now") LocalDateTime now);

    @Query(value = "SELECT * FROM promotions", nativeQuery = true)
    Page<Promotion> findAllWithDeleted(Pageable pageable);

    @Query(value = "SELECT * FROM promotions WHERE id = ?1", nativeQuery = true)
    Optional<Promotion> findWithDeletedById(Long id);
}
