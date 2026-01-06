package com.example.ordersagaconsumer.adapter.out.webclient;

import com.example.ordersagaconsumer.adapter.out.webclient.dto.CompensatePointRequest;
import com.example.ordersagaconsumer.adapter.out.webclient.dto.ConfirmPointRequest;
import com.example.ordersagaconsumer.application.port.out.PointServicePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PointServiceClient implements PointServicePort {

    private final WebClient webClient;

    public PointServiceClient(
            WebClient.Builder builder,
            @Value("${external.point.base-url}") String baseUrl
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
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
            System.out.println("### Point service call failed ### : path=" + path + " message=" + ex.getMessage());
            return false;
        }
    }
}
