package com.app.shopin.modules.favorites.controller;

import com.app.shopin.modules.favorites.dto.AssignProductToListsDTO;
import com.app.shopin.modules.favorites.dto.CreateFavoriteListDTO;
import com.app.shopin.modules.favorites.dto.FavoriteListDTO;
import com.app.shopin.modules.favorites.dto.UpdateFavoriteListDTO;
import com.app.shopin.modules.favorites.service.FavoriteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@PreAuthorize("isAuthenticated()")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    // --- ENDPOINTS PARA GESTIONAR LAS LISTAS ---

    @PostMapping("/lists")
    public ResponseEntity<FavoriteListDTO> createList(
            @Valid @RequestBody CreateFavoriteListDTO dto,
            @AuthenticationPrincipal UserDetails currentUser) {
        FavoriteListDTO newList = favoriteService.createList(dto, currentUser);
        return new ResponseEntity<>(newList, HttpStatus.CREATED);
    }

    @GetMapping("/lists")
    public ResponseEntity<List<FavoriteListDTO>> getMyLists(@AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(favoriteService.getListsForUser(currentUser));
    }

    @GetMapping("/lists/{listId}")
    public ResponseEntity<FavoriteListDTO> getListById(
            @PathVariable Long listId,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(favoriteService.getListById(listId, currentUser));
    }

    @PutMapping("/lists/{listId}")
    public ResponseEntity<FavoriteListDTO> updateList(
            @PathVariable Long listId,
            @Valid @RequestBody UpdateFavoriteListDTO dto,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(favoriteService.updateListName(listId, dto, currentUser));
    }

    @DeleteMapping("/lists/{listId}")
    public ResponseEntity<Void> deleteList(
            @PathVariable Long listId,
            @AuthenticationPrincipal UserDetails currentUser) {
        favoriteService.deleteList(listId, currentUser);
        return ResponseEntity.noContent().build();
    }

    // --- ENDPOINT PARA GESTIONAR PRODUCTOS DENTRO DE LAS LISTAS ---

    @PutMapping("/products/assign")
    public ResponseEntity<Void> assignProductToLists(
            @RequestBody @Valid AssignProductToListsDTO dto,
            @AuthenticationPrincipal UserDetails currentUser) {
        favoriteService.assignProductToLists(dto, currentUser);
        return ResponseEntity.ok().build();
    }
}