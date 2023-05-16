package com.rocky.config;

import com.rocky.external.client.PaymentClient;
import com.rocky.external.client.ProductClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class WebClientConfig {

    @Autowired
    private LoadBalancedExchangeFilterFunction filterFunction;


    @Bean
    public WebClient productWebClient() {
        return WebClient.builder()
                .baseUrl("http://product-service")
                .filter(filterFunction)
                .build();
    }

    @Bean
    public ProductClient productClient() {
        HttpServiceProxyFactory httpServiceProxyFactory
                = HttpServiceProxyFactory
                .builder(WebClientAdapter.forClient(productWebClient()))
                .build();
        return httpServiceProxyFactory.createClient(ProductClient.class);
    }



    @Bean
    public WebClient paymentWebClient() {
        return WebClient.builder()
                .baseUrl("http://payment-service")
                .filter(filterFunction)
                .build();
    }

    @Bean
    public PaymentClient PaymentClient() {
        HttpServiceProxyFactory httpServiceProxyFactory
                = HttpServiceProxyFactory
                .builder(WebClientAdapter.forClient(paymentWebClient()))
                .build();
        return httpServiceProxyFactory.createClient(PaymentClient.class);
    }




    @Bean
    @LoadBalanced
    public WebClient.Builder filterFunction() {
        return WebClient.builder();
    }

}


