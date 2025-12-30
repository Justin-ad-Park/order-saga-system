package com.example.orderorchestrator;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class OrderOrchestratorApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(OrderOrchestratorApplication.class)
                .properties(
                        "spring.config.name=orderOS_application"
                ).run(args);
    }
}
