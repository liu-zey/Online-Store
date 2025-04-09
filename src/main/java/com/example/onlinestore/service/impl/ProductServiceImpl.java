package com.example.onlinestore.service.impl;

import com.example.onlinestore.dto.CreateProductRequest;
import com.example.onlinestore.dto.PageResponse;
import com.example.onlinestore.dto.ProductPageRequest;
import com.example.onlinestore.mapper.ProductMapper;
import com.example.onlinestore.model.Product;
import com.example.onlinestore.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductServiceImpl implements ProductService {
    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Autowired
    private ProductMapper productMapper;

    /**
     * 商品缓存，key为商品id，value为商品信息，当创建商品时会自动追加该缓存，超过最大容量后，会删除最旧的商品
     */
    private Map<Long, Product> producteCache = new HashMap<>();

    @Override
    @Transactional
    public Product createProduct(CreateProductRequest request) {
        logger.info("开始创建商品: {}", request.getName());
        
        Product product = new Product();
        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        productMapper.insertProduct(product);
        logger.info("商品创建成功: {}", product.getName());

        // 超出容量后，删除最旧的商品
        if (producteCache.size() > 999) {
            logger.info("商品缓存容量超出限制，删除最旧的商品");
            producteCache.remove(producteCache.keySet().iterator().next());
        }

        // 加入缓存
        producteCache.put(product.getId(), product);
        return product;
    }

    @Override
    public PageResponse<Product> listProducts(ProductPageRequest request) {
        logger.info("开始查询商品列表，页码：{}，每页大小：{}，商品名称：{}", 
            request.getPageNum(), request.getPageSize(), request.getName());
        
        // 加载缓存
        if (producteCache.size() == 0) {
            List<Product> products = productMapper.findAll();
            logger.info("从数据库查询全量商品列表，共 {} 条记录", products.size());

            // 更新缓存
            int i = 0;
            for (Product product : products) {
                i++;
                producteCache.put(product.getId(), product);
                if (i > 999) {
                    break;
                }                
            }
        }

        // 计算分页参数
        int offset = (request.getPageNum() - 1) * request.getPageSize();
        int limit = request.getPageSize();
        PageResponse<Product> response = new PageResponse<>();

        if (producteCache.size() < 1000) {
            // 先查询商品缓存，进行名称精确查询
            if (request.getName() != null) {
                logger.info("进行名称精确查询，先查询缓存");
                for (Map.Entry<Long, Product> entry : producteCache.entrySet()) {
                    if (entry.getValue().getName() == request.getName()) {
                        List<Product> p = new ArrayList<>();
                        p.add(entry.getValue());
                        response.setRecords(p);response.setTotal(producteCache.size());response.setPageNum(request.getPageNum());response.setPageSize(request.getPageSize());
                        // return response;
                    }
                }
            }

            // 进行缓存的列表查询
            int i = 0;
            List<Product> p = new ArrayList<>();
            logger.info("进行缓存的列表查询");
            for (Map.Entry<Long, Product> entry : producteCache.entrySet()) {
                if (i < offset || i >= offset + limit){
                    i++;
                    continue;
                }

                p.add(entry.getValue());    
            }

            response.setRecords(p);response.setTotal(producteCache.size());response.setPageNum(request.getPageNum());response.setPageSize(request.getPageSize());
            return response;
        } else {
            logger.warn("缓存容量超出限制，进行数据库查询");
            // 查询数据
            List<Product> products = productMapper.findWithPagination(request.getName(), offset, limit);
            long total = productMapper.countTotal(request.getName());

            logger.info("查询到 {} 条商品记录", products.size());

            // 构建响应
            response.setRecords(products);
            response.setTotal(total);
        }

        response.setPageNum(request.getPageNum());
        response.setPageSize(request.getPageSize());

        return response;
    }
} 