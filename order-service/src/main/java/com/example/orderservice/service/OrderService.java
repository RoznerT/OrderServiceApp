package com.example.orderservice.service;

import com.example.common.enums.OrderStatus;
import com.example.common.events.OrderCreatedEvent;
import com.example.common.events.InventoryCheckResultEvent;
import com.example.common.models.Order;
import com.example.common.models.OrderRequest;
import com.example.common.utils.ValidationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * שירות לניהול הזמנות
 * מספק פונקציונליות ליצירה, שליפה ועדכון של הזמנות
 * כולל מנגנון fallback עבור כשלים ב-Redis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String ORDER_CREATED_TOPIC = "order-created";
    private static final String ORDER_CREATED_DLQ_TOPIC = "order-created-dlq";
    private static final String ORDER_KEY_PREFIX = "order:";

    private final ConcurrentHashMap<String, Order> localCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> cacheTimestamps = new ConcurrentHashMap<>();
    private final AtomicBoolean redisAvailable = new AtomicBoolean(true);
    private final AtomicBoolean fallbackMode = new AtomicBoolean(false);
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final int MAX_CACHE_SIZE = 1000;
    
    /**
     * יצירת הזמנה חדשה
     * מבצעת validation, שומרת ב-Redis (עם fallback למטמון מקומי) ומפרסמת אירוע ל-Kafka
     * @param orderRequest פרטי ההזמנה
     * @return ההזמנה החדשה
     */
    public Mono<Order> createOrder(OrderRequest orderRequest) {
        log.info("Creating order for customer: {}, Request ID: {}, Items: {}", 
                orderRequest.getCustomerName(), orderRequest.getRequestId(), 
                ValidationUtils.isNotEmpty(orderRequest.getItems()) ? orderRequest.getItems().size() : 0);
        
        return Mono.fromCallable(() -> {
            Order validatedOrder = validateAndCreateOrder(orderRequest);
            log.info("Order validation completed successfully - Order ID: {}", validatedOrder.getOrderId());
            return validatedOrder;
        })
        .flatMap(this::saveOrder)
        .flatMap(this::publishOrderCreatedEvent)
        .doOnSuccess(order -> {
            log.info("Order creation completed successfully: {} - Customer: {}, Status: {}", 
                    order.getOrderId(), order.getCustomerName(), order.getStatus());
        })
        .doOnError(error -> {
            log.error("Order creation failed for customer {} (Request ID: {}): {}", 
                    orderRequest.getCustomerName(), orderRequest.getRequestId(), error.getMessage(), error);
        });
    }
    
    /**
     * שליפת הזמנה לפי מזהה
     * מחזירה את פרטי ההזמנה מ-Redis או מהמטמון המקומי
     * @param orderId מזהה ההזמנה
     * @return פרטי ההזמנה
     */
    public Mono<Order> getOrder(String orderId) {
        if (ValidationUtils.isEmpty(orderId)) {
            return Mono.error(new IllegalArgumentException("Order ID cannot be null or empty"));
        }
        

        
        if (!redisAvailable.get()) {
            return getOrderFromLocalCache(orderId);
        }
        
        return redisTemplate.opsForValue()
            .get(ORDER_KEY_PREFIX + orderId)
            .timeout(Duration.ofSeconds(5))
            .flatMap(this::convertToOrder)
            .switchIfEmpty(Mono.defer(() -> getOrderFromLocalCache(orderId)))
            .doOnSuccess(order -> log.info("Order retrieved successfully from Redis: {}", orderId))
            .onErrorResume(error -> {
                log.error("Error retrieving order from Redis {}: {}", orderId, error.getMessage());
                handleRedisError(error);
                return getOrderFromLocalCache(orderId);
            });
    }
    
    /**
     * שליפת הזמנה מהמטמון המקומי
     * @param orderId מזהה ההזמנה
     * @return פרטי ההזמנה
     */
    private Mono<Order> getOrderFromLocalCache(String orderId) {
        Order cachedOrder = localCache.get(orderId);
        
        if (cachedOrder != null) {
            LocalDateTime timestamp = cacheTimestamps.get(orderId);
            if (timestamp != null && timestamp.plus(CACHE_TTL).isAfter(LocalDateTime.now())) {
                log.info("Order retrieved successfully from local cache: {}", orderId);
                return Mono.just(cachedOrder);
            } else {
                // מחיקת רשומה שפגה
                localCache.remove(orderId);
                cacheTimestamps.remove(orderId);
                log.warn("Order {} expired in local cache", orderId);
            }
        }
        
        log.warn("Order not found in local cache: {}", orderId);
        return Mono.error(new RuntimeException("Order not found: " + orderId));
    }
    
    /**
     * המרת אוביקט מ-Redis ל-Order
     * מבצעת deserialization בטוח
     * @param obj אוביקט מ-Redis
     * @return Mono של Order
     */
    private Mono<Order> convertToOrder(Object obj) {
        return Mono.fromCallable(() -> {
            try {
                if (obj instanceof Order) {
                    return (Order) obj;
                } else if (obj instanceof Map) {
                    return objectMapper.convertValue(obj, Order.class);
                } else {
                    return objectMapper.readValue(obj.toString(), Order.class);
                }
            } catch (Exception e) {
                log.error("Error converting object to Order: {}", e.getMessage());
                throw new RuntimeException("Failed to convert object to Order", e);
            }
        });
    }
    
    /**
     * שליפת סטטוס הזמנה
     * מחזירה מידע על סטטוס ההזמנה
     * @param orderId מזהה ההזמנה
     * @return מידע על סטטוס ההזמנה
     */
    public Mono<Map<String, Object>> getOrderStatus(String orderId) {
        if (ValidationUtils.isEmpty(orderId)) {
            return Mono.error(new IllegalArgumentException("Order ID cannot be null or empty"));
        }
        

        return getOrder(orderId)
            .map(order -> {
                Map<String, Object> result = new HashMap<>();
                result.put("orderId", order.getOrderId());
                result.put("status", order.getStatus());
                result.put("customerName", order.getCustomerName());
                result.put("createdAt", order.getCreatedAt());
                result.put("source", redisAvailable.get() ? "Redis" : "Local Cache");
                return result;
            })
            .doOnSuccess(status -> log.info("Order status retrieved successfully: {}", orderId))
            .doOnError(error -> log.error("Error retrieving order status {}: {}", orderId, error.getMessage()));
    }
    
    /**
     * עדכון סטטוס הזמנה
     * מעדכנת את הסטטוס של הזמנה קיימת
     * @param orderId מזהה ההזמנה
     * @param status הסטטוס החדש
     * @return ההזמנה המעודכנת
     */
    public Mono<Order> updateOrderStatus(String orderId, OrderStatus status) {
        if (ValidationUtils.isEmpty(orderId)) {
            return Mono.error(new IllegalArgumentException("Order ID cannot be null or empty"));
        }
        

        return getOrder(orderId)
            .flatMap(order -> {
                order.setStatus(status);
                order.setLastUpdated(LocalDateTime.now());
                return saveOrder(order);
            })
            .doOnSuccess(order -> log.info("Order status updated successfully: {} -> {}", orderId, status))
            .doOnError(error -> log.error("Error updating order status {}: {}", orderId, error.getMessage()));
    }
    
    /**
     * אימות ויצירת הזמנה
     * מבצעת validation מקיף על הנתונים
     * @param orderRequest בקשת ההזמנה
     * @return הזמנה מאומתת
     */
    private Order validateAndCreateOrder(OrderRequest orderRequest) {
        if (ValidationUtils.isNull(orderRequest)) {
            throw new IllegalArgumentException("Order request cannot be null");
        }
        
        if (ValidationUtils.isEmpty(orderRequest.getCustomerName())) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        
        if (ValidationUtils.isEmpty(orderRequest.getItems()) || orderRequest.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order items cannot be null or empty");
        }
        
        orderRequest.getItems().forEach(item -> {
            if (ValidationUtils.isEmpty(item.getProductId())) {
                throw new IllegalArgumentException("Product ID cannot be null or empty");
            }
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
        });
        
        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString());
        order.setCustomerName(orderRequest.getCustomerName());
        order.setItems(orderRequest.getItems());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        
        return order;
    }
    
    /**
     * שמירת הזמנה ב-Redis עם fallback למטמון מקומי
     * מבצעת שמירה עם TTL
     * @param order ההזמנה לשמירה
     * @return Mono של ההזמנה
     */
    private Mono<Order> saveOrder(Order order) {
        String key = ORDER_KEY_PREFIX + order.getOrderId();
        saveToLocalCache(order);
        
        if (!redisAvailable.get()) {
            log.info("Redis unavailable - Order saved to local cache only: {}", order.getOrderId());
            return Mono.just(order);
        }
        
        return redisTemplate.opsForValue()
            .set(key, order, Duration.ofDays(7))
            .timeout(Duration.ofSeconds(5))
            .map(success -> {
                if (success) {
                    log.info("Order saved successfully to Redis: {}", order.getOrderId());
                    return order;
                } else {
                    throw new RuntimeException("Failed to save order to Redis");
                }
            })
            .onErrorResume(error -> {
                log.error("Error saving order to Redis: {}", error.getMessage());
                handleRedisError(error);
                log.info("Order saved to local cache as fallback: {}", order.getOrderId());
                return Mono.just(order);
            });
    }
    
    /**
     * שמירה במטמון המקומי
     * @param order ההזמנה לשמירה
     */
    private void saveToLocalCache(Order order) {
        // ניקוי מטמון אם הגיע לגודל מקסימלי
        if (localCache.size() >= MAX_CACHE_SIZE) {
            cleanupExpiredEntries();
        }
        
        localCache.put(order.getOrderId(), order);
        cacheTimestamps.put(order.getOrderId(), LocalDateTime.now());
        log.debug("Order saved to local cache: {}", order.getOrderId());
    }
    
    /**
     * ניקוי רשומות שפגו מהמטמון המקומי
     */
    private void cleanupExpiredEntries() {
        LocalDateTime now = LocalDateTime.now();
        cacheTimestamps.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().plus(CACHE_TTL).isBefore(now);
            if (expired) {
                localCache.remove(entry.getKey());
                log.debug("Removed expired entry from local cache: {}", entry.getKey());
            }
            return expired;
        });
    }
    
    /**
     * טיפול בשגיאות Redis
     * @param error השגיאה
     */
    private void handleRedisError(Throwable error) {
        if (error.getMessage().contains("Redis") || 
            error.getMessage().contains("Connection") ||
            error.getMessage().contains("timeout") ||
            error instanceof java.util.concurrent.TimeoutException) {
            
            log.warn("Redis connection issue detected, switching to fallback mode");
            redisAvailable.set(false);
            fallbackMode.set(true);
        }
    }
    
    /**
     * בדיקה תקופתית של זמינות Redis וסנכרון
     * רץ כל 30 שניות כדי לבדוק אם Redis חזר לפעילות
     */
    @Scheduled(fixedRate = 30000)
    public void checkRedisAvailability() {
        if (!redisAvailable.get()) {
            log.debug("Checking Redis availability...");
            
            redisTemplate.opsForValue()
                .get("health-check")
                .timeout(Duration.ofSeconds(2))
                .doOnSuccess(result -> {
                    log.info("Redis is back online! Switching back from fallback mode");
                    redisAvailable.set(true);
                    fallbackMode.set(false);
                    syncLocalCacheToRedis();
                })
                .doOnError(error -> log.debug("Redis still unavailable: {}", error.getMessage()))
                .subscribe();
        }
    }
    
    /**
     * סנכרון המטמון המקומי חזרה ל-Redis
     */
    private void syncLocalCacheToRedis() {
        if (localCache.isEmpty()) {
            return;
        }
        
        log.info("Syncing local cache to Redis. Cache size: {}", localCache.size());
        localCache.entrySet().forEach(entry -> {
            String orderId = entry.getKey();
            Order order = entry.getValue();
            LocalDateTime timestamp = cacheTimestamps.get(orderId);
            
            if (timestamp != null && timestamp.plus(CACHE_TTL).isAfter(LocalDateTime.now())) {
                String key = ORDER_KEY_PREFIX + orderId;
                redisTemplate.opsForValue()
                    .set(key, order, Duration.ofDays(7))
                    .timeout(Duration.ofSeconds(5))
                    .doOnSuccess(success -> {
                        if (success) {
                            log.info("Order synced to Redis: {}", orderId);
                        }
                    })
                    .doOnError(error -> log.error("Error syncing order to Redis: {}", orderId))
                    .subscribe();
            }
        });
    }
    
    /**
     * קבלת מידע על מצב המטמון
     * @return מידע על המטמון המקומי
     */
    public Map<String, Object> getCacheStatus() {
        return Map.of(
            "redisAvailable", redisAvailable.get(),
            "fallbackMode", fallbackMode.get(),
            "localCacheSize", localCache.size(),
            "maxCacheSize", MAX_CACHE_SIZE,
            "cacheTtlMinutes", CACHE_TTL.toMinutes()
        );
    }
    
    /**
     * פרסום אירוע יצירת הזמנה ל-Kafka
     * מפרסמת אירוע עם מנגנון DLQ
     * @param order ההזמנה
     * @return Mono של ההזמנה
     */
    private Mono<Order> publishOrderCreatedEvent(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getOrderId(),
            order.getCustomerName(),
            order.getItems(),
            UUID.randomUUID().toString(),
            order.getCreatedAt(),
            LocalDateTime.now()
        );
        
        return Mono.fromFuture(() -> {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(ORDER_CREATED_TOPIC, order.getOrderId(), event);
            
            return future.handle((result, throwable) -> {
                if (throwable != null) {
                    log.error("Failed to publish order created event to Kafka: {}", throwable.getMessage());
                    try {
                        CompletableFuture<SendResult<String, Object>> dlqFuture = kafkaTemplate.send(ORDER_CREATED_DLQ_TOPIC, order.getOrderId(), event);
                        dlqFuture.whenComplete((dlqResult, dlqError) -> {
                            if (dlqError != null) {
                                log.error("Failed to send to DLQ: {}", dlqError.getMessage());
                            } else {
                                log.info("Order event sent to DLQ successfully: {}", order.getOrderId());
                            }
                        });
                    } catch (Exception dlqException) {
                        log.error("Error sending to DLQ: {}", dlqException.getMessage());
                    }
                    
                    log.warn("Order created but event failed to publish, continuing with graceful");
                    return order;
                } else {
                    log.info("Order created event published successfully: {}", order.getOrderId());
                    return order;
                }
            });
        });
    }
} 