package com.example.couponservice.adapter.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponJpaRepository extends JpaRepository<CouponJpaEntity, String> {
}
