package com.example.onlinestore.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CreateProductRequest {
    @NotBlank(message = "error.product.name.empty")
    private String name;

    @NotBlank(message = "error.product.category.empty")
    private String category;

    @NotNull(message = "error.product.price.empty")
    @DecimalMin(value = "0.01", message = "error.product.price.min")
    private BigDecimal price;

    /**
     * Retrieves the product's name.
     *
     * @return the name of the product
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the product's name.
     *
     * <p>Assigns the provided name to the CreateProductRequest instance. The name must not be blank.</p>
     *
     * @param name the product's name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Retrieves the category of the product.
     *
     * @return the product's category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the category of the product.
     *
     * @param category the new product category, which must not be blank.
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
     * Sets the product's price.
     *
     * <p>The provided price is expected to be non-null and at least 0.01,
     * in line with the validation constraints.
     *
     * @param price the product price to set
     */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
} 