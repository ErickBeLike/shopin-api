package com.app.shopin.modules.cart.service;

import com.app.shopin.modules.cart.dto.AddOrUpdateCartItemDTO;
import com.app.shopin.modules.cart.dto.CartDTO;
import com.app.shopin.modules.cart.dto.CartItemDTO;
import com.app.shopin.modules.cart.entity.Cart;
import com.app.shopin.modules.cart.entity.CartItem;
import com.app.shopin.modules.cart.repository.CartItemRepository;
import com.app.shopin.modules.cart.repository.CartRepository;
import com.app.shopin.modules.exception.CustomException;
import com.app.shopin.modules.favorites.dto.CreateFavoriteListDTO;
import com.app.shopin.modules.favorites.dto.FavoriteListDTO;
import com.app.shopin.modules.favorites.entity.FavoriteList;
import com.app.shopin.modules.favorites.entity.FavoriteListIcon;
import com.app.shopin.modules.favorites.repository.FavoriteListRepository;
import com.app.shopin.modules.favorites.service.FavoriteService;
import com.app.shopin.modules.product.entity.Product;
import com.app.shopin.modules.product.repository.ProductRepository;
import com.app.shopin.modules.security.entity.PrincipalUser;
import com.app.shopin.modules.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private FavoriteService favoriteService;
    @Autowired
    private FavoriteListRepository favoriteListRepository;

    // --- LÓGICA DE CREACIÓN INICIAL ---
    @Transactional
    public void createCartForUser(User user) {
        // Se llama cuando un usuario nuevo se registra
        Cart cart = new Cart();
        cart.setUser(user);
        cartRepository.save(cart);
    }

    // --- GESTIÓN DE ITEMS DEL CARRITO ---

    @Transactional
    public CartDTO addOrUpdateItem(AddOrUpdateCartItemDTO dto, UserDetails currentUser) {
        Cart cart = getCartForUser(currentUser);
        Product product = productRepository.findById(dto.productId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Producto no encontrado."));

        // Validamos el stock disponible
        if (product.getStockQuantity() < dto.quantity()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "No hay suficiente stock para '" + product.getName() + "'. Disponibles: " + product.getStockQuantity());
        }

        // Validamos tu regla de negocio de cantidad máxima por compra
        if (product.getMaxPurchaseAmount() != null && dto.quantity() > product.getMaxPurchaseAmount()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Solo puedes comprar un máximo de " + product.getMaxPurchaseAmount() + " unidades de '" + product.getName() + "'.");
        }

        // Buscamos si el item ya existe en el carrito
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartAndProduct(cart, product);

        if (existingItemOpt.isPresent()) {
            // Si existe, actualizamos la cantidad
            CartItem item = existingItemOpt.get();
            item.setQuantity(dto.quantity());
            cartItemRepository.save(item);
        } else {
            // Si no existe, creamos uno nuevo
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(dto.quantity());
            cartItemRepository.save(newItem);
        }

        // Devolvemos el estado actualizado del carrito
        return getCartDTOForUser(currentUser);
    }

    @Transactional
    public CartDTO removeItem(Long productId, UserDetails currentUser) {
        Cart cart = getCartForUser(currentUser);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Producto no encontrado."));

        // Buscamos el item que queremos eliminar
        Optional<CartItem> itemToRemoveOpt = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        if (itemToRemoveOpt.isPresent()) {
            CartItem itemToRemove = itemToRemoveOpt.get();

            cart.getItems().remove(itemToRemove);
        }

        cartRepository.save(cart);

        return getCartDTOForUser(currentUser);
    }

    @Transactional
    public CartDTO clearCart(UserDetails currentUser) {
        Cart cart = getCartForUser(currentUser);
        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear(); // Aseguramos que la lista en memoria también se limpie
        cartRepository.save(cart);
        return getCartDTOForUser(currentUser);
    }

    // --- LÓGICA PARA "GUARDAR PARA DESPUÉS" ---
    @Transactional
    public void saveItemForLater(Long productId, UserDetails currentUser) {
        // 1. Buscamos la lista "Guardados para después"
        User user = ((PrincipalUser) currentUser).getUser();
        FavoriteList savedList = favoriteListRepository.findByUserUserIdAndNameIgnoreCase(user.getUserId(), "Guardados para después")
                .orElseGet(() -> {
                    // Si no existe, la creamos usando el DTO correcto y un icono por defecto
                    CreateFavoriteListDTO newListDTO = new CreateFavoriteListDTO("Guardados para después", FavoriteListIcon.BOX);
                    FavoriteListDTO createdDto = favoriteService.createList(newListDTO, currentUser);
                    // La volvemos a buscar para tener la entidad completa
                    return favoriteListRepository.findById(createdDto.id()).orElseThrow();
                });

        // 2. Añadimos el producto a esa lista usando nuestro nuevo método "bisturí"
        favoriteService.addProductToSingleList(savedList.getId(), productId, currentUser);

        // 3. Quitamos el producto del carrito
        removeItem(productId, currentUser);
    }

    // --- MÉTODOS DE LECTURA ---
    @Transactional(readOnly = true)
    public CartDTO getCartDTOForUser(UserDetails currentUser) {
        Cart cart = getCartForUser(currentUser);
        return mapEntityToDto(cart);
    }

    // --- MÉTODO DE LECTURA PARA ADMINS/PRUEBAS ---
    @Transactional(readOnly = true)
    public List<CartDTO> getAllCarts() {
        return cartRepository.findAll().stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    // --- MÉTODOS DE AYUDA ---
    private Cart getCartForUser(UserDetails currentUser) {
        User user = ((PrincipalUser) currentUser).getUser();
        return cartRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Carrito no encontrado para el usuario."));
    }

    private CartDTO mapEntityToDto(Cart cart) {
        List<CartItemDTO> itemDTOs = cart.getItems().stream().map(item -> {
            Product p = item.getProduct();
            String imageUrl = (p.getMedia() != null && !p.getMedia().isEmpty()) ? p.getMedia().get(0).getUrl() : null;
            return new CartItemDTO(
                    item.getId(),
                    p.getId(),
                    p.getName(),
                    item.getQuantity(),
                    p.getEffectivePrice(),
                    item.getSubtotal(),
                    imageUrl
            );
        }).collect(Collectors.toList());

        return new CartDTO(cart.getId(), itemDTOs, cart.getGrandTotal());
    }
}