package com.example.common.models;

import com.example.common.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * מודל המייצג הזמנה במערכת
 * מכיל את כל הנתונים הנדרשים לניהול הזמנה
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "מודל הזמנה במערכת")
public class Order {
    
    /**
     * מזהה ייחודי של ההזמנה
     */
    @Schema(description = "מזהה ייחודי של ההזמנה", example = "550e8400-e29b-41d4-a716-446655440000")
    private String orderId;
    
    /**
     * שם הלקוח
     */
    @Schema(description = "שם הלקוח", example = "דוד כהן")
    private String customerName;
    
    /**
     * רשימת הפריטים בהזמנה
     */
    @Schema(description = "רשימת הפריטים בהזמנה")
    private List<OrderItem> items;
    
    /**
     * מזהה ייחודי של הבקשה המקורית
     */
    @Schema(description = "מזהה ייחודי של הבקשה המקורית", example = "2025-01-13T10:00:00Z")
    private String requestId;
    
    /**
     * תאריך ושעה יצירת הבקשה
     */
    @Schema(description = "תאריך ושעה יצירת הבקשה", example = "2025-01-13T10:00:00")
    private LocalDateTime requestDateTime;
    
    /**
     * סטטוס ההזמנה
     */
    @Schema(description = "סטטוס ההזמנה", example = "PENDING")
    private OrderStatus status;
    
    /**
     * תאריך ושעה יצירת ההזמנה
     */
    @Schema(description = "תאריך ושעה יצירת ההזמנה", example = "2025-01-13T10:00:00")
    private LocalDateTime createdAt;
    
    /**
     * תאריך ושעה עדכון אחרון
     */
    @Schema(description = "תאריך ושעה עדכון אחרון", example = "2025-01-13T10:00:00")
    private LocalDateTime lastUpdated;
} 