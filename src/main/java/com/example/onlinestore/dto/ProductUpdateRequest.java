package com.example.onlinestore.dto;

import com.example.onlinestore.model.ProductImage;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.List; // Allow null to indicate "do not update images"

public class ProductUpdateRequest {
    // Not all fields need to be @NotBlank or @NotNull, as it's an update (partial updates allowed)
    private String name;
    private String description;
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0 if provided")
    private BigDecimal price;
    @Min(value = 0, message = "Stock quantity cannot be negative if provided")
    private Integer stockQuantity;
    private String sku;
    private Long categoryId;
    private String status;
    private List<ProductImage> images; // If null, images are not updated. If empty list, all images removed.

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<ProductImage> getImages() { return images; }
    public void setImages(List<ProductImage> images) { this.images = images; }
}
