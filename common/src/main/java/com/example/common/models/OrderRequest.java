package com.example.common.models;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * מודל המייצג בקשת הזמנה
 * מכיל את הנתונים הנדרשים ליצירת הזמנה חדשה
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "בקשת הזמנה חדשה")
public class OrderRequest {
    
    /**
     * שם הלקוח
     */
    @Schema(description = "שם הלקוח", example = "דוד כהן", required = true)
    private String customerName;
    
    /**
     * רשימת הפריטים בהזמנה
     */
    @Schema(description = "רשימת הפריטים בהזמנה", required = true)
    private List<OrderItem> items;
    
    /**
     * מזהה ייחודי של הבקשה
     */
    @Schema(description = "מזהה ייחודי של הבקשה", example = "2025-01-13T10:00:00Z", required = true)
    private String requestId;
    
    /**
     * תאריך ושעה יצירת הבקשה
     */
    @Schema(description = "תאריך ושעה יצירת הבקשה", example = "2025-01-13T10:00:00", required = true)
    private LocalDateTime requestDateTime;
} 