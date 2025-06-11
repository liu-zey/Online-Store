package com.example.onlinestore.service;

import com.example.onlinestore.dto.ProductCreateRequest;
import com.example.onlinestore.model.Product;

public interface ProductService {
    Product addProduct(ProductCreateRequest request);
}
