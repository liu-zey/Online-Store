package com.example.onlinestore.controller;

import com.example.onlinestore.dto.ProductCreateRequest;
import com.example.onlinestore.model.Product;
import com.example.onlinestore.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<Product> addProduct(@Valid @RequestBody ProductCreateRequest request) {
        Product product = productService.addProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }
}
