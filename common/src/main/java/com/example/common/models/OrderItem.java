package com.example.common.models;

import com.example.common.enums.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * מודל המייצג פריט בהזמנה
 * מכיל את כל הנתונים הנדרשים לפריט יחיד
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "פריט בהזמנה")
public class OrderItem {
    
    /**
     * מזהה המוצר
     */
    @Schema(description = "מזהה המוצר", example = "P1001", required = true)
    private String productId;
    
    /**
     * כמות המוצר
     */
    @Schema(description = "כמות המוצר", example = "2", required = true)
    private int quantity;
    
    /**
     * קטגוריית המוצר
     */
    @Schema(description = "קטגוריית המוצר", example = "STANDARD", required = true)
    private Category category;
} 