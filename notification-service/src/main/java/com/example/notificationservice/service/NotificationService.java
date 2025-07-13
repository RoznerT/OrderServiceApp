package com.example.notificationservice.service;

import com.example.common.events.InventoryCheckResultEvent;
import com.example.common.models.Order;
import com.example.common.utils.ValidationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * שירות ההודעות
 * מטפל בהודעות לגבי תוצאות בדיקת מלאי ומציג אותן בקונסול
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String ORDER_KEY_PREFIX = "order:";

    /**
     * עיבוד תוצאות בדיקת מלאי
     * מאחזר פרטי הזמנה מ-Redis ומציג הודעה מתאימה
     *
     * @param inventoryCheckResult תוצאות בדיקת המלאי
     */
    public void processInventoryCheckResult(InventoryCheckResultEvent inventoryCheckResult) {
        log.info("Processing inventory result for order {} - Customer: {}, Approved: {}", 
                inventoryCheckResult.getOrderId(), inventoryCheckResult.getCustomerName(), 
                inventoryCheckResult.isApproved());

        if (ValidationUtils.isNull(inventoryCheckResult)) {
            log.error("Inventory check result cannot be null");
            return;
        }

        if (ValidationUtils.isEmpty(inventoryCheckResult.getOrderId())) {
            log.error("Order ID cannot be empty in inventory check result");
            return;
        }

        retrieveOrderFromRedis(inventoryCheckResult.getOrderId())
                .subscribe(
                        order -> {
                            log.info("Order retrieved successfully: {}", order.getOrderId());
                            displayNotification(order, inventoryCheckResult);
                        },
                        error -> {
                            log.error("Order retrieval failed for {}: {}", 
                                    inventoryCheckResult.getOrderId(), error.getMessage());
                            displayNotificationWithoutOrder(inventoryCheckResult);
                        }
                );
    }

    /**
     * אחזור הזמנה מ-Redis
     * מחזיר את פרטי ההזמנה המלאים
     *
     * @param orderId מזהה ההזמנה
     * @return פרטי ההזמנה
     */
    private Mono<Order> retrieveOrderFromRedis(String orderId) {
        return redisTemplate.opsForValue()
                .get(ORDER_KEY_PREFIX + orderId)
                .flatMap(this::convertToOrder)
                .doOnSuccess(order -> {
                    if (ValidationUtils.isNotNull(order)) {
                        log.info("Order retrieved from Redis: {}", orderId);
                    } else {
                        log.warn("Order not found in Redis: {}", orderId);
                    }
                })
                .doOnError(error -> log.error("Error retrieving order {} from Redis: {}",
                        orderId, error.getMessage()));
    }

    /**
     * המרת אוביקט מ-Redis ל-Order
     * ממיר LinkedHashMap או Object ל-Order object
     *
     * @param obj האוביקט מ-Redis
     * @return Order object
     */
    private Mono<Order> convertToOrder(Object obj) {
        return Mono.fromCallable(() -> {
            if (obj == null) {
                throw new IllegalArgumentException("Object from Redis is null");
            }

            if (obj instanceof Order) {
                return (Order) obj;
            } else if (obj instanceof Map) {
                try {
                    log.debug("Converting Map to Order: {}", obj);
                    return objectMapper.convertValue(obj, Order.class);
                } catch (Exception e) {
                    log.error("Error converting Map to Order: {}", e.getMessage());
                    throw new IllegalArgumentException("Failed to convert Map to Order: " + e.getMessage(), e);
                }
            } else {
                log.error("Unexpected object type from Redis: {} - {}", obj.getClass().getName(), obj);
                throw new IllegalArgumentException("Cannot convert object of type " + obj.getClass().getName() + " to Order");
            }
        });
    }

    /**
     * הצגת הודעה עם פרטי ההזמנה
     * מציגה הודעה מפורטת על תוצאות בדיקת המלאי
     *
     * @param order                פרטי ההזמנה
     * @param inventoryCheckResult תוצאות בדיקת המלאי
     */
    private void displayNotification(Order order, InventoryCheckResultEvent inventoryCheckResult) {
        String status = inventoryCheckResult.isApproved() ? "APPROVED" : "REJECTED";
        String customerName = ValidationUtils.hasText(inventoryCheckResult.getCustomerName()) ?
                inventoryCheckResult.getCustomerName() : (ValidationUtils.isNotNull(order) ? order.getCustomerName() : "Unknown");

        System.out.println("=".repeat(60));
        System.out.println("ORDER NOTIFICATION");
        System.out.println("=".repeat(60));
        System.out.println("Order ID: " + inventoryCheckResult.getOrderId());
        System.out.println("Customer: " + customerName);
        System.out.println("Status: " + status);
        System.out.println("Timestamp: " + LocalDateTime.now());

        if (ValidationUtils.isNotNull(order)) {
            System.out.println("Items Count: " + (ValidationUtils.isNotEmpty(order.getItems()) ? order.getItems().size() : 0));
            System.out.println("Request ID: " + order.getRequestId());
            System.out.println("Created At: " + order.getCreatedAt());
        }

        if (!inventoryCheckResult.isApproved()) {
            System.out.println("REJECTION DETAILS:");

            if (ValidationUtils.hasText(inventoryCheckResult.getErrorMessage())) {
                System.out.println("Error: " + inventoryCheckResult.getErrorMessage());
            }

            if (ValidationUtils.isNotEmpty(inventoryCheckResult.getUnavailableItems())) {
                System.out.println("Unavailable Items:");
                inventoryCheckResult.getUnavailableItems().forEach(item ->
                        System.out.println("  - " + item));
            }
        } else {
            System.out.println("All items are available and the order has been approved!");
        }

        System.out.println("=".repeat(60) + "\n");
        log.info("Notification displayed for order: {} - Status: {}",
                inventoryCheckResult.getOrderId(), status);
    }

    /**
     * הצגת הודעה ללא פרטי ההזמנה
     * מציגה הודעה כשלא ניתן לאחזר את פרטי ההזמנה מ-Redis
     *
     * @param inventoryCheckResult תוצאות בדיקת המלאי
     */
    private void displayNotificationWithoutOrder(InventoryCheckResultEvent inventoryCheckResult) {
        String status = inventoryCheckResult.isApproved() ? "APPROVED" : "REJECTED";

        System.out.println("=".repeat(60));
        System.out.println("ORDER NOTIFICATION (Limited Info)");
        System.out.println("=".repeat(60));
        System.out.println("Order ID: " + inventoryCheckResult.getOrderId());
        System.out.println("Customer: " + (ValidationUtils.hasText(inventoryCheckResult.getCustomerName()) ?
                inventoryCheckResult.getCustomerName() : "Unknown"));
        System.out.println("Status: " + status);
        System.out.println("Timestamp: " + LocalDateTime.now());
        System.out.println("Note: Could not retrieve full order details from Redis");

        if (!inventoryCheckResult.isApproved()) {
            System.out.println("REJECTION DETAILS:");

            if (ValidationUtils.hasText(inventoryCheckResult.getErrorMessage())) {
                System.out.println("Error: " + inventoryCheckResult.getErrorMessage());
            }

            if (ValidationUtils.isNotEmpty(inventoryCheckResult.getUnavailableItems())) {
                System.out.println("Unavailable Items:");
                inventoryCheckResult.getUnavailableItems().forEach(item -> System.out.println("  - " + item));
            }
        } else {
            System.out.println("All items are available and the order has been approved!");
        }

        System.out.println("=".repeat(60) + "\n");
        log.warn("Notification displayed for order: {} without full order details - Status: {}", inventoryCheckResult.getOrderId(), status);
    }
} 