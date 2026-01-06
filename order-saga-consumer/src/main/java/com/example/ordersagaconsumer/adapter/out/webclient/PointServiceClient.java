package com.example.ordersagaconsumer.adapter.out.webclient;

import com.example.ordersagaconsumer.adapter.out.webclient.dto.CompensatePointRequest;
import com.example.ordersagaconsumer.adapter.out.webclient.dto.ConfirmPointRequest;
import com.example.ordersagaconsumer.adapter.out.webclient.support.ServiceClientSupport;
import com.example.ordersagaconsumer.application.port.out.PointServicePort;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PointServiceClient extends ServiceClientSupport implements PointServicePort {

    public PointServiceClient(
            WebClient.Builder builder,
            @Value("${external.point.base-url}") String baseUrl,
            @Value("${external.client.timeout-seconds:3}") long timeoutSeconds,
            @Value("${external.client.retry-count:0}") int retryCount
    ) {
        super(builder, baseUrl, "Point", Duration.ofSeconds(timeoutSeconds), retryCount);
    }

    @Override
    public boolean confirm(String pointNumber, String orderId) {
        ConfirmPointRequest request = new ConfirmPointRequest(pointNumber, orderId);
        return post("/api/v1/points/confirm", request);
    }

    @Override
    public boolean compensate(String pointNumber, String orderId) {
        CompensatePointRequest request = new CompensatePointRequest(pointNumber, orderId);
        return post("/api/v1/points/compensate", request);
    }
}
