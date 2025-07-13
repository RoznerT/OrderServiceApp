package com.example.orderservice.listener;

import com.example.common.events.InventoryCheckResultEvent;
import com.example.common.enums.OrderStatus;
import com.example.common.utils.ValidationUtils;
import com.example.orderservice.service.OrderService;
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
 * מאזין לאירועי תוצאות בדיקת מלאי בשירות ההזמנות
 * מעדכן את סטטוס ההזמנה ב-Redis בהתאם לתוצאות הבדיקה
 * כולל מנגנון Dead Letter Queue לטיפול בכשלים
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryResultListener {

    private final OrderService orderService;

    /**
     * מאזין לתוצאות בדיקת מלאי
     * מקבל אירוע ומעדכן את סטטוס ההזמנה בהתאם
     * כולל מנגנון retry עם DLQ
     *
     * @param inventoryCheckResult תוצאות בדיקת המלאי
     * @param key                  מזהה ההודעה
     * @param topic                שם הטופיק
     * @param partition            מספר הpartition
     */
    @KafkaListener(topics = "inventory-check-result", groupId = "order-service-group")
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            include = {Exception.class},
            dltTopicSuffix = "-dlq"
    )
    public void handleInventoryCheckResult(@Payload InventoryCheckResultEvent inventoryCheckResult,
                                           @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                           @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                           @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {

        log.info("=== KAFKA EVENT RECEIVED - INVENTORY CHECK RESULT ===");
        log.info("Topic: {}", topic);
        log.info("Partition: {}", partition);
        log.info("Key: {}", key);
        log.info("Order ID: {}", inventoryCheckResult.getOrderId());
        log.info("Customer: {}", inventoryCheckResult.getCustomerName());
        log.info("Approved: {}", inventoryCheckResult.isApproved());
        log.info("Unavailable Items Count: {}", 
                ValidationUtils.isNotEmpty(inventoryCheckResult.getUnavailableItems()) ? 
                inventoryCheckResult.getUnavailableItems().size() : 0);
        log.info("Event DateTime: {}", inventoryCheckResult.getEventDateTime());

        if (ValidationUtils.isNull(inventoryCheckResult)) {
            log.error("Received null inventory check result event");
            throw new IllegalArgumentException("Inventory check result cannot be null");
        }

        if (ValidationUtils.isEmpty(inventoryCheckResult.getOrderId())) {
            log.error("Received inventory check result with empty order ID");
            throw new IllegalArgumentException("Order ID cannot be empty");
        }

        if (ValidationUtils.isEmpty(inventoryCheckResult.getCustomerName())) {
            log.error("Received inventory check result with empty customer name for order: {}",
                    inventoryCheckResult.getOrderId());
            throw new IllegalArgumentException("Customer name cannot be empty");
        }

        log.info("=== VALIDATION PASSED - PROCESSING INVENTORY RESULT ===");
        log.info("Processing inventory result for order: {}, customer: {}, approved: {}",
                inventoryCheckResult.getOrderId(),
                inventoryCheckResult.getCustomerName(),
                inventoryCheckResult.isApproved());

        if (!inventoryCheckResult.isApproved() &&
                ValidationUtils.isNotEmpty(inventoryCheckResult.getUnavailableItems())) {
            log.warn("Order {} rejected with {} unavailable items: {}", 
                    inventoryCheckResult.getOrderId(), 
                    inventoryCheckResult.getUnavailableItems().size(),
                    inventoryCheckResult.getUnavailableItems());
        }

        if (ValidationUtils.hasText(inventoryCheckResult.getErrorMessage())) {
            log.warn("Inventory check error for order {}: {}", 
                    inventoryCheckResult.getOrderId(), inventoryCheckResult.getErrorMessage());
        }

        try {
            log.info("=== UPDATING ORDER STATUS ===");
            
            // קביעת הסטטוס החדש בהתאם לתוצאות הבדיקה
            OrderStatus newStatus = inventoryCheckResult.isApproved() ? 
                    OrderStatus.APPROVED : OrderStatus.REJECTED;
            
            log.info("Updating order {} status to: {}", 
                    inventoryCheckResult.getOrderId(), newStatus);
            
            // עדכון הסטטוס ב-Redis ובמטמון המקומי
            orderService.updateOrderStatus(inventoryCheckResult.getOrderId(), newStatus)
                    .subscribe(
                            updatedOrder -> {
                                log.info("=== ORDER STATUS UPDATED SUCCESSFULLY ===");
                                log.info("Order ID: {}", updatedOrder.getOrderId());
                                log.info("New Status: {}", updatedOrder.getStatus());
                                log.info("Last Updated: {}", updatedOrder.getLastUpdated());
                                log.info("Successfully updated order status for order: {}", 
                                        inventoryCheckResult.getOrderId());
                            },
                            error -> {
                                log.error("=== ORDER STATUS UPDATE FAILED ===");
                                log.error("Order ID: {}", inventoryCheckResult.getOrderId());
                                log.error("Error Type: {}", error.getClass().getSimpleName());
                                log.error("Error Message: {}", error.getMessage());
                                log.error("Stack Trace:", error);
                                throw new RuntimeException("Failed to update order status", error);
                            }
                    );
            
            log.info("=== INVENTORY RESULT PROCESSING COMPLETED SUCCESSFULLY ===");
            log.info("Successfully processed inventory result for order: {}", 
                    inventoryCheckResult.getOrderId());
                    
        } catch (Exception e) {
            log.error("=== INVENTORY RESULT PROCESSING FAILED ===");
            log.error("Order ID: {}", inventoryCheckResult.getOrderId());
            log.error("Error Type: {}", e.getClass().getSimpleName());
            log.error("Error Message: {}", e.getMessage());
            log.error("Stack Trace:", e);
            throw new RuntimeException("Failed to process inventory check result", e);
        }
    }
} 