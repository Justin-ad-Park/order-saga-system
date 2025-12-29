package com.example.pointservice.adapter.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PointJpaRepository extends JpaRepository<PointJpaEntity, String> {
}
