package com.example.pointservice.config;

import com.example.pointservice.adapter.out.persistence.jpa.PointJpaRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Ensures test runs start with a clean point table when the test profile is active.
 */
//@Component
//@Profile("test")
//public class TestDataCleaner implements ApplicationRunner {
//
//    private final PointJpaRepository pointJpaRepository;
//
//    public TestDataCleaner(PointJpaRepository pointJpaRepository) {
//        this.pointJpaRepository = pointJpaRepository;
//    }
//
//    @Override
//    public void run(ApplicationArguments args) {
//        pointJpaRepository.deleteAll();
//    }
//}
