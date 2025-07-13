package com.example.common.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * אירוע המתפרסם כאשר בדיקת מלאי הושלמה בשירות המלאי
 * מכיל את תוצאות הבדיקה עבור ההזמנה
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCheckResultEvent {
    
    /**
     * מזהה ייחודי של ההזמנה
     */
    private String orderId;
    
    /**
     * האם ההזמנה אושרה
     */
    private boolean approved;
    
    /**
     * רשימת מוצרים שאינם זמינים או שנכשלו בבדיקה
     */
    private List<String> unavailableItems;
    
    /**
     * הודעת שגיאה במידה וקיימת
     */
    private String errorMessage;
    
    /**
     * תאריך ושעה יצירת האירוע
     */
    private LocalDateTime eventDateTime;
    
    /**
     * שם הלקוח
     */
    private String customerName;
} 