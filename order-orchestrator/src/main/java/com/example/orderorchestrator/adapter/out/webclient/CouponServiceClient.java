package com.example.orderorchestrator.adapter.out.webclient;

import com.example.orderorchestrator.adapter.out.webclient.dto.ReserveCouponRequest;
import com.example.orderorchestrator.adapter.out.webclient.dto.ReserveCouponResponse;
import com.example.orderorchestrator.adapter.out.webclient.dto.WebApiResponse;
import com.example.orderorchestrator.application.port.out.ReserveCouponPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CouponServiceClient implements ReserveCouponPort {

    private final WebClient webClient;

    public CouponServiceClient(
            WebClient.Builder builder,
            @Value("${external.coupon.base-url}") String baseUrl
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public Mono<Void> reserveCoupon(String couponNumber, String orderId) {
        ReserveCouponRequest request = new ReserveCouponRequest(couponNumber, orderId);

        return webClient.post()
                .uri("/api/v1/coupons/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<WebApiResponse<ReserveCouponResponse>>() {})
                .flatMap(response -> {
                    ReserveCouponResponse data = response.getData();
                    if (data == null) {
                        return Mono.error(new IllegalStateException("Reserve coupon response missing data"));
                    }
                    return Mono.just(data);
                })
                .then();
    }
}
