package com.example.inventoryservice.listener;

import com.example.common.events.OrderCreatedEvent;
import com.example.common.utils.ValidationUtils;
import com.example.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * מאזין לאירועי Kafka עבור שירות המלאי
 * מטפל באירועי יצירת הזמנות ומפעיל בדיקת מלאי
 * כולל מנגנון Dead Letter Queue לטיפול בכשלים
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final InventoryService inventoryService;

    /**
     * מאזין לאירועי יצירת הזמנות
     * מקבל אירוע ומפעיל בדיקת מלאי
     * כולל מנגנון retry עם DLQ
     *
     * @param orderCreatedEvent האירוע של יצירת הזמנה
     * @param key               מזהה ההודעה
     * @param topic             שם הטופיק
     * @param partition         מספר הpartition
     */
    @KafkaListener(topics = "order-created", groupId = "inventory-service-group")
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            include = {Exception.class},
            dltTopicSuffix = "-dlq"
    )
    public void handleOrderCreatedEvent(@Payload OrderCreatedEvent orderCreatedEvent,
                                        @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {

        log.info("=== KAFKA EVENT RECEIVED - ORDER CREATED ===");
        log.info("Topic: {}", topic);
        log.info("Partition: {}", partition);
        log.info("Key: {}", key);
        log.info("Order ID: {}", orderCreatedEvent.getOrderId());
        log.info("Customer: {}", orderCreatedEvent.getCustomerName());
        log.info("Items Count: {}", ValidationUtils.isNotEmpty(orderCreatedEvent.getItems()) ? orderCreatedEvent.getItems().size() : 0);
        log.info("Request ID: {}", orderCreatedEvent.getRequestId());
        log.info("Event DateTime: {}", orderCreatedEvent.getEventDateTime());

        if (ValidationUtils.isNull(orderCreatedEvent)) {
            log.error("Received null order created event");
            throw new IllegalArgumentException("Order created event cannot be null");
        }

        if (ValidationUtils.isEmpty(orderCreatedEvent.getOrderId())) {
            log.error("Received order created event with empty order ID");
            throw new IllegalArgumentException("Order ID cannot be empty");
        }

        if (ValidationUtils.isEmpty(orderCreatedEvent.getCustomerName())) {
            log.error("Received order created event with empty customer name for order: {}",
                    orderCreatedEvent.getOrderId());
            throw new IllegalArgumentException("Customer name cannot be empty");
        }

        if (ValidationUtils.isEmpty(orderCreatedEvent.getItems())) {
            log.error("Received order created event with empty items for order: {}",
                    orderCreatedEvent.getOrderId());
            throw new IllegalArgumentException("Order items cannot be empty");
        }

        log.info("=== VALIDATION PASSED - PROCESSING ORDER ===");
        log.info("Processing order created event for order: {}, customer: {}, items count: {}",
                orderCreatedEvent.getOrderId(),
                orderCreatedEvent.getCustomerName(),
                orderCreatedEvent.getItems().size());

        if (ValidationUtils.isNotEmpty(orderCreatedEvent.getItems())) {
            orderCreatedEvent.getItems().forEach(item -> {
                log.info("Item - Product ID: {}, Quantity: {}, Category: {}",
                        item.getProductId(), item.getQuantity(), item.getCategory());
            });
        }

        try {
            log.info("=== CALLING INVENTORY SERVICE ===");
            inventoryService.checkInventory(orderCreatedEvent);
            log.info("=== ORDER PROCESSING COMPLETED SUCCESSFULLY ===");
            log.info("Successfully processed order created event for order: {}", orderCreatedEvent.getOrderId());
        } catch (Exception e) {
            log.error("=== ORDER PROCESSING FAILED ===");
            log.error("Order ID: {}", orderCreatedEvent.getOrderId());
            log.error("Error Type: {}", e.getClass().getSimpleName());
            log.error("Error Message: {}", e.getMessage());
            log.error("Stack Trace:", e);
            throw new RuntimeException("Failed to process order created event", e);
        }
    }
} 