package com.example.ordersagaconsumer.adapter.out.webclient.support;

import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

public abstract class ServiceClientSupport {

    private static final Duration RETRY_DELAY = Duration.ofMillis(200);

    private final WebClient webClient;
    private final String serviceName;
    private final Duration timeout;
    private final int retryCount;

    protected ServiceClientSupport(
            WebClient.Builder builder,
            String baseUrl,
            String serviceName,
            Duration timeout,
            int retryCount
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.serviceName = serviceName;
        this.timeout = timeout;
        this.retryCount = retryCount;
    }

    protected boolean post(String path, Object request) {
        try {
            Mono<Void> call = webClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .then();

            if (timeout != null) {
                call = call.timeout(timeout);
            }

            if (retryCount > 0) {
                call = call.retryWhen(Retry.fixedDelay(retryCount, RETRY_DELAY));
            }

            call.block();
            return true;
        } catch (Exception ex) {
            System.out.println("### " + serviceName + " service call failed ### : path=" + path
                    + " message=" + ex.getMessage());
            return false;
        }
    }
}
