package com.example.onlinestore.mapper;

import com.example.onlinestore.model.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {
    /**
 * Inserts a new product into the database.
 *
 * @param product the product entity to be inserted
 */
void insertProduct(Product product);
    
    /**
                                     * Retrieves a paginated list of products that match the specified name filter.
                                     *
                                     * @param name   the product name filter; only products containing this string will be returned
                                     * @param offset the index from which to start returning products, used for pagination
                                     * @param limit  the maximum number of products to return
                                     * @return a list of products that match the filter criteria within the specified pagination range
                                     */
                                    List<Product> findWithPagination(@Param("name") String name, 
                                    @Param("offset") int offset, 
                                    @Param("limit") int limit);
    
    /**
 * Counts the total number of products that match the specified name.
 *
 * @param name the name criteria for filtering products
 * @return the total number of products matching the specified name
 */
long countTotal(@Param("name") String name);

    /**
 * Retrieves all Product entries from the database.
 *
 * @return a list containing all Product objects.
 */
List<Product> findAll();
} 