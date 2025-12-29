package com.example.pointservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;


@SpringBootApplication
public class PointServiceApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(PointServiceApplication.class)
                .properties(
                        "spring.config.name=point_application"
                ).run(args);
    }
}
