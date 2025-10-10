package com.app.shopin.modules.favorites.repository;

import com.app.shopin.modules.favorites.entity.FavoriteList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteListRepository extends JpaRepository<FavoriteList, Long> {

    List<FavoriteList> findByUserId(Long userId);

    Optional<FavoriteList> findByUserIdAndNameIgnoreCase(Long userId, String name);
}