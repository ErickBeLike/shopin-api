package com.app.shopin.modules.cart.controller;

import com.app.shopin.modules.cart.dto.AddOrUpdateCartItemDTO;
import com.app.shopin.modules.cart.dto.CartDTO;
import com.app.shopin.modules.cart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("isAuthenticated()")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping("/me")
    public ResponseEntity<CartDTO> getMyCart(@AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(cartService.getCartDTOForUser(currentUser));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDTO> addOrUpdateItem(
            @Valid @RequestBody AddOrUpdateCartItemDTO itemDTO,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(cartService.addOrUpdateItem(itemDTO, currentUser));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartDTO> removeItem(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(cartService.removeItem(productId, currentUser));
    }

    @DeleteMapping
    public ResponseEntity<CartDTO> clearCart(@AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(cartService.clearCart(currentUser));
    }

    @PostMapping("/items/{productId}/save-for-later")
    public ResponseEntity<Void> saveItemForLater(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails currentUser) {
        cartService.saveItemForLater(productId, currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CartDTO>> getAllCarts() {
        return ResponseEntity.ok(cartService.getAllCarts());
    }
}