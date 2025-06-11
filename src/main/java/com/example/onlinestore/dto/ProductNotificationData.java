package com.example.onlinestore.dto;

import java.math.BigDecimal;
import java.util.Objects;

public class ProductNotificationData {

    private String name;
    private String description;
    private BigDecimal price;
    private String productUrl;

    public ProductNotificationData() {
    }

    public ProductNotificationData(String name, String description, BigDecimal price, String productUrl) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.productUrl = productUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getProductUrl() {
        return productUrl;
    }

    public void setProductUrl(String productUrl) {
        this.productUrl = productUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductNotificationData that = (ProductNotificationData) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(description, that.description) &&
               Objects.equals(price, that.price) &&
               Objects.equals(productUrl, that.productUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, price, productUrl);
    }

    @Override
    public String toString() {
        return "ProductNotificationData{" +
               "name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", price=" + price +
               ", productUrl='" + productUrl + '\'' +
               '}';
    }
}
