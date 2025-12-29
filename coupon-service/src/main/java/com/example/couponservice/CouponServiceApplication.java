package com.example.couponservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;


@SpringBootApplication
public class CouponServiceApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(CouponServiceApplication.class)
                .properties(
                        "spring.config.name=coupon_application"
                ).run(args);
    }
}
