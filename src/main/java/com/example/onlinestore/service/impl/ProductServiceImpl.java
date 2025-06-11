package com.example.onlinestore.service.impl;

import com.example.onlinestore.dto.ProductCreateRequest;
import com.example.onlinestore.event.NewProductEvent;
import com.example.onlinestore.mapper.ProductMapper;
import com.example.onlinestore.model.Product;
import com.example.onlinestore.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public ProductServiceImpl(ProductMapper productMapper, ApplicationEventPublisher eventPublisher) {
        this.productMapper = productMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Product addProduct(ProductCreateRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCreatedAt(LocalDateTime.now());

        productMapper.insert(product);

        eventPublisher.publishEvent(new NewProductEvent(product));

        return product;
    }
}
