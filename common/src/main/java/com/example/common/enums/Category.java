package com.example.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum המגדיר את סוגי הקטגוריות השונות עבור מוצרים
 * כל קטגוריה דורשת טיפול שונה בבדיקת זמינות המוצר
 */
public enum Category {
    /**
     * מוצרים דיגיטליים - תמיד זמינים
     */
    DIGITAL,
    
    /**
     * מוצרים מתכלים - דורש בדיקת תאריך תוקף
     */
    PERISHABLE,
    
    /**
     * מוצרים רגילים - דורש בדיקת מלאי
     */
    STANDARD;
    
    /**
     * מחזיר את הערך כפי שהוא צריך להופיע ב-JSON
     */
    @JsonValue
    public String getValue() {
        return this.name();
    }
    
    /**
     * Custom Deserializer עבור Category
     * מאפשר המרה מ-String ל-Category עם טיפול בשגיאות
     */
    @JsonCreator
    public static Category fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        
        try {
            return Category.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Category must be one of: DIGITAL, PERISHABLE, STANDARD. Received: " + value);
        }
    }
} 