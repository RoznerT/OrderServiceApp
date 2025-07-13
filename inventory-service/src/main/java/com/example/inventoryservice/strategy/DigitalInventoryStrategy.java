package com.example.inventoryservice.strategy;

import com.example.common.models.OrderItem;
import com.example.common.utils.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * אסטרטגיה לבדיקת זמינות מוצרים דיגיטליים
 * מוצרים דיגיטליים תמיד זמינים
 */
@Component
@Slf4j
public class DigitalInventoryStrategy implements InventoryCheckStrategy {
    
    /**
     * בדיקת זמינות מוצר דיגיטלי
     * מוצרים דיגיטליים תמיד זמינים
     * @param item הפריט לבדיקה
     * @return תמיד true (זמין)
     */
    @Override
    public boolean isAvailable(OrderItem item) {
        if (ValidationUtils.isNull(item) || ValidationUtils.isEmpty(item.getProductId()) || 
            ValidationUtils.isNull(item.getQuantity()) || !ValidationUtils.isPositive(item.getQuantity())) {
            log.error("Invalid item for digital inventory check: {}", item);
            return false;
        }
        
        log.info("Digital product {} is always available. Quantity: {}", item.getProductId(), item.getQuantity());
        return true;
    }
    
    /**
     * שליפת סוג הקטגוריה
     * @return DIGITAL
     */
    @Override
    public String getCategoryType() {
        return "DIGITAL";
    }
} 