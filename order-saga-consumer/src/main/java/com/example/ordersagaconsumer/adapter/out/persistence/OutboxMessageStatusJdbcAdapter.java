package com.example.ordersagaconsumer.adapter.out.persistence;

import com.example.ordersagaconsumer.application.port.out.UpdateOutboxMessagePort;
import com.example.ordersagaconsumer.domain.model.status.MSAStatus;
import com.example.ordersagaconsumer.domain.model.status.OrderSagaStatus;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OutboxMessageStatusJdbcAdapter implements UpdateOutboxMessagePort {

    private final JdbcTemplate jdbcTemplate;

    public OutboxMessageStatusJdbcAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void updateCouponStatus(String orderId, MSAStatus status) {
        jdbcTemplate.update(
                "update outbox_message set coupon_status = ?, updated_at = ? where order_id = ?",
                status.name(),
                Timestamp.valueOf(LocalDateTime.now()),
                orderId
        );
    }

    @Override
    public void updatePointStatus(String orderId, MSAStatus status) {
        jdbcTemplate.update(
                "update outbox_message set point_status = ?, updated_at = ? where order_id = ?",
                status.name(),
                Timestamp.valueOf(LocalDateTime.now()),
                orderId
        );
    }

    @Override
    public void updateCompletedStatus(String orderId) {
        jdbcTemplate.update(
                "update outbox_message set saga_status = ?, order_status = ?, updated_at = ? where order_id = ?",
                OrderSagaStatus.Completed.name(),
                MSAStatus.Completed.name(),
                Timestamp.valueOf(LocalDateTime.now()),
                orderId
        );
    }

    @Override
    public void updateCompensatedStatus(String orderId) {
        jdbcTemplate.update(
                "update outbox_message set saga_status = ?, order_status = ?, updated_at = ? where order_id = ?",
                OrderSagaStatus.Compensated.name(),
                MSAStatus.Compensated.name(),
                Timestamp.valueOf(LocalDateTime.now()),
                orderId
        );
    }

    @Override
    public void updateSagaStatus(String orderId, OrderSagaStatus status) {
        jdbcTemplate.update(
                "update outbox_message set saga_status = ?, updated_at = ? where order_id = ?",
                status.name(),
                Timestamp.valueOf(LocalDateTime.now()),
                orderId
        );
    }
}
