package com.example.notificationservice.listener;

import com.example.common.events.InventoryCheckResultEvent;
import com.example.common.utils.ValidationUtils;
import com.example.notificationservice.service.NotificationService;
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
 * מאזין לאירועי Kafka עבור שירות ההודעות
 * מטפל בתוצאות בדיקת מלאי ומציג הודעות בקונסול
 * כולל מנגנון Dead Letter Queue לטיפול בכשלים
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryResultListener {

    private final NotificationService notificationService;

    /**
     * מאזין לתוצאות בדיקת מלאי
     * מקבל אירוע ומפעיל הצגת הודעה בקונסול
     * כולל מנגנון retry עם DLQ
     *
     * @param inventoryCheckResult תוצאות בדיקת המלאי
     * @param key                  מזהה ההודעה
     * @param topic                שם הטופיק
     * @param partition            מספר הpartition
     */
    @KafkaListener(topics = "inventory-check-result", groupId = "notification-service-group")
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

        log.info("Kafka event received for order {} - Customer: {}, Approved: {}, Topic: {}", 
                inventoryCheckResult.getOrderId(), inventoryCheckResult.getCustomerName(), 
                inventoryCheckResult.isApproved(), topic);

        if (ValidationUtils.isNull(inventoryCheckResult)) {
            log.error("Received null inventory check result event");
            throw new IllegalArgumentException("Inventory check result cannot be null");
        }

        if (ValidationUtils.isEmpty(inventoryCheckResult.getOrderId())) {
            log.error("Received inventory check result with empty order ID");
            throw new IllegalArgumentException("Order ID cannot be empty");
        }

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
            notificationService.processInventoryCheckResult(inventoryCheckResult);
            log.info("Successfully processed inventory check result for order: {}", inventoryCheckResult.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process inventory check result for order {}: {}", 
                    inventoryCheckResult.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("Failed to process inventory check result", e);
        }
    }
}