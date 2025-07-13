package com.example.inventoryservice.strategy;

import com.example.common.models.OrderItem;
import com.example.common.utils.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * אסטרטגיה לבדיקת זמינות מוצרים רגילים
 * בודקת מלאי זמין במחסן
 */
@Component
@Slf4j
public class StandardInventoryStrategy implements InventoryCheckStrategy {
    
    private static final Map<String, Integer> PRODUCT_STOCK = new HashMap<>();
    
    static {
        PRODUCT_STOCK.put("P1001", 100);
        PRODUCT_STOCK.put("P1002", 0);
        PRODUCT_STOCK.put("P1003", 50);
        PRODUCT_STOCK.put("P1004", 25);
        PRODUCT_STOCK.put("P1005", 5);
        PRODUCT_STOCK.put("P1006", 0);
    }
    
    /**
     * בדיקת זמינות מוצר רגיל
     * בודקת שיש מלאי מספיק במחסן
     * @param item הפריט לבדיקה
     * @return true אם יש מלאי מספיק, false אחרת
     */
    @Override
    public boolean isAvailable(OrderItem item) {
        if (ValidationUtils.isNull(item) || ValidationUtils.isEmpty(item.getProductId()) || 
            ValidationUtils.isNull(item.getQuantity()) || !ValidationUtils.isPositive(item.getQuantity())) {
            log.error("Invalid item for standard inventory check: {}", item);
            return false;
        }
        
        Integer currentStock = PRODUCT_STOCK.get(item.getProductId());
        
        if (ValidationUtils.isNull(currentStock)) {
            log.warn("No stock information found for standard product: {}", item.getProductId());
            return false;
        }
        
        boolean isAvailable = currentStock >= item.getQuantity();
        
        if (isAvailable) {
            PRODUCT_STOCK.put(item.getProductId(), currentStock - item.getQuantity());
            log.info("Standard product {} available - Stock: {}, Requested: {}, Remaining: {}", 
                    item.getProductId(), currentStock, item.getQuantity(), PRODUCT_STOCK.get(item.getProductId()));
        } else {
            log.warn("Standard product {} not available - Stock: {}, Requested: {}", 
                    item.getProductId(), currentStock, item.getQuantity());
        }
        
        return isAvailable;
    }
    
    /**
     * שליפת סוג הקטגוריה
     * @return STANDARD
     */
    @Override
    public String getCategoryType() {
        return "STANDARD";
    }
} 