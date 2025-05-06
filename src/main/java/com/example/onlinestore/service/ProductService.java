package com.example.onlinestore.service;

import com.example.onlinestore.dto.CreateProductRequest;
import com.example.onlinestore.dto.PageResponse;
import com.example.onlinestore.dto.ProductPageRequest;
import com.example.onlinestore.model.Product;

public interface ProductService {
    /**
 * Creates a new product based on the provided creation request.
 *
 * @param request the product creation request containing the necessary product details
 * @return the newly created product
 */
Product createProduct(CreateProductRequest request);
    /**
 * Retrieves a paginated list of products based on the specified criteria.
 *
 * @param request the product page request containing pagination and filter parameters
 * @return a paginated response with the list of products and pagination details
 */
PageResponse<Product> listProducts(ProductPageRequest request);
} 