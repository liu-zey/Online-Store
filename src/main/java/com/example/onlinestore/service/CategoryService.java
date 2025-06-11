package com.example.onlinestore.service;

import com.example.onlinestore.model.Category;
import java.util.List;

public interface CategoryService {
    Category createCategory(Category category);
    Category getCategoryById(Long categoryId);
    Category getCategoryByName(String name);
    List<Category> getAllCategories();
    List<Category> getChildCategories(Long parentId);
    Category updateCategory(Long categoryId, Category categoryDetails);
    void deleteCategory(Long categoryId);
}
