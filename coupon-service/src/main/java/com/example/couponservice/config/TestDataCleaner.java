package com.example.couponservice.config;

import com.example.couponservice.adapter.out.persistence.jpa.CouponJpaRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Ensures test runs start with a clean coupon table when the test profile is active.
 */
//@Component
//@Profile("test")
//public class TestDataCleaner implements ApplicationRunner {
//
//    private final CouponJpaRepository couponJpaRepository;
//
//    public TestDataCleaner(CouponJpaRepository couponJpaRepository) {
//        this.couponJpaRepository = couponJpaRepository;
//    }
//
//    @Override
//    public void run(ApplicationArguments args) {
//        couponJpaRepository.deleteAll();
//    }
//}
