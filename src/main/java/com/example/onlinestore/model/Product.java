package com.example.onlinestore.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Product {
    private Long id;
    private String name;
    private String category;
    private BigDecimal price;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Retrieves the product's unique identifier.
     *
     * @return the product id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier for the product.
     *
     * @param id the unique identifier to assign to the product
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the product's name.
     *
     * @return the name of the product
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the product.
     *
     * @param name the new name for the product
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the category of the product.
     *
     * @return the product's category as a String
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the category of the product.
     *
     * @param category the product category to set
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Returns the price of the product.
     *
     * @return the product's price as a BigDecimal
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * Sets the price of the product.
     *
     * @param price the new price to assign to the product
     */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /**
     * Returns the timestamp when the product was created.
     *
     * @return the creation timestamp as a LocalDateTime instance
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp for this product.
     *
     * @param createdAt the new timestamp indicating when the product was created
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Returns the timestamp when the product was last updated.
     *
     * @return the LocalDateTime representing the product's last update time
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the timestamp when this product was last updated.
     *
     * @param updatedAt the new timestamp reflecting the product's last update
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
} 