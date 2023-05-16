package com.rocky.service;

import com.rocky.exception.CustomException;
import com.rocky.external.client.PaymentClient;
import com.rocky.external.client.ProductClient;
import com.rocky.external.client.ProductClientApi;
import com.rocky.external.request.PaymentRequest;
import com.rocky.external.response.PaymentResponse;
import com.rocky.external.response.ProductResponse;
import com.rocky.models.OrderModel;
import com.rocky.models.OrderRequest;
import com.rocky.models.OrderResponse;
import com.rocky.repository.OrderRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@Log4j2
public class OrderServiceImpl implements OrderService{

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductClient productClient;
    @Autowired
    private PaymentClient paymentClient;
    @Autowired
    private WebClient.Builder webClientBuilder;
    @Autowired
    private ProductClientApi productClientApi;

    private final String baseProductUrl = "http://product-service";
    private final String basePaymentUrl = "http://payment-service";

    @Override
    public long placeOrder(OrderRequest orderRequest) {

        //Order Entity -> Save the data with Status Order Created
        //Product Service - Block Products (Reduce the Quantity)
        //Payment Service -> Payments -> Success-> COMPLETE, Else
        //CANCELLED

        log.info("Placing Order Request: {}", orderRequest);
        ProductResponse response=productClient.getProductById(orderRequest.getProductId()).getBody();
        System.out.println("========================"+response.toString());
        productClient.reduceQuantity(orderRequest.getProductId(), orderRequest.getQuantity());
        log.info("Creating Order with Status CREATED");
        OrderModel order = OrderModel.builder()
                .amount(orderRequest.getTotalAmount())
                .orderStatus("CREATED")
                .productId(orderRequest.getProductId())
                .orderDate(Instant.now())
                .quantity(orderRequest.getQuantity())
                .build();

        order = orderRepository.save(order);

        log.info("Calling Payment Service to complete the payment");
        PaymentRequest paymentRequest
                = PaymentRequest.builder()
                .orderId(order.getId())
                .paymentMode(orderRequest.getPaymentMode())
                .amount(orderRequest.getTotalAmount())
                .build();

        String orderStatus = null;
        try {
            paymentClient.doPayment(paymentRequest);
            log.info("Payment done Successfully. Changing the Oder status to PLACED");
            orderStatus = "PLACED";
        } catch (Exception e) {
            log.error("Error occurred in payment. Changing order status to PAYMENT_FAILED");
            orderStatus = "PAYMENT_FAILED";
        }

       // order.setOrderStatus(orderStatus);
        orderRepository.save(order);

        log.info("Order Places successfully with Order Id: {}", order.getId());
        return order.getId();
    }

    @Override
    public OrderResponse getOrderDetails(long orderId) throws ExecutionException, InterruptedException {
        log.info("Get order details for Order Id : {}", orderId);

        OrderModel order
                = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Order not found for the order Id:" + orderId,
                        "NOT_FOUND",
                        404));

        log.info("Invoking Product service to fetch the product for id: {}", order.getProductId());
//        Mono<ProductResponse> productResponse = webClientBuilder.baseUrl(baseProductUrl)
//                .build()
//                .get()
//                .uri("/product/"+ order.getProductId())
//                .accept(MediaType.APPLICATION_JSON)
//                .retrieve()
//                .bodyToMono(ProductResponse.class);
         CompletableFuture<Mono<ProductResponse>> productResponse = CompletableFuture.supplyAsync(() -> webClientBuilder.baseUrl(baseProductUrl)
                 .build()
                 .get()
                 .uri("/product/"+ order.getProductId())
                 .accept(MediaType.APPLICATION_JSON)
                 .retrieve()
                 .bodyToMono(ProductResponse.class));

        log.info("Getting payment information form the payment Service");
//        Mono<PaymentResponse> paymentResponse= webClientBuilder.baseUrl(basePaymentUrl)
//                .build()
//                .get()
//                .uri("/payment/order/"+order.getId())
//                .accept(MediaType.APPLICATION_JSON)
//                .retrieve()
//                .bodyToMono(PaymentResponse.class);

        CompletableFuture<Mono<PaymentResponse>> paymentResponse = CompletableFuture.supplyAsync(() -> webClientBuilder.baseUrl(basePaymentUrl)
                .build()
                .get()
                .uri("/payment/order/"+order.getId())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(PaymentResponse.class));

        CompletableFuture.allOf(productResponse, paymentResponse).join();

        Mono<ProductResponse> productResponseMono=productResponse.get();
        Mono<PaymentResponse> paymentResponseMono=paymentResponse.get();

        OrderResponse.ProductDetails productDetails
                = OrderResponse.ProductDetails
                .builder()
                .productName(productResponseMono.block().getProductName())
                .productId(productResponseMono.block().getProductId())
                .build();

        OrderResponse.PaymentDetails paymentDetails
                = OrderResponse.PaymentDetails
                .builder()
                .paymentId(paymentResponseMono.block().getPaymentId())
                .paymentStatus(paymentResponseMono.block().getStatus())
                .paymentDate(paymentResponseMono.block().getPaymentDate())
                .paymentMode(paymentResponseMono.block().getPaymentMode())
                .build();

        OrderResponse orderResponse
                = OrderResponse.builder()
                .orderId(order.getId())
                .orderStatus(order.getOrderStatus())
                .amount(order.getAmount())
                .orderDate(order.getOrderDate())
                .productDetails(productDetails)
                .paymentDetails(paymentDetails)
                .build();
        System.out.println(orderResponse.toString());
        return orderResponse;
    }
}
