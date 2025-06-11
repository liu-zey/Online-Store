package com.example.onlinestore.mapper;

import com.example.onlinestore.model.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper {
    void insert(Product product);
}
