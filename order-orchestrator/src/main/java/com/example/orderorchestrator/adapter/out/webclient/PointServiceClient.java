package com.example.orderorchestrator.adapter.out.webclient;

import com.example.orderorchestrator.adapter.out.webclient.dto.ReservePointRequest;
import com.example.orderorchestrator.adapter.out.webclient.dto.ReservePointResponse;
import com.example.orderorchestrator.adapter.out.webclient.dto.WebApiResponse;
import com.example.orderorchestrator.application.port.out.ReservePointPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class PointServiceClient implements ReservePointPort {

    private final WebClient webClient;

    public PointServiceClient(
            WebClient.Builder builder,
            @Value("${external.point.base-url}") String baseUrl
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public Mono<Void> reservePoint(String pointNumber, String orderId) {
        ReservePointRequest request = new ReservePointRequest(pointNumber, orderId);

        return webClient.post()
                .uri("/api/v1/points/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<WebApiResponse<ReservePointResponse>>() {})
                .flatMap(response -> {
                    ReservePointResponse data = response.getData();
                    if (data == null) {
                        return Mono.error(new IllegalStateException("Reserve point response missing data"));
                    }
                    return Mono.just(data);
                })
                .then();
    }
}
