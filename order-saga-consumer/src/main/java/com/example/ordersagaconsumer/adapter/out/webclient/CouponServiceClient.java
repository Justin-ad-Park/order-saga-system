package com.example.ordersagaconsumer.adapter.out.webclient;

import com.example.ordersagaconsumer.adapter.out.webclient.dto.CompensateCouponRequest;
import com.example.ordersagaconsumer.adapter.out.webclient.dto.ConfirmCouponRequest;
import com.example.ordersagaconsumer.application.port.out.CouponServicePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class CouponServiceClient implements CouponServicePort {

    private final WebClient webClient;

    public CouponServiceClient(
            WebClient.Builder builder,
            @Value("${external.coupon.base-url}") String baseUrl
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public boolean confirm(String couponNumber, String orderId) {
        ConfirmCouponRequest request = new ConfirmCouponRequest(couponNumber, orderId);
        return post("/api/v1/coupons/confirm", request);
    }

    @Override
    public boolean compensate(String couponNumber, String orderId) {
        CompensateCouponRequest request = new CompensateCouponRequest(couponNumber, orderId);
        return post("/api/v1/coupons/compensate", request);
    }

    private boolean post(String path, Object request) {
        try {
            webClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception ex) {
            System.out.println("### Coupon service call failed ### : path=" + path + " message=" + ex.getMessage());
            return false;
        }
    }
}
