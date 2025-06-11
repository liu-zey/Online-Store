package com.example.onlinestore.controller;

import com.example.onlinestore.model.Category;
import com.example.onlinestore.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api") // Base path for all category related endpoints
public class CategoryController {

    private final CategoryService categoryService;

    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // --- Admin Endpoints for Category Management ---

    @PostMapping("/admin/categories")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Category> createCategory(@Valid @RequestBody Category categoryRequest) {
        // @Valid assumes Category model might have JSR 303 annotations.
        // If not, it has no effect here but is good practice for DTOs.
        Category createdCategory = categoryService.createCategory(categoryRequest);
        return new ResponseEntity<>(createdCategory, HttpStatus.CREATED);
    }

    @PutMapping("/admin/categories/{categoryId}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Category> updateCategory(@PathVariable Long categoryId,
                                                 @Valid @RequestBody Category categoryDetails) {
        Category updatedCategory = categoryService.updateCategory(categoryId, categoryDetails);
        // Service layer should throw exception if categoryId not found, handled by GlobalExceptionHandler
        return ResponseEntity.ok(updatedCategory);
    }

    @DeleteMapping("/admin/categories/{categoryId}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId); // Service should throw exception if not found
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/categories/{categoryId}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Category> getCategoryByIdForAdmin(@PathVariable Long categoryId) {
        Category category = categoryService.getCategoryById(categoryId);
        if (category == null) {
            // This could also throw a ResourceNotFoundException handled globally
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(category);
    }

    @GetMapping("/admin/categories")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<Category>> getAllCategoriesForAdmin() {
        List<Category> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }


    // --- Public Endpoints for Category Viewing by Customers ---

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAllCategoriesForCustomer() {
        List<Category> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/categories/{categoryId}")
    public ResponseEntity<Category> getCategoryByIdForCustomer(@PathVariable Long categoryId) {
        Category category = categoryService.getCategoryById(categoryId);
        if (category == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(category);
    }

    @GetMapping("/categories/{parentId}/subcategories")
    public ResponseEntity<List<Category>> getSubcategories(@PathVariable Long parentId) {
        // Ensure parentId category exists before fetching children, or handle gracefully
        Category parentCategory = categoryService.getCategoryById(parentId);
        if (parentCategory == null) {
            return ResponseEntity.notFound().build(); // Parent category not found
        }
        List<Category> subcategories = categoryService.getChildCategories(parentId);
        // getChildCategories should return empty list if no children, not null.
        return ResponseEntity.ok(subcategories);
    }
}
