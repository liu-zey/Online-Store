package com.example.onlinestore.controller;

import com.example.onlinestore.annotation.RequireAdmin;
import com.example.onlinestore.annotation.ValidateParams;
import com.example.onlinestore.dto.CreateProductRequest;
import com.example.onlinestore.dto.ErrorResponse;
import com.example.onlinestore.dto.ProductPageRequest;
import com.example.onlinestore.model.Product;
import com.example.onlinestore.service.ProductService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductService productService;

    @Autowired
    private MessageSource messageSource;

    /**
     * Creates a new product from the provided request.
     *
     * <p>This endpoint processes a validated product creation request and returns an HTTP OK response with
     * the created product if successful. If the input is invalid or an illegal argument is encountered, it returns
     * a 400 Bad Request response. For unexpected errors, it returns a 500 Internal Server Error with an internationalized
     * error message. Access to this operation is restricted to users with admin privileges.</p>
     *
     * @param request the validated product creation request details
     * @return a ResponseEntity containing the created product or an error response
     */
    @PostMapping
    @RequireAdmin
    @ValidateParams
    public ResponseEntity<?> createProduct(@RequestBody @Valid CreateProductRequest request) {
        try {
            logger.debug("开始创建商品，请求参数：{}", request);
            Product product = productService.createProduct(request);
            logger.debug("商品创建成功：{}", product.getName());
            return ResponseEntity.ok(product);
        } catch (IllegalArgumentException e) {
            logger.warn("创建商品失败：{}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("创建商品失败：{}", e.getMessage(), e);
            String errorMessage = messageSource.getMessage(
                "error.system.internal", null, LocaleContextHolder.getLocale());
            return ResponseEntity.internalServerError().body(new ErrorResponse(errorMessage));
        }
    }

    /**
     * Retrieves a paginated list of products.
     *
     * <p>
     * Handles HTTP GET requests and uses pagination criteria from the provided request to query the product list.
     * Returns a successful response with the paginated results on a valid request, a 400 Bad Request for invalid input,
     * or a 500 Internal Server Error for unexpected issues.
     * </p>
     *
     * @param request the pagination and filtering criteria for querying products
     * @return a ResponseEntity containing the paginated product list or an error response
     */
    @GetMapping
    @ValidateParams
    public ResponseEntity<?> listProducts(@Valid ProductPageRequest request) {
        try {
            logger.debug("开始查询商品列表，请求参数：{}", request);
            return ResponseEntity.ok(productService.listProducts(request));
        } catch (IllegalArgumentException e) {
            logger.warn("查询商品列表失败：{}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("查询商品列表失败：{}", e.getMessage(), e);
            String errorMessage = messageSource.getMessage(
                "error.system.internal", null, LocaleContextHolder.getLocale());
            return ResponseEntity.internalServerError().body(new ErrorResponse(errorMessage));
        }
    }
} 