package com.example.common.events;

import com.example.common.models.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * אירוע המתפרסם כאשר הזמנה נוצרה בשירות ההזמנות
 * מכיל את כל המידע הנדרש עבור בדיקת מלאי
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    
    /**
     * מזהה ייחודי של ההזמנה
     */
    private String orderId;
    
    /**
     * שם הלקוח
     */
    private String customerName;
    
    /**
     * רשימת הפריטים בהזמנה
     */
    private List<OrderItem> items;
    
    /**
     * מזהה ייחודי של הבקשה
     */
    private String requestId;
    
    /**
     * תאריך ושעה יצירת הבקשה
     */
    private LocalDateTime requestDateTime;
    
    /**
     * תאריך ושעה יצירת האירוע
     */
    private LocalDateTime eventDateTime;
} 