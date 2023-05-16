package com.rocky.service;

import com.rocky.model.PaymentRequest;
import com.rocky.model.PaymentResponse;

public interface PaymentService {
    long doPayment(PaymentRequest paymentRequest);

    PaymentResponse getPaymentDetailsByOrderId(String orderId);
}
