package com.example.ordersagaconsumer;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class OrderSagaConsumerApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(OrderSagaConsumerApplication.class)
                .properties(
                        "spring.config.name=OSC_application"
                ).run(args);
    }
}
