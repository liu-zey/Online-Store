package com.example.onlinestore.service.impl;

import com.example.onlinestore.dto.PageResponse;
import com.example.onlinestore.mapper.CategoryMapper;
import com.example.onlinestore.mapper.ProductMapper;
import com.example.onlinestore.model.Category;
import com.example.onlinestore.model.Product;
import com.example.onlinestore.model.ProductImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;
    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product sampleProduct;
    private Category sampleCategory;
    private ProductImage sampleImage;

    @BeforeEach
    void setUp() {
        sampleCategory = new Category();
        sampleCategory.setId(1L);
        sampleCategory.setName("Electronics");

        sampleProduct = new Product();
        sampleProduct.setId(1L);
        sampleProduct.setName("Test Laptop");
        sampleProduct.setPrice(new BigDecimal("1200.00"));
        sampleProduct.setStockQuantity(10);
        sampleProduct.setCategoryId(1L);
        sampleProduct.setCategory(sampleCategory); // Assuming Product object can hold Category
        sampleProduct.setStatus("ACTIVE");

        sampleImage = new ProductImage();
        sampleImage.setId(1L);
        sampleImage.setProductId(1L);
        sampleImage.setImageUrl("http://example.com/image.jpg");
        sampleImage.setPrimary(true);
    }

    @Test
    void createProduct_success() {
        Product newProduct = new Product();
        newProduct.setName("New Gadget");
        newProduct.setPrice(new BigDecimal("99.99"));
        newProduct.setStockQuantity(50);
        newProduct.setCategoryId(1L);

        List<ProductImage> images = Collections.singletonList(new ProductImage());

        when(categoryMapper.findById(1L)).thenReturn(sampleCategory);
        // Simulate ID generation for product
        doAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(2L); // Simulate ID generation
            return null;
        }).when(productMapper).insertProduct(any(Product.class));
        doNothing().when(productMapper).insertProductImage(any(ProductImage.class));
        // Mock the re-fetch
        when(productMapper.findById(2L)).thenReturn(newProduct);


        Product created = productService.createProduct(newProduct, images);

        assertNotNull(created);
        assertEquals("New Gadget", created.getName());
        assertEquals(2L, created.getId());
        verify(productMapper).insertProduct(newProduct);
        verify(productMapper, times(images.size())).insertProductImage(any(ProductImage.class));
    }

    @Test
    void createProduct_invalidCategory_throwsException() {
        Product newProduct = new Product();
        newProduct.setName("New Gadget");
        newProduct.setPrice(new BigDecimal("99.99"));
        newProduct.setStockQuantity(50);
        newProduct.setCategoryId(99L); // Non-existent category

        when(categoryMapper.findById(99L)).thenReturn(null);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.createProduct(newProduct, Collections.emptyList());
        });
        assertEquals("Category with id '99' not found.", exception.getMessage());
    }

    @Test
    void getProductById_found() {
        when(productMapper.findById(1L)).thenReturn(sampleProduct);
        Product found = productService.getProductById(1L);
        assertNotNull(found);
        assertEquals("Test Laptop", found.getName());
    }

    @Test
    void updateProduct_success() {
        Product productDetails = new Product();
        productDetails.setName("Updated Laptop");
        productDetails.setPrice(new BigDecimal("1250.00"));
        List<ProductImage> newImages = Arrays.asList(new ProductImage());

        when(productMapper.findById(1L)).thenReturn(sampleProduct); // Initial find
        doNothing().when(productMapper).updateProduct(any(Product.class));
        doNothing().when(productMapper).deleteProductImagesByProductId(1L);
        doNothing().when(productMapper).insertProductImage(any(ProductImage.class));
        // Mock the re-fetch after update
        when(productMapper.findById(1L)).thenReturn(sampleProduct);


        Product updated = productService.updateProduct(1L, productDetails, newImages);

        assertNotNull(updated);
        assertEquals("Updated Laptop", updated.getName()); // Check if name was updated on sampleProduct
        verify(productMapper).updateProduct(sampleProduct);
        verify(productMapper).deleteProductImagesByProductId(1L);
        verify(productMapper, times(newImages.size())).insertProductImage(any(ProductImage.class));
    }

    @Test
    void updateProduct_noImagesProvided_doesNotDeleteExistingImages() {
        Product productDetails = new Product();
        productDetails.setName("Updated Laptop Name Only");

        when(productMapper.findById(1L)).thenReturn(sampleProduct); // Initial find
        doNothing().when(productMapper).updateProduct(any(Product.class));
         // Mock the re-fetch
        when(productMapper.findById(1L)).thenReturn(sampleProduct);

        Product updated = productService.updateProduct(1L, productDetails, null); // Pass null for imagesDetails

        assertNotNull(updated);
        verify(productMapper).updateProduct(sampleProduct);
        verify(productMapper, never()).deleteProductImagesByProductId(anyLong());
        verify(productMapper, never()).insertProductImage(any(ProductImage.class));
    }

    @Test
    void updateProduct_emptyImageListProvided_deletesExistingImages() {
        Product productDetails = new Product();
        productDetails.setName("Updated Laptop Name Only");
        List<ProductImage> emptyImages = new ArrayList<>();


        when(productMapper.findById(1L)).thenReturn(sampleProduct); // Initial find
        doNothing().when(productMapper).updateProduct(any(Product.class));
        doNothing().when(productMapper).deleteProductImagesByProductId(1L);
         // Mock the re-fetch
        when(productMapper.findById(1L)).thenReturn(sampleProduct);

        Product updated = productService.updateProduct(1L, productDetails, emptyImages);

        assertNotNull(updated);
        verify(productMapper).updateProduct(sampleProduct);
        verify(productMapper).deleteProductImagesByProductId(1L);
        verify(productMapper, never()).insertProductImage(any(ProductImage.class));
    }

    @Test
    void deleteProduct_softDeletes() {
        when(productMapper.findById(1L)).thenReturn(sampleProduct);
        doNothing().when(productMapper).updateProduct(any(Product.class));

        productService.deleteProduct(1L);

        assertEquals("INACTIVE", sampleProduct.getStatus());
        verify(productMapper).updateProduct(sampleProduct);
    }

    @Test
    void getAvailableProducts_callsMapperWithCorrectParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("categoryId", 1L);
        int page = 0;
        int size = 10;

        List<Product> products = Arrays.asList(sampleProduct);
        when(productMapper.findAvailable(anyMap())).thenReturn(products);
        when(productMapper.countAvailable(anyMap())).thenReturn(1);

        PageResponse<Product> response = productService.getAvailableProducts(params, page, size);

        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getContent().size());
        verify(productMapper).findAvailable(argThat(map ->
            "ACTIVE".equals(map.get("status")) &&
            Integer.valueOf(0).equals(map.get("offset")) &&
            Integer.valueOf(10).equals(map.get("limit")) &&
            Long.valueOf(1L).equals(map.get("categoryId"))
        ));
    }
}
