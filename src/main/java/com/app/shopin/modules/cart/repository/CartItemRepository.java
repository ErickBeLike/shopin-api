package com.app.shopin.modules.cart.repository;

import com.app.shopin.modules.cart.entity.Cart;
import com.app.shopin.modules.cart.entity.CartItem;
import com.app.shopin.modules.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
}
