package com.example.inventoryservice.strategy;

import com.example.common.models.OrderItem;

/**
 * Strategy interface לבדיקת זמינות מוצרים
 * מגדירה חוזה לבדיקת מוצרים לפי קטגוריות שונות
 */
public interface InventoryCheckStrategy {
    
    /**
     * בדיקת זמינות פריט
     * כל אסטרטגיה מממשת בדיקה שונה בהתאם לקטגוריה
     * @param item הפריט לבדיקה
     * @return true אם הפריט זמין, false אחרת
     */
    boolean isAvailable(OrderItem item);
    
    /**
     * שליפת סוג הקטגוריה שהאסטרטגיה מטפלת בה
     * @return סוג הקטגוריה
     */
    String getCategoryType();
} 