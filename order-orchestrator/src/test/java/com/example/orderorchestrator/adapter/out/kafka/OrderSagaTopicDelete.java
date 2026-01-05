package com.example.orderorchestrator.adapter.out.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = AbstractOrderSagaTopicDelete.KafkaTestConfig.class,
        properties = {
                "spring.config.name=orderOS_application",
                "spring.profiles.active=test"
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class OrderSagaTopicDelete extends AbstractOrderSagaTopicDelete {
    @Value("${order.saga.events.topic:order-saga-events-test}")
    private String topic;

    @Override
    protected String topic() {
        return topic;
    }
}
