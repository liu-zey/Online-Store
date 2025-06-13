package com.example.onlinestore.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CartDto {
    private List<CartItemDto> items;
    private BigDecimal grandTotal;
    private Long userId; // Optional: for context

    public CartDto() {
        this.items = new ArrayList<>();
        this.grandTotal = BigDecimal.ZERO;
    }

    // Getters, Setters
    public List<CartItemDto> getItems() { return items; }
    public void setItems(List<CartItemDto> items) { this.items = items; }
    public BigDecimal getGrandTotal() { return grandTotal; }
    public void setGrandTotal(BigDecimal grandTotal) { this.grandTotal = grandTotal; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public void calculateGrandTotal() {
        this.grandTotal = BigDecimal.ZERO;
        if (this.items != null) {
            for (CartItemDto item : this.items) {
                if (item.getSubtotal() != null) { // Ensure subtotal is not null
                    this.grandTotal = this.grandTotal.add(item.getSubtotal());
                }
            }
        }
    }
}
