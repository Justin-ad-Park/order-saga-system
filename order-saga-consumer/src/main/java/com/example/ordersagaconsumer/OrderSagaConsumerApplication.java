package com.example.ordersagaconsumer;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.kafka.annotation.EnableKafka;
import java.util.Arrays;

@EnableKafka
@SpringBootApplication
public class OrderSagaConsumerApplication {

    public static void main(String[] args) {
        String systemProfile = System.getProperty("spring.profiles.active");
        boolean hasProfileArg = Arrays.stream(args)
                .anyMatch(arg -> arg.startsWith("--spring.profiles.active="));
        String envProfile = System.getenv("SPRING_PROFILES_ACTIVE");

        if ((systemProfile == null || systemProfile.isBlank())
                && !hasProfileArg
                && (envProfile == null || envProfile.isBlank())) {
            System.setProperty("spring.profiles.active", "k8s-local");
        }
        new SpringApplicationBuilder(OrderSagaConsumerApplication.class)
                .properties(
                        "spring.config.name=OSC_application"
                ).run(args);
    }
}
