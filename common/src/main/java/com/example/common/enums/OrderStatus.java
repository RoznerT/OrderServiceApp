package com.example.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum המגדיר את סטטוסי ההזמנה השונים
 * מייצג את מחזור החיים של הזמנה במערכת
 */
public enum OrderStatus {
    /**
     * ההזמנה נוצרה ומחכה לעיבוד
     */
    PENDING,
    
    /**
     * ההזמנה בעיבוד - נבדק מלאי
     */
    PROCESSING,
    
    /**
     * ההזמנה אושרה - כל הפריטים זמינים
     */
    APPROVED,
    
    /**
     * ההזמנה נדחתה - חסרים פריטים או שגיאה
     */
    REJECTED;
    
    /**
     * מחזיר את הערך כפי שהוא צריך להופיע ב-JSON
     */
    @JsonValue
    public String getValue() {
        return this.name();
    }
} 