package com.example.onlinestore.mapper;

import com.example.onlinestore.model.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {
    void insertProduct(Product product);
    
    List<Product> findWithPagination(@Param("name") String name, 
                                    @Param("offset") int offset, 
                                    @Param("limit") int limit);
    
    long countTotal(@Param("name") String name);

    List<Product> findAll();
} 