package com.app.shopin.modules.product.service;

import com.app.shopin.modules.exception.CustomException;
import com.app.shopin.modules.product.dto.CategoryDTO;
import com.app.shopin.modules.product.entity.Category;
import com.app.shopin.modules.product.repository.CategoryRepository;
import com.app.shopin.modules.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository; // Inyectado para la validación de borrado

    // --- MÉTODOS DE CREACIÓN Y ACTUALIZACIÓN ---

    @Transactional
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        Category newCategory = new Category();
        newCategory.setName(categoryDTO.name());
        newCategory.setDescription(categoryDTO.description());

        // Lógica para asignar la categoría padre
        if (categoryDTO.parentId() != null) {
            Category parentCategory = categoryRepository.findById(categoryDTO.parentId())
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "La categoría padre con ID " + categoryDTO.parentId() + " no existe."));
            newCategory.setParent(parentCategory);
        }

        Category savedCategory = categoryRepository.save(newCategory);
        return mapEntityToDto(savedCategory);
    }

    @Transactional
    public CategoryDTO updateCategory(Long categoryId, CategoryDTO categoryDTO) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "La categoría con ID " + categoryId + " no existe."));

        category.setName(categoryDTO.name());
        category.setDescription(categoryDTO.description());

        // Lógica para actualizar la categoría padre
        if (categoryDTO.parentId() != null) {
            // Regla de negocio: una categoría no puede ser su propio padre
            if (categoryId.equals(categoryDTO.parentId())) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Una categoría no puede ser su propio padre.");
            }
            Category parentCategory = categoryRepository.findById(categoryDTO.parentId())
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "La nueva categoría padre no existe."));
            category.setParent(parentCategory);
        } else {
            // Si el parentId es null, se convierte en una categoría de nivel superior
            category.setParent(null);
        }

        Category updatedCategory = categoryRepository.save(category);
        return mapEntityToDto(updatedCategory);
    }

    // --- MÉTODOS DE LECTURA ---

    @Transactional(readOnly = true)
    public List<CategoryDTO> getTopLevelCategories() {
        // Devuelve solo las categorías que no tienen padre
        return categoryRepository.findByParentId(null).stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDTO getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .map(this::mapEntityToDto)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Categoría no encontrada."));
    }

    // --- MÉTODO DE BORRADO ---

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Categoría no encontrada."));

        // REGLA DE NEGOCIO: No permitir borrar una categoría si tiene productos asociados.
        if (!productRepository.findByCategoryId(categoryId).isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "No se puede eliminar la categoría porque tiene productos asociados.");
        }

        // REGLA DE NEGOCIO: No permitir borrar una categoría si tiene subcategorías.
        if (!category.getChildren().isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "No se puede eliminar la categoría porque tiene subcategorías. Primero debe eliminar o mover las subcategorías.");
        }

        categoryRepository.delete(category);
    }

    // --- MÉTODO DE AYUDA (Mapper) ---

    private CategoryDTO mapEntityToDto(Category category) {
        Long parentId = (category.getParent() != null) ? category.getParent().getId() : null;
        return new CategoryDTO(
                category.getId(),
                category.getName(),
                category.getDescription(),
                parentId
        );
    }
}
