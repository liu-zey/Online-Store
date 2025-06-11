package com.example.onlinestore.controller;

import com.example.onlinestore.dto.PageResponse;
import com.example.onlinestore.dto.ProductCreateRequest;
import com.example.onlinestore.dto.ProductUpdateRequest;
import com.example.onlinestore.model.Product;
import com.example.onlinestore.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal; // For convertFilterParams
import java.util.Collections;
import java.util.HashMap; // For convertFilterParams
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api") // Base path
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // --- Admin Endpoints for Product Management ---

    @PostMapping("/admin/products")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')") // Simplified for now
    public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductCreateRequest productCreateRequest) {
        Product product = convertToEntity(productCreateRequest);
        Product createdProduct = productService.createProduct(product, productCreateRequest.getImages());
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    @PutMapping("/admin/products/{productId}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')") // Simplified for now
    public ResponseEntity<Product> updateProduct(@PathVariable Long productId,
                                               @Valid @RequestBody ProductUpdateRequest productUpdateRequest) {
        Product productDetails = convertToEntity(productUpdateRequest);
        Product updatedProduct = productService.updateProduct(productId, productDetails, productUpdateRequest.getImages());
        return ResponseEntity.ok(updatedProduct);
    }

    @PatchMapping("/admin/products/{productId}/status")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')") // Simplified for now
    public ResponseEntity<Product> updateProductStatus(@PathVariable Long productId, @RequestBody Map<String, String> statusMap) {
        String status = statusMap.get("status");
        if (status == null || status.trim().isEmpty()) {
            // Consider throwing custom exception handled by GlobalExceptionHandler
            return ResponseEntity.badRequest().body(null);
        }
        Product updatedProduct = productService.updateProductStatus(productId, status);
        return ResponseEntity.ok(updatedProduct);
    }

    @PatchMapping("/admin/products/{productId}/stock")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')") // Simplified for now
    public ResponseEntity<Product> updateProductStock(@PathVariable Long productId, @RequestBody Map<String, Integer> stockMap) {
        Integer stockQuantityChange = stockMap.get("stockQuantityChange");
        if (stockQuantityChange == null) {
             return ResponseEntity.badRequest().body(null);
        }
        Product updatedProduct = productService.updateStock(productId, stockQuantityChange);
        return ResponseEntity.ok(updatedProduct);
    }


    @DeleteMapping("/admin/products/{productId}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')") // Simplified for now
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId); // Soft delete
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/products")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')") // Simplified for now
    public ResponseEntity<PageResponse<Product>> getAllProductsForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Map<String, String> filterParams) {
        Map<String, Object> serviceFilterParams = convertFilterParams(filterParams);
        PageResponse<Product> products = productService.getAllProducts(serviceFilterParams, page, size);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/admin/products/{productId}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')") // Simplified for now
    public ResponseEntity<Product> getProductByIdForAdmin(@PathVariable Long productId) {
        Product product = productService.getProductById(productId);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }

    // --- Public Endpoints for Product Viewing by Customers ---

    @GetMapping("/products")
    public ResponseEntity<PageResponse<Product>> getAvailableProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Map<String, String> filterParams) {
        Map<String, Object> serviceFilterParams = convertFilterParams(filterParams);
        PageResponse<Product> products = productService.getAvailableProducts(serviceFilterParams, page, size);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/products/search")
    public ResponseEntity<PageResponse<Product>> searchAvailableProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Map<String, String> filterParams) {
        Map<String, Object> serviceFilterParams = convertFilterParams(filterParams);
        PageResponse<Product> products = productService.searchAvailableProducts(keyword, serviceFilterParams, page, size);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<Product> getProductByIdForCustomer(@PathVariable Long productId) {
        Product product = productService.getProductById(productId);
        if (product == null || !"ACTIVE".equalsIgnoreCase(product.getStatus())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }

    private Map<String, Object> convertFilterParams(Map<String, String> stringParams) {
        if (stringParams == null || stringParams.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> serviceParams = new HashMap<>();
        for (Map.Entry<String, String> entry : stringParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null || value.trim().isEmpty()) continue;

            // Basic type conversion examples - extend as needed
            if ("categoryId".equalsIgnoreCase(key) || "minStock".equalsIgnoreCase(key)) {
                try {
                    serviceParams.put(key, Long.parseLong(value));
                } catch (NumberFormatException e) {
                    // Log warning or throw bad request for invalid format
                }
            } else if ("minPrice".equalsIgnoreCase(key) || "maxPrice".equalsIgnoreCase(key)) {
                try {
                    serviceParams.put(key, new BigDecimal(value));
                } catch (NumberFormatException e) {
                    // Log warning or throw
                }
            } else if ("inStock".equalsIgnoreCase(key)) {
                 serviceParams.put(key, Boolean.parseBoolean(value));
            }
            else {
                serviceParams.put(key, value); // Default to string
            }
        }
        return serviceParams;
    }

    private Product convertToEntity(ProductCreateRequest dto) {
        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStockQuantity(dto.getStockQuantity());
        product.setSku(dto.getSku());
        product.setCategoryId(dto.getCategoryId());
        product.setStatus(dto.getStatus() == null ? "DRAFT" : dto.getStatus()); // Default status if not provided
        // Images are handled by ProductService using dto.getImages()
        return product;
    }

    private Product convertToEntity(ProductUpdateRequest dto) {
        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStockQuantity(dto.getStockQuantity());
        product.setSku(dto.getSku());
        product.setCategoryId(dto.getCategoryId());
        product.setStatus(dto.getStatus());
        return product;
    }
}
