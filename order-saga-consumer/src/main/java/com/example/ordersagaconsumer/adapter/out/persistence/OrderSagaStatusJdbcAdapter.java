package com.example.ordersagaconsumer.adapter.out.persistence;

import com.example.ordersagaconsumer.application.port.out.UpdateOrderSagaStatusPort;
import com.example.ordersagaconsumer.domain.model.status.OrderSagaStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrderSagaStatusJdbcAdapter implements UpdateOrderSagaStatusPort {

    private final JdbcTemplate jdbcTemplate;

    public OrderSagaStatusJdbcAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void updateStatus(String orderId, OrderSagaStatus status) {
        jdbcTemplate.update(
                "update order_saga set status = ? where order_id = ?",
                status.name(),
                orderId
        );
    }
}
