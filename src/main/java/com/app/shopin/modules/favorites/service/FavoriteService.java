package com.app.shopin.modules.favorites.service;

import com.app.shopin.modules.exception.CustomException;
import com.app.shopin.modules.favorites.dto.AssignProductToListsDTO;
import com.app.shopin.modules.favorites.dto.CreateFavoriteListDTO;
import com.app.shopin.modules.favorites.dto.FavoriteListDTO;
import com.app.shopin.modules.favorites.dto.UpdateFavoriteListDTO;
import com.app.shopin.modules.favorites.entity.FavoriteList;
import com.app.shopin.modules.favorites.entity.FavoriteListIcon;
import com.app.shopin.modules.favorites.repository.FavoriteListRepository;
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
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteListRepository favoriteListRepository;
    @Autowired
    private ProductRepository productRepository;

    // --- LÓGICA DE CREACIÓN INICIAL ---
    @Transactional
    public void createDefaultListForUser(User user) {
        // Se llama cuando un usuario nuevo se registra
        FavoriteList defaultList = new FavoriteList();
        defaultList.setName("Mis Favoritos");
        defaultList.setUser(user);
        defaultList.setIcon(FavoriteListIcon.HEART);
        favoriteListRepository.save(defaultList);
    }

    // --- GESTIÓN DE LISTAS ---
    @Transactional
    public FavoriteListDTO createList(CreateFavoriteListDTO dto, UserDetails currentUser) {
        User user = ((PrincipalUser) currentUser).getUser();

        // Evitamos nombres de lista duplicados para el mismo usuario
        if (favoriteListRepository.findByUserIdAndNameIgnoreCase(user.getId(), dto.name()).isPresent()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Ya tienes una lista con ese nombre.");
        }

        FavoriteList newList = new FavoriteList();
        newList.setName(dto.name());
        newList.setUser(user);
        newList.setIcon(dto.icon());

        FavoriteList savedList = favoriteListRepository.save(newList);
        return mapEntityToDto(savedList);
    }

    @Transactional
    public FavoriteListDTO updateListName(Long listId, UpdateFavoriteListDTO dto, UserDetails currentUser) {
        FavoriteList list = getAndVerifyOwnership(listId, currentUser);

        if ("Mis Favoritos".equalsIgnoreCase(list.getName())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "No se puede editar la lista de favoritos principal.");
        }
        // Actualizamos el nombre solo si se proporciona uno nuevo
        if (dto.name() != null && !dto.name().isBlank()) {
            list.setName(dto.name());
        }
        // Actualizamos el icono solo si se proporciona uno nuevo
        if (dto.icon() != null) {
            list.setIcon(dto.icon());
        }

        favoriteListRepository.save(list);
        return mapEntityToDto(list);
    }

    @Transactional
    public void deleteList(Long listId, UserDetails currentUser) {
        FavoriteList list = getAndVerifyOwnership(listId, currentUser);

        // Regla de negocio: No se puede borrar la lista por defecto "Mis Favoritos"
        if ("Mis Favoritos".equalsIgnoreCase(list.getName())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "No se puede eliminar la lista de favoritos principal.");
        }

        favoriteListRepository.delete(list);
    }

    // --- GESTIÓN DE PRODUCTOS EN LISTAS ---

    @Transactional
    public void assignProductToLists(AssignProductToListsDTO dto, UserDetails currentUser) {
        User user = ((PrincipalUser) currentUser).getUser();
        Product product = productRepository.findById(dto.productId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Producto no encontrado."));

        // 1. Obtenemos TODAS las listas del usuario.
        List<FavoriteList> allUserLists = favoriteListRepository.findByUserId(user.getId());

        // 2. Iteramos sobre cada lista del usuario para sincronizar.
        for (FavoriteList list : allUserLists) {
            // Verificamos que el usuario tenga permiso sobre la lista que se está intentando modificar
            // (Esta es una doble comprobación de seguridad)
            if (dto.listIds() != null && dto.listIds().contains(list.getId())) {
                // Si el ID de la lista está en la petición, nos aseguramos de que el producto esté dentro.
                list.getProducts().add(product);
            } else {
                // Si el ID de la lista NO está en la petición, nos aseguramos de que el producto NO esté dentro.
                list.getProducts().remove(product);
            }
        }

        // Guardamos todas las listas modificadas
        favoriteListRepository.saveAll(allUserLists);
    }

    // --- NUEVO MÉTODO PARA AÑADIR A UNA SOLA LISTA ---
    @Transactional
    public void addProductToSingleList(Long listId, Long productId, UserDetails currentUser) {
        FavoriteList list = getAndVerifyOwnership(listId, currentUser);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Producto no encontrado."));

        // Simplemente añade el producto a la colección de la lista
        list.getProducts().add(product);
        favoriteListRepository.save(list);
    }

    // --- MÉTODOS DE LECTURA ---

    @Transactional(readOnly = true)
    public List<FavoriteListDTO> getListsForUser(UserDetails currentUser) {
        User user = ((PrincipalUser) currentUser).getUser();
        return favoriteListRepository.findByUserId(user.getId()).stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FavoriteListDTO getListById(Long listId, UserDetails currentUser) {
        FavoriteList list = getAndVerifyOwnership(listId, currentUser);
        return mapEntityToDto(list);
    }

    // --- MÉTODOS DE AYUDA ---

    private FavoriteList getAndVerifyOwnership(Long listId, UserDetails currentUser) {
        User user = ((PrincipalUser) currentUser).getUser();
        FavoriteList list = favoriteListRepository.findById(listId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Lista de favoritos no encontrada."));

        if (!list.getUser().getId().equals(user.getId())) {
            throw new CustomException(HttpStatus.FORBIDDEN, "No tienes permiso para acceder a esta lista.");
        }
        return list;
    }

    private FavoriteListDTO mapEntityToDto(FavoriteList list) {
        List<Long> productIds = list.getProducts().stream()
                .map(Product::getId)
                .collect(Collectors.toList());

        return new FavoriteListDTO(list.getId(), list.getName(), list.getIcon(), productIds);
    }
}