package com.example.ordersagaconsumer.adapter.out.webclient;

import com.example.ordersagaconsumer.adapter.out.webclient.dto.CompensateCouponRequest;
import com.example.ordersagaconsumer.adapter.out.webclient.dto.ConfirmCouponRequest;
import com.example.ordersagaconsumer.adapter.out.webclient.support.ServiceClientSupport;
import com.example.ordersagaconsumer.application.port.out.CouponServicePort;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class CouponServiceClient extends ServiceClientSupport implements CouponServicePort {

    public CouponServiceClient(
            WebClient.Builder builder,
            @Value("${external.coupon.base-url}") String baseUrl,
            @Value("${external.client.timeout-seconds:3}") long timeoutSeconds,
            @Value("${external.client.retry-count:0}") int retryCount
    ) {
        super(builder, baseUrl, "Coupon", Duration.ofSeconds(timeoutSeconds), retryCount);
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
}
