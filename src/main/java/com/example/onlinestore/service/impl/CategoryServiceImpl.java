package com.example.onlinestore.service.impl;

import com.example.onlinestore.mapper.CategoryMapper;
import com.example.onlinestore.model.Category;
import com.example.onlinestore.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryMapper categoryMapper;
    // private final ProductMapper productMapper; // For checks before deletion, if needed

    @Autowired
    public CategoryServiceImpl(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Transactional
    @Override
    public Category createCategory(Category category) {
        Assert.notNull(category, "Category object must not be null.");
        Assert.hasText(category.getName(), "Category name must not be empty.");

        if (categoryMapper.findByName(category.getName()) != null) {
            throw new IllegalArgumentException("Category with name '" + category.getName() + "' already exists.");
        }
        if (category.getParentId() != null && categoryMapper.findById(category.getParentId()) == null) {
             throw new IllegalArgumentException("Parent category with id '" + category.getParentId() + "' does not exist.");
        }

        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.insertCategory(category);
        log.info("Created new category with id: {}, name: {}", category.getId(), category.getName());
        return category;
    }

    @Override
    public Category getCategoryById(Long categoryId) {
        Assert.notNull(categoryId, "Category ID must not be null.");
        return categoryMapper.findById(categoryId);
    }

    @Override
    public Category getCategoryByName(String name) {
        Assert.hasText(name, "Category name must not be empty.");
        return categoryMapper.findByName(name);
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryMapper.findAll();
    }

    @Override
    public List<Category> getChildCategories(Long parentId) {
        Assert.notNull(parentId, "Parent Category ID must not be null.");
        return categoryMapper.findByParentId(parentId);
    }

    @Transactional
    @Override
    public Category updateCategory(Long categoryId, Category categoryDetails) {
        Assert.notNull(categoryId, "Category ID must not be null.");
        Assert.notNull(categoryDetails, "Category details must not be null.");
        Assert.hasText(categoryDetails.getName(), "Category name must not be empty.");

        Category existingCategory = categoryMapper.findById(categoryId);
        if (existingCategory == null) {
            throw new IllegalArgumentException("Category with id '" + categoryId + "' not found.");
        }

        // Check for name conflict if name is being changed
        Category categoryByName = categoryMapper.findByName(categoryDetails.getName());
        if (categoryByName != null && !categoryByName.getId().equals(categoryId)) {
            throw new IllegalArgumentException("Category with name '" + categoryDetails.getName() + "' already exists.");
        }

        if (categoryDetails.getParentId() != null) {
            if (categoryDetails.getParentId().equals(categoryId)) {
                 throw new IllegalArgumentException("Category cannot be its own parent.");
            }
            if (categoryMapper.findById(categoryDetails.getParentId()) == null) {
                 throw new IllegalArgumentException("Parent category with id '" + categoryDetails.getParentId() + "' does not exist.");
            }
        }

        existingCategory.setName(categoryDetails.getName());
        existingCategory.setDescription(categoryDetails.getDescription());
        existingCategory.setParentId(categoryDetails.getParentId());
        existingCategory.setUpdatedAt(LocalDateTime.now());

        categoryMapper.updateCategory(existingCategory);
        log.info("Updated category with id: {}", categoryId);
        return existingCategory;
    }

    @Transactional
    @Override
    public void deleteCategory(Long categoryId) {
        Assert.notNull(categoryId, "Category ID must not be null.");
        Category existingCategory = categoryMapper.findById(categoryId);
        if (existingCategory == null) {
            throw new IllegalArgumentException("Category with id '" + categoryId + "' not found.");
        }

        // Optional: Add checks here e.g., prevent deletion if category has child categories or products.
        // List<Category> childCategories = categoryMapper.findByParentId(categoryId);
        // if (childCategories != null && !childCategories.isEmpty()) {
        //    throw new IllegalStateException("Cannot delete category with id '" + categoryId + "' as it has child categories.");
        // }
        // List<Product> productsInCategory = productMapper.findByCategoryId(categoryId); // Assuming such method exists
        // if (productsInCategory != null && !productsInCategory.isEmpty()) {
        //    throw new IllegalStateException("Cannot delete category with id '" + categoryId + "' as it contains products.");
        // }

        categoryMapper.deleteCategory(categoryId);
        log.info("Deleted category with id: {}", categoryId);
    }
}
