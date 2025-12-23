package org.oldvabik.orderservice.kafka;

import lombok.extern.slf4j.Slf4j;
import org.oldvabik.orderservice.event.CreateOrderEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.create-order-topic:order-created-topic}")
    private String orderTopic;

    public OrderEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOrderCreatedEvent(CreateOrderEvent event) {
        log.info("[OrderEventProducer] Sending CREATE_ORDER event: orderId={}, userId={}, amount={}", 
            event.getOrderId(), event.getUserId(), event.getTotalAmount());

        kafkaTemplate.send(orderTopic, event.getOrderId(), event);

        log.debug("[OrderEventProducer] CREATE_ORDER event sent successfully");
    }
}

