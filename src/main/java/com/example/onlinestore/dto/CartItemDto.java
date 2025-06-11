package com.example.onlinestore.dto;

import com.example.onlinestore.model.Product; // To hold product details
import com.example.onlinestore.model.ProductImage; // For productImageUrl

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CartItemDto {
    private Long productId;
    private String productName; // Denormalized for easy display
    private String productImageUrl; // Denormalized
    private Integer quantity;
    private BigDecimal priceAtAddition; // Price when item was added
    private BigDecimal currentPrice; // Current price from Product service, for display
    private LocalDateTime addedAt;
    private Integer stockQuantity; // Current stock quantity of the product

    // Constructor, Getters, Setters
    public CartItemDto() {}

    public CartItemDto(Product product, int quantity, BigDecimal priceAtAddition) {
        this.productId = product.getId();
        this.productName = product.getName();
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            // Assuming the first image or primary image is used
            this.productImageUrl = product.getImages().stream()
                                          .filter(img -> img.isPrimary() || product.getImages().iterator().next() == img) // Prefer primary
                                          .findFirst()
                                          .map(ProductImage::getImageUrl)
                                          .orElse(null);
             // Fallback if no primary, take the first one from the set if any
            if (this.productImageUrl == null && product.getImages() != null && !product.getImages().isEmpty()) {
                 this.productImageUrl = product.getImages().iterator().next().getImageUrl();
            }
        }
        this.quantity = quantity;
        this.priceAtAddition = priceAtAddition;
        this.currentPrice = product.getPrice(); // Current price
        this.addedAt = LocalDateTime.now();
        this.stockQuantity = product.getStockQuantity();
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductImageUrl() { return productImageUrl; }
    public void setProductImageUrl(String productImageUrl) { this.productImageUrl = productImageUrl; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getPriceAtAddition() { return priceAtAddition; }
    public void setPriceAtAddition(BigDecimal priceAtAddition) { this.priceAtAddition = priceAtAddition; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public BigDecimal getSubtotal() {
        if (priceAtAddition != null && quantity != null) {
            return priceAtAddition.multiply(BigDecimal.valueOf(quantity));
        }
        return BigDecimal.ZERO;
    }
}
