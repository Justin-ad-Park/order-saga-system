package com.example.orderorchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Profile;


@SpringBootApplication
public class OrderOrchestratorApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(OrderOrchestratorApplication.class)
                .properties(
                        // "spring.profiles.active=test",
                        "spring.config.name=orderOS_application"
                ).run(args);
    }
}
