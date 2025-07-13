package com.example.orderservice.controller;

import com.example.common.enums.OrderStatus;
import com.example.common.models.Order;
import com.example.common.models.OrderRequest;
import com.example.orderservice.service.OrderService;
import com.example.orderservice.service.OpenApiYamlGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.util.Map;

/**
 * REST Controller לניהול הזמנות
 * מספק endpoints ליצירה, שליפה ועדכון של הזמנות
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order Management", description = "API לניהול הזמנות")
public class OrderController {
    
    private final OrderService orderService;
    private final OpenApiYamlGenerator openApiYamlGenerator;
    
    /**
     * יצירת הזמנה חדשה
     * @param orderRequest פרטי ההזמנה
     * @return ההזמנה החדשה
     */
    @PostMapping
    @Operation(summary = "יצירת הזמנה חדשה", description = "מקבל פרטי הזמנה ויוצר הזמנה חדשה במערכת")
    public Mono<ResponseEntity<Order>> createOrder(@RequestBody OrderRequest orderRequest) {
        log.info("Creating order for customer: {}, Request ID: {}, Items: {}", 
                orderRequest.getCustomerName(), orderRequest.getRequestId(), 
                orderRequest.getItems() != null ? orderRequest.getItems().size() : 0);
        
        return orderService.createOrder(orderRequest)
            .map(order -> {
                log.info("Order created successfully: {}", order.getOrderId());
                return ResponseEntity.status(HttpStatus.CREATED).body(order);
            })
            .onErrorResume(error -> {
                log.error("Error creating order: {}", error.getMessage());
                return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
            });
    }
    
    /**
     * שליפת הזמנה לפי מזהה
     * @param orderId מזהה ההזמנה
     * @return פרטי ההזמנה
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "שליפת הזמנה", description = "מחזיר פרטי הזמנה לפי מזהה")
    public Mono<ResponseEntity<Order>> getOrder(@Parameter(description = "מזהה ההזמנה") @PathVariable String orderId) {
        return orderService.getOrder(orderId)
            .map(order -> {
                log.info("Order retrieved successfully: {}", orderId);
                return ResponseEntity.ok(order);
            })
            .onErrorResume(error -> {
                log.error("Error retrieving order {}: {}", orderId, error.getMessage());
                return Mono.just(ResponseEntity.notFound().build());
            });
    }
    
    /**
     * שליפת סטטוס הזמנה
     * @param orderId מזהה ההזמנה
     * @return סטטוס ההזמנה
     */
    @GetMapping("/{orderId}/status")
    @Operation(summary = "שליפת סטטוס הזמנה", description = "מחזיר את הסטטוס הנוכחי של ההזמנה")
    public Mono<ResponseEntity<Map<String, Object>>> getOrderStatus(@Parameter(description = "מזהה ההזמנה") @PathVariable String orderId) {
        return orderService.getOrderStatus(orderId)
            .map(status -> {
                log.info("Order status retrieved successfully: {}", orderId);
                return ResponseEntity.ok(status);
            })
            .onErrorResume(error -> {
                log.error("Error retrieving order status {}: {}", orderId, error.getMessage());
                return Mono.just(ResponseEntity.notFound().build());
            });
    }
    
    /**
     * עדכון סטטוס הזמנה
     * @param orderId מזהה ההזמנה
     * @param status הסטטוס החדש
     * @return ההזמנה המעודכנת
     */
    @PutMapping("/{orderId}/status")
    @Operation(summary = "עדכון סטטוס הזמנה", description = "מעדכן את הסטטוס של הזמנה קיימת")
    public Mono<ResponseEntity<Order>> updateOrderStatus(@Parameter(description = "מזהה ההזמנה") @PathVariable String orderId, 
                                                        @Parameter(description = "הסטטוס החדש") @RequestParam OrderStatus status) {
        return orderService.updateOrderStatus(orderId, status)
            .map(order -> {
                log.info("Order status updated successfully: {} -> {}", orderId, status);
                return ResponseEntity.ok(order);
            })
            .onErrorResume(error -> {
                log.error("Error updating order status {}: {}", orderId, error.getMessage());
                return Mono.just(ResponseEntity.notFound().build());
            });
    }
    
    /**
     * בדיקת מצב המטמון המקומי ומצב Redis
     * @return מידע על מצב המטמון
     */
    @GetMapping("/cache/status")
    @Operation(summary = "בדיקת מצב המטמון", description = "מחזיר מידע על מצב המטמון המקומי וחיבור Redis")
    public Mono<ResponseEntity<Map<String, Object>>> getCacheStatus() {
        return Mono.fromCallable(() -> {
            Map<String, Object> cacheStatus = orderService.getCacheStatus();
            log.info("Cache status retrieved successfully");
            return ResponseEntity.ok(cacheStatus);
        })
        .onErrorResume(error -> {
            log.error("Error retrieving cache status: {}", error.getMessage());
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        });
    }
    
    /**
     * בדיקת זמינות השירות
     * @return מידע על זמינות השירות
     */
    @GetMapping("/health")
    @Operation(summary = "בדיקת זמינות השירות", description = "מחזיר מידע על מצב השירות")
    public Mono<ResponseEntity<Map<String, Object>>> healthCheck() {
        log.debug("Health check requested");
        
        return Mono.fromCallable(() -> {
            Map<String, Object> cacheStatus = orderService.getCacheStatus();
            Map<String, Object> health = Map.of(
                "status", "UP",
                "timestamp", java.time.LocalDateTime.now(),
                "service", "order-service",
                "redis", cacheStatus.get("redisAvailable"),
                "fallbackMode", cacheStatus.get("fallbackMode")
            );
            
            return ResponseEntity.ok(health);
        })
        .onErrorResume(error -> {
            log.error("Error in health check: {}", error.getMessage());
            Map<String, Object> health = Map.of(
                "status", "DOWN",
                "timestamp", java.time.LocalDateTime.now(),
                "service", "order-service",
                "error", error.getMessage()
            );
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health));
        });
    }

    /**
     * יצירת קובץ OpenAPI YAML
     * @return קובץ YAML עם תיעוד המלא של ה-API
     */
    @GetMapping(value = "/api-docs/openapi.yaml", produces = "application/x-yaml")
    @Operation(summary = "קובץ OpenAPI YAML", description = "מחזיר קובץ YAML עם תיעוד המלא של ה-API")
    public Mono<ResponseEntity<Resource>> getOpenApiYaml() {
        return Mono.fromCallable(() -> {
            try {
                String filePath = "order-service-openapi.yaml";
                openApiYamlGenerator.generateOpenApiYaml(filePath);
                
                Resource resource = new FileSystemResource(filePath);
                log.info("OpenAPI YAML file served successfully");
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("application/x-yaml"))
                        .body(resource);
            } catch (Exception e) {
                log.error("Error generating OpenAPI YAML: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        });
    }
} 