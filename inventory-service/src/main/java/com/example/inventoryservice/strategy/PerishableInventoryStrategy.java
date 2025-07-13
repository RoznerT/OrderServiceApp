package com.example.inventoryservice.strategy;

import com.example.common.models.OrderItem;
import com.example.common.utils.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * אסטרטגיה לבדיקת זמינות מוצרים מתכלים
 * בודקת תאריך תוקף של המוצר
 */
@Component
@Slf4j
public class PerishableInventoryStrategy implements InventoryCheckStrategy {
    
    private static final Map<String, LocalDateTime> PRODUCT_EXPIRATION_DATES = new HashMap<>();
    
    static {
        PRODUCT_EXPIRATION_DATES.put("P1001", LocalDateTime.now().plusDays(30));
        PRODUCT_EXPIRATION_DATES.put("P1002", LocalDateTime.now().minusDays(1));
        PRODUCT_EXPIRATION_DATES.put("P1003", LocalDateTime.now().plusDays(7));
        PRODUCT_EXPIRATION_DATES.put("P1004", LocalDateTime.now().plusDays(15));
        PRODUCT_EXPIRATION_DATES.put("P1005", LocalDateTime.now().minusDays(5));
    }
    
    /**
     * בדיקת זמינות מוצר מתכלה
     * בודקת שתאריך התוקף לא עבר
     * @param item הפריט לבדיקה
     * @return true אם המוצר בתוקף, false אחרת
     */
    @Override
    public boolean isAvailable(OrderItem item) {
        if (ValidationUtils.isNull(item) || ValidationUtils.isEmpty(item.getProductId()) || 
            ValidationUtils.isNull(item.getQuantity()) || !ValidationUtils.isPositive(item.getQuantity())) {
            log.error("Invalid item for perishable inventory check: {}", item);
            return false;
        }
        
        LocalDateTime expirationDate = PRODUCT_EXPIRATION_DATES.get(item.getProductId());

        if (ValidationUtils.isNull(expirationDate)) {
            log.warn("No expiration date found for perishable product: {}", item.getProductId());
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        boolean isValid = expirationDate.isAfter(now);
        
        if (isValid) {
            log.info("Perishable product {} available - Expires: {}, Quantity: {}", 
                    item.getProductId(), expirationDate, item.getQuantity());
        } else {
            log.warn("Perishable product {} expired - Expired: {}, Current: {}", 
                    item.getProductId(), expirationDate, now);
        }
        
        return isValid;
    }
    
    /**
     * שליפת סוג הקטגוריה
     * @return PERISHABLE
     */
    @Override
    public String getCategoryType() {
        return "PERISHABLE";
    }
} 