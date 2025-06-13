package com.example.onlinestore.mapper;

import com.example.onlinestore.model.Product;
import com.example.onlinestore.model.ProductImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map; // For dynamic query parameters

@Mapper
public interface ProductMapper {

    Product findById(@Param("id") Long id);

    // For admin view, might include inactive products
    List<Product> findAll(Map<String, Object> params);
    int countAll(Map<String, Object> params);

    // For customer view, typically only active products
    List<Product> findAvailable(Map<String, Object> params);
    int countAvailable(Map<String, Object> params);

    List<Product> searchAvailable(@Param("keyword") String keyword, Map<String, Object> params);
    int countSearchAvailable(@Param("keyword") String keyword, Map<String, Object> params);

    void insertProduct(Product product);

    void updateProduct(Product product);

    void updateStock(@Param("id") Long id, @Param("stockQuantity") Integer stockQuantity);

    void deleteProduct(@Param("id") Long id); // Or soft delete by updating status

    // ProductImage related methods (if managed within ProductMapper)
    void insertProductImage(ProductImage productImage);

    void deleteProductImagesByProductId(@Param("productId") Long productId);

    List<ProductImage> findImagesByProductId(@Param("productId") Long productId);
}
