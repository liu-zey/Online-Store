package com.example.onlinestore.service;

import com.example.onlinestore.dto.PageResponse; // Re-using if structure is suitable
import com.example.onlinestore.model.Product;
import com.example.onlinestore.model.ProductImage;

import java.util.List;
import java.util.Map;

public interface ProductService {
    Product createProduct(Product product, List<ProductImage> images);
    Product getProductById(Long productId);
    // Product getProductBySku(String sku); // Optional

    PageResponse<Product> getAllProducts(Map<String, Object> params, int page, int size); // For Admin
    PageResponse<Product> getAvailableProducts(Map<String, Object> params, int page, int size); // For Customer
    PageResponse<Product> searchAvailableProducts(String keyword, Map<String, Object> params, int page, int size);

    Product updateProduct(Long productId, Product productDetails, List<ProductImage> imagesDetails);
    Product updateProductStatus(Long productId, String status);
    Product updateStock(Long productId, int stockQuantityChange); // Can be positive or negative

    void deleteProduct(Long productId); // Soft delete by changing status to INACTIVE or ARCHIVED

    // ProductImage specific methods (could be part of ProductService or a separate ProductImageService)
    // For now, let's keep basic image management here, tied to product updates.
    // List<ProductImage> getProductImages(Long productId);
    // ProductImage addImageToProduct(Long productId, ProductImage image);
    // void removeImageFromProduct(Long productId, Long imageId);
}
