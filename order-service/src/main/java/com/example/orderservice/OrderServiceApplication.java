package com.example.orderservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * מחלקה ראשית עבור שירות ההזמנות
 * אחראית על אתחול האפליקציה והגדרת הקומפוננטים
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
    "com.example.orderservice",
    "com.example.common",
    "com.example.utils"
})
@EntityScan(basePackages = {
    "com.example.common.models",
    "com.example.common.events"
})
@Slf4j
public class OrderServiceApplication {
    
    /**
     * נקודת הכניסה לאפליקציה
     * מאתחלת את Spring Boot ומתחילה את השירות
     * @param args ארגומנטים מהשורת פקודה
     */
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
} 