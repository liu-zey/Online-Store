package com.example.onlinestore.model;

import java.time.LocalDateTime;
// import java.util.HashSet;
// import java.util.Set;

public class Category {
    private Long id;
    private String name;
    private String description;
    private Long parentId; // For sub-categories
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Conceptual parent category (loaded separately or via join)
    // private Category parentCategory;
    // Conceptual children categories (loaded separately)
    // private Set<Category> childCategories = new HashSet<>();
    // Conceptual products in this category (loaded separately)
    // private Set<Product> products = new HashSet<>();


    public Category() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // public Category getParentCategory() { return parentCategory; }
    // public void setParentCategory(Category parentCategory) { this.parentCategory = parentCategory; }
    // public Set<Category> getChildCategories() { return childCategories; }
    // public void setChildCategories(Set<Category> childCategories) { this.childCategories = childCategories; }
    // public Set<Product> getProducts() { return products; }
    // public void setProducts(Set<Product> products) { this.products = products; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return id != null ? id.equals(category.id) : category.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", parentId=" + parentId +
                '}';
    }
}
