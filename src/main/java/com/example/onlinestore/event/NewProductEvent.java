package com.example.onlinestore.event;

import com.example.onlinestore.model.Product;
import org.springframework.context.ApplicationEvent;

public class NewProductEvent extends ApplicationEvent {

    private final Product product;

    public NewProductEvent(Product product) {
        super(product);
        this.product = product;
    }

    public Product getProduct() {
        return product;
    }
}
