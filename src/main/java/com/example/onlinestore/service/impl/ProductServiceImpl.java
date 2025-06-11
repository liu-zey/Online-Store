package com.example.onlinestore.service.impl;

import com.example.onlinestore.dto.PageResponse;
import com.example.onlinestore.mapper.CategoryMapper;
import com.example.onlinestore.mapper.ProductMapper;
import com.example.onlinestore.model.Category;
import com.example.onlinestore.model.Product;
import com.example.onlinestore.model.ProductImage;
import com.example.onlinestore.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils; // For checking if list is empty

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper; // To validate categoryId

    @Autowired
    public ProductServiceImpl(ProductMapper productMapper, CategoryMapper categoryMapper) {
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
    }

    @Transactional
    @Override
    public Product createProduct(Product product, List<ProductImage> images) {
        Assert.notNull(product, "Product object must not be null.");
        Assert.hasText(product.getName(), "Product name must not be empty.");
        Assert.notNull(product.getPrice(), "Product price must not be null.");
        Assert.notNull(product.getStockQuantity(), "Product stock quantity must not be null.");
        Assert.notNull(product.getCategoryId(), "Product categoryId must not be null.");

        Category category = categoryMapper.findById(product.getCategoryId());
        if (category == null) {
            throw new IllegalArgumentException("Category with id '" + product.getCategoryId() + "' not found.");
        }
        // product.setCategory(category); // Set the category object if needed for some logic, though mapper only needs ID

        if (product.getSku() != null) {
            // Optional: Check for SKU uniqueness if required by business logic, though DB constraint is primary
        }

        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        if (product.getStatus() == null) {
            product.setStatus("DRAFT"); // Default status
        }
        productMapper.insertProduct(product); // This should set the generated ID on the product object

        if (!CollectionUtils.isEmpty(images)) {
            for (ProductImage image : images) {
                image.setProductId(product.getId());
                image.setCreatedAt(LocalDateTime.now());
                productMapper.insertProductImage(image);
            }
        }
        log.info("Created new product with id: {}", product.getId());
        // Re-fetch to get populated images and category from DB as defined in ProductResultMap
        return productMapper.findById(product.getId());
    }

    @Override
    public Product getProductById(Long productId) {
        Assert.notNull(productId, "Product ID must not be null.");
        // ProductResultMap should handle loading category and images
        return productMapper.findById(productId);
    }

    private PageResponse<Product> getPagedProducts(Map<String, Object> params, int page, int size, boolean availableOnly, String keyword) {
        Assert.isTrue(page >= 0, "Page number must be non-negative.");
        Assert.isTrue(size > 0, "Page size must be positive.");

        params.put("offset", page * size);
        params.put("limit", size);

        List<Product> products;
        int total;

        if (keyword != null && !keyword.trim().isEmpty()) {
            products = productMapper.searchAvailable(keyword.trim(), params);
            total = productMapper.countSearchAvailable(keyword.trim(), params);
        } else if (availableOnly) {
            products = productMapper.findAvailable(params);
            total = productMapper.countAvailable(params);
        } else {
            products = productMapper.findAll(params);
            total = productMapper.countAll(params);
        }

        return new PageResponse<>(products, page, size, total);
    }

    @Override
    public PageResponse<Product> getAllProducts(Map<String, Object> params, int page, int size) {
        return getPagedProducts(params, page, size, false, null);
    }

    @Override
    public PageResponse<Product> getAvailableProducts(Map<String, Object> params, int page, int size) {
        params.put("status", "ACTIVE"); // Ensure only active products are considered available
        return getPagedProducts(params, page, size, true, null);
    }

    @Override
    public PageResponse<Product> searchAvailableProducts(String keyword, Map<String, Object> params, int page, int size) {
        params.put("status", "ACTIVE"); // Ensure search is within active products
        return getPagedProducts(params, page, size, true, keyword);
    }

    @Transactional
    @Override
    public Product updateProduct(Long productId, Product productDetails, List<ProductImage> imagesDetails) {
        Assert.notNull(productId, "Product ID must not be null.");
        Assert.notNull(productDetails, "Product details must not be null.");

        Product existingProduct = productMapper.findById(productId);
        if (existingProduct == null) {
            throw new IllegalArgumentException("Product with id '" + productId + "' not found.");
        }

        // Update fields from productDetails
        if (productDetails.getName() != null) existingProduct.setName(productDetails.getName());
        if (productDetails.getDescription() != null) existingProduct.setDescription(productDetails.getDescription());
        if (productDetails.getPrice() != null) existingProduct.setPrice(productDetails.getPrice());
        if (productDetails.getStockQuantity() != null) existingProduct.setStockQuantity(productDetails.getStockQuantity());
        if (productDetails.getSku() != null) existingProduct.setSku(productDetails.getSku());
        if (productDetails.getCategoryId() != null) {
            Category category = categoryMapper.findById(productDetails.getCategoryId());
            if (category == null) {
                throw new IllegalArgumentException("Category with id '" + productDetails.getCategoryId() + "' not found.");
            }
            existingProduct.setCategoryId(productDetails.getCategoryId());
            // existingProduct.setCategory(category); // Update associated object if needed
        }
        if (productDetails.getStatus() != null) existingProduct.setStatus(productDetails.getStatus());

        existingProduct.setUpdatedAt(LocalDateTime.now());
        productMapper.updateProduct(existingProduct);

        // Handle images: delete existing and add new ones (simplest approach)
        // More sophisticated logic could compare and update/add/delete selectively.
        if (imagesDetails != null) { // If imagesDetails is null, don't touch existing images. If empty list, delete all.
            productMapper.deleteProductImagesByProductId(productId);
            if (!imagesDetails.isEmpty()) {
                for (ProductImage image : imagesDetails) {
                    image.setProductId(productId); // Ensure association
                    image.setCreatedAt(LocalDateTime.now()); // Set creation time for new images
                    productMapper.insertProductImage(image);
                }
            }
        }
        log.info("Updated product with id: {}", productId);
        return productMapper.findById(productId); // Re-fetch to get all updated associations
    }

    @Transactional
    @Override
    public Product updateProductStatus(Long productId, String status) {
        Assert.notNull(productId, "Product ID must not be null.");
        Assert.hasText(status, "Status must not be empty."); // Add validation for allowed statuses if using Enum

        Product existingProduct = productMapper.findById(productId);
        if (existingProduct == null) {
            throw new IllegalArgumentException("Product with id '" + productId + "' not found.");
        }
        existingProduct.setStatus(status);
        existingProduct.setUpdatedAt(LocalDateTime.now());
        productMapper.updateProduct(existingProduct); // Assuming updateProduct can update status
        log.info("Updated status of product id {} to {}", productId, status);
        return existingProduct;
    }

    @Transactional
    @Override
    public Product updateStock(Long productId, int stockQuantityChange) {
        Assert.notNull(productId, "Product ID must not be null.");
        Product product = productMapper.findById(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found with id: " + productId);
        }
        int newStock = product.getStockQuantity() + stockQuantityChange;
        if (newStock < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative.");
        }
        productMapper.updateStock(productId, newStock);
        log.info("Updated stock for product id {} to {}", productId, newStock);
        product.setStockQuantity(newStock); // Update in-memory object
        product.setUpdatedAt(LocalDateTime.now()); // Reflect stock update time
        return product;
    }

    @Transactional
    @Override
    public void deleteProduct(Long productId) { // Soft delete
        Assert.notNull(productId, "Product ID must not be null.");
        Product existingProduct = productMapper.findById(productId);
        if (existingProduct == null) {
            throw new IllegalArgumentException("Product with id '" + productId + "' not found.");
        }
        // Instead of actual deletion, change status to INACTIVE or ARCHIVED
        existingProduct.setStatus("INACTIVE"); // Or a more specific "ARCHIVED" status
        existingProduct.setUpdatedAt(LocalDateTime.now());
        productMapper.updateProduct(existingProduct);
        log.info("Soft deleted product with id: {} by setting status to INACTIVE", productId);
    }
}
