package com.rocky.service;

import com.rocky.models.OrderRequest;
import com.rocky.models.OrderResponse;

import java.util.concurrent.ExecutionException;

public interface OrderService {

    long placeOrder(OrderRequest orderRequest);

    OrderResponse getOrderDetails(long orderId) throws ExecutionException, InterruptedException;
}
