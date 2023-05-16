package com.rocky.service;

import com.rocky.model.ProductRequest;
import com.rocky.model.ProductResponse;

public interface ProductService {
    long addProduct(ProductRequest productRequest);

    ProductResponse getProductById(long productId);

    void reduceQuantity(long productId, long quantity);
}
