package com.example.onlinestore.mapper;

import com.example.onlinestore.model.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CategoryMapper {

    Category findById(@Param("id") Long id);

    Category findByName(@Param("name") String name);

    List<Category> findAll();

    List<Category> findByParentId(@Param("parentId") Long parentId);

    void insertCategory(Category category);

    void updateCategory(Category category);

    void deleteCategory(@Param("id") Long id);
}
