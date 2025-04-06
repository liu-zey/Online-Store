package com.example.onlinestore.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public class ProductPageRequest {
    @Min(value = 1, message = "error.page.number.min")
    private int pageNum = 1;

    @Min(value = 1, message = "error.page.size.min")
    @Max(value = 100, message = "error.page.size.max")
    private int pageSize = 10;

    private String name;

    /**
     * Retrieves the current page number for the product page request.
     *
     * @return the current page number, which is guaranteed to be at least 1
     */
    public int getPageNum() {
        return pageNum;
    }

    /**
     * Sets the page number for the product page request.
     *
     * @param pageNum the new page number; must be at least 1
     */
    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    /**
     * Returns the page size indicating the number of items to display per page.
     *
     * @return the current page size
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Updates the number of items per page for the product page request.
     *
     * @param pageSize the new page size value, which should be between 1 and 100
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Returns the name of the product being requested.
     *
     * @return the product name, or null if not specified
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the product name filter used to refine product page results.
     *
     * @param name the product name to set
     */
    public void setName(String name) {
        this.name = name;
    }
} 