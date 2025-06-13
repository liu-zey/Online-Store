package com.example.onlinestore.service.impl;

import com.example.onlinestore.mapper.CategoryMapper;
import com.example.onlinestore.model.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category sampleCategory;

    @BeforeEach
    void setUp() {
        sampleCategory = new Category();
        sampleCategory.setId(1L);
        sampleCategory.setName("Electronics");
        sampleCategory.setDescription("Gadgets and devices");
    }

    @Test
    void createCategory_success() {
        when(categoryMapper.findByName(anyString())).thenReturn(null);
        // Simulate ID generation if your insertCategory in mapper modifies the object
        doAnswer(invocation -> {
            Category cat = invocation.getArgument(0);
            cat.setId(1L); // Simulate ID being set by MyBatis
            return null;
        }).when(categoryMapper).insertCategory(any(Category.class));

        Category newCategory = new Category();
        newCategory.setName("Books");
        Category created = categoryService.createCategory(newCategory);

        assertNotNull(created);
        assertEquals("Books", created.getName());
        assertNotNull(created.getId()); // Ensure ID is set
        verify(categoryMapper).insertCategory(any(Category.class));
    }

    @Test
    void createCategory_alreadyExists_throwsException() {
        Category existingCategory = new Category();
        existingCategory.setName("Electronics");
        when(categoryMapper.findByName("Electronics")).thenReturn(sampleCategory);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            categoryService.createCategory(existingCategory);
        });
        assertEquals("Category with name 'Electronics' already exists.", exception.getMessage());
    }

    @Test
    void createCategory_invalidParent_throwsException() {
        Category newCategory = new Category();
        newCategory.setName("SubBooks");
        newCategory.setParentId(99L); // Non-existent parent
        when(categoryMapper.findByName(anyString())).thenReturn(null); // Name is unique
        when(categoryMapper.findById(99L)).thenReturn(null); // Parent does not exist

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            categoryService.createCategory(newCategory);
        });
        assertEquals("Parent category with id '99' does not exist.", exception.getMessage());
    }

    @Test
    void getCategoryById_found() {
        when(categoryMapper.findById(1L)).thenReturn(sampleCategory);
        Category found = categoryService.getCategoryById(1L);
        assertNotNull(found);
        assertEquals("Electronics", found.getName());
    }

    @Test
    void updateCategory_success() {
        Category updatedDetails = new Category();
        updatedDetails.setName("Digital Goods");
        updatedDetails.setDescription("Software and digital items");

        when(categoryMapper.findById(1L)).thenReturn(sampleCategory);
        when(categoryMapper.findByName("Digital Goods")).thenReturn(null); // No name conflict
        doNothing().when(categoryMapper).updateCategory(any(Category.class));

        Category updated = categoryService.updateCategory(1L, updatedDetails);

        assertNotNull(updated);
        assertEquals("Digital Goods", updated.getName());
        verify(categoryMapper).updateCategory(sampleCategory);
    }

    @Test
    void updateCategory_notFound_throwsException() {
        Category updatedDetails = new Category();
        updatedDetails.setName("Digital Goods");
        when(categoryMapper.findById(1L)).thenReturn(null);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            categoryService.updateCategory(1L, updatedDetails);
        });
        assertEquals("Category with id '1' not found.", exception.getMessage());
    }

    @Test
    void deleteCategory_success() {
        when(categoryMapper.findById(1L)).thenReturn(sampleCategory);
        doNothing().when(categoryMapper).deleteCategory(1L);
        categoryService.deleteCategory(1L);
        verify(categoryMapper).deleteCategory(1L);
    }
}
