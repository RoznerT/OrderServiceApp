package com.example.inventoryservice.service;

import com.example.common.events.InventoryCheckResultEvent;
import com.example.common.events.OrderCreatedEvent;
import com.example.common.models.OrderItem;
import com.example.common.utils.ValidationUtils;
import com.example.inventoryservice.strategy.InventoryCheckStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * שירות לבדיקת מלאי
 * מתאם בין האסטרטגיות השונות ומבצע בדיקת זמינות מוצרים
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final List<InventoryCheckStrategy> inventoryStrategies;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String INVENTORY_CHECK_RESULT_TOPIC = "inventory-check-result";

    /**
     * מאגר לאחסון אסטרטגיות לפי קטגוריה
     * נוצר דינמית בעת אתחול השירות
     */
    private Map<String, InventoryCheckStrategy> strategyMap;

    /**
     * אתחול מאגר האסטרטגיות
     * יוצר מיפוי בין קטגוריות לאסטרטגיות
     */
    public void initializeStrategies() {
        if (ValidationUtils.isNull(strategyMap)) {
            strategyMap = inventoryStrategies.stream()
                    .collect(Collectors.toMap(
                            InventoryCheckStrategy::getCategoryType,
                            Function.identity()
                    ));
            log.info("Initialized {} inventory strategies", strategyMap.size());
        }
    }

    /**
     * בדיקת זמינות עבור הזמנה
     * מבצעת בדיקה עבור כל פריט בהזמנה ומפרסמת תוצאות
     *
     * @param orderCreatedEvent אירוע יצירת הזמנה
     */
    public void checkInventory(OrderCreatedEvent orderCreatedEvent) {
        log.info("Inventory check started for order: {} - Customer: {}, Items: {}", 
                orderCreatedEvent.getOrderId(), orderCreatedEvent.getCustomerName(), 
                ValidationUtils.isNotEmpty(orderCreatedEvent.getItems()) ? orderCreatedEvent.getItems().size() : 0);

        if (ValidationUtils.isNull(orderCreatedEvent)) {
            log.error("Order created event cannot be null");
            return;
        }

        if (ValidationUtils.isEmpty(orderCreatedEvent.getOrderId())) {
            log.error("Order ID cannot be empty");
            return;
        }

        log.info("=== INITIALIZING INVENTORY STRATEGIES ===");
        initializeStrategies();
        log.info("Inventory strategies initialized successfully");
        List<String> unavailableItems = new ArrayList<>();
        boolean allItemsAvailable = true;
        String errorMessage = null;

        try {
            if (ValidationUtils.isEmpty(orderCreatedEvent.getItems())) {
                log.error("Order has no items to check");
                errorMessage = "Order has no items";
                allItemsAvailable = false;
            } else {
                log.info("=== CHECKING INDIVIDUAL ITEMS ===");
                for (OrderItem item : orderCreatedEvent.getItems()) {
                    log.info("Checking item - Product ID: {}, Quantity: {}, Category: {}",
                            item.getProductId(), item.getQuantity(), item.getCategory());

                    boolean itemAvailable = isItemAvailable(item);
                    log.info("Item availability result - Product ID: {}, Available: {}",
                            item.getProductId(), itemAvailable);

                    if (!itemAvailable) {
                        unavailableItems.add(item.getProductId());
                        allItemsAvailable = false;
                        log.warn("Item unavailable - Product ID: {}", item.getProductId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("=== ERROR DURING INVENTORY CHECK ===");
            log.error("Order ID: {}", orderCreatedEvent.getOrderId());
            log.error("Error Type: {}", e.getClass().getSimpleName());
            log.error("Error Message: {}", e.getMessage());
            log.error("Stack Trace:", e);
            errorMessage = "Error during inventory check: " + e.getMessage();
            allItemsAvailable = false;
        }

        log.info("=== INVENTORY CHECK COMPLETED ===");
        log.info("Order ID: {}", orderCreatedEvent.getOrderId());
        log.info("All Items Available: {}", allItemsAvailable);
        log.info("Unavailable Items Count: {}", unavailableItems.size());
        if (!unavailableItems.isEmpty()) {
            log.info("Unavailable Items: {}", unavailableItems);
        }
        if (ValidationUtils.hasText(errorMessage)) {
            log.error("Error Message: {}", errorMessage);
        }
        publishInventoryCheckResult(orderCreatedEvent, allItemsAvailable, unavailableItems, errorMessage);
    }

    /**
     * בדיקת זמינות פריט יחיד
     * בוחרת את האסטרטגיה המתאימה ומבצעת בדיקה
     *
     * @param item הפריט לבדיקה
     * @return true אם הפריט זמין, false אחרת
     */
    private boolean isItemAvailable(OrderItem item) {
        if (ValidationUtils.isNull(item)) {
            log.error("Item cannot be null");
            return false;
        }

        if (ValidationUtils.isEmpty(item.getProductId())) {
            log.error("Product ID cannot be empty");
            return false;
        }

        if (ValidationUtils.isNull(item.getCategory())) {
            log.error("Product category cannot be null for product: {}", item.getProductId());
            return false;
        }

        String categoryType = item.getCategory().name();
        InventoryCheckStrategy strategy = strategyMap.get(categoryType);

        if (ValidationUtils.isNull(strategy)) {
            log.error("No strategy found for category: {} for product: {}",
                    categoryType, item.getProductId());
            return false;
        }

        try {
            boolean isAvailable = strategy.isAvailable(item);
            log.info("Inventory check for product {} (category: {}): {}",
                    item.getProductId(), categoryType, isAvailable ? "AVAILABLE" : "UNAVAILABLE");
            return isAvailable;
        } catch (Exception e) {
            log.error("Error checking availability for product {} (category: {}): {}", item.getProductId(), categoryType, e.getMessage());
            return false;
        }
    }

    /**
     * פרסום תוצאות בדיקת מלאי
     * שולחת אירוע עם תוצאות הבדיקה לכל המעוניינים
     *
     * @param orderCreatedEvent האירוע המקורי
     * @param approved          האם ההזמנה אושרה
     * @param unavailableItems  רשימת מוצרים שאינם זמינים
     * @param errorMessage      הודעת שגיאה במידה וקיימת
     */
    private void publishInventoryCheckResult(OrderCreatedEvent orderCreatedEvent, boolean approved, List<String> unavailableItems, String errorMessage) {
        InventoryCheckResultEvent resultEvent = new InventoryCheckResultEvent(
                orderCreatedEvent.getOrderId(),
                approved,
                unavailableItems,
                errorMessage,
                LocalDateTime.now(),
                orderCreatedEvent.getCustomerName()
        );

        try {
            kafkaTemplate.send(INVENTORY_CHECK_RESULT_TOPIC, orderCreatedEvent.getOrderId(), resultEvent);
            log.info("Inventory check result published for order: {}. Approved: {}, Unavailable items: {}", orderCreatedEvent.getOrderId(), approved, unavailableItems.size());
        } catch (Exception e) {
            log.error("Error publishing inventory check result for order {}: {}", orderCreatedEvent.getOrderId(), e.getMessage());
        }
    }
} 