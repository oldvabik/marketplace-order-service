package org.oldvabik.orderservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oldvabik.orderservice.event.CreatePaymentEvent;
import org.oldvabik.orderservice.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {
    private final OrderService orderService;

    @KafkaListener(topics = "${app.kafka.create-payment-topic:payment-created-topic}", 
                   groupId = "${app.kafka.order-group-id:order-service-group}")
    public void consumeCreatePaymentEvent(CreatePaymentEvent event) {
        log.info("[PaymentEventConsumer] Received CREATE_PAYMENT event: paymentId={}, orderId={}, status={}", 
            event.getPaymentId(), event.getOrderId(), event.getStatus());

        try {
            orderService.updateOrderStatusByPayment(event.getOrderId(), event.getStatus());
            log.info("[PaymentEventConsumer] Order status updated for orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("[PaymentEventConsumer] Error processing CREATE_PAYMENT event: {}", e.getMessage(), e);
        }
    }
}

