package com.example.onlinestore.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class UpdateCartItemRequest {
    @NotNull(message = "Quantity cannot be null")
    @Min(value = 0, message = "Quantity cannot be negative. Use 0 to remove item if desired, or use DELETE endpoint.")
    // Note: CartService.updateItemInCart handles quantity 0 as removal.
    private Integer quantity;

    // Getters and Setters
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
