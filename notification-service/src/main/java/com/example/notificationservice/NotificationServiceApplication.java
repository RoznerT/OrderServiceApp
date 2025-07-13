package com.example.notificationservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * מחלקה ראשית עבור שירות ההודעות
 * אחראית על אתחול האפליקציה והגדרת הקומפוננטים
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.example.notificationservice",
    "com.example.common",
    "com.example.utils"
})
@EntityScan(basePackages = {
    "com.example.common.models",
    "com.example.common.events"
})
@Slf4j
public class NotificationServiceApplication {
    
    /**
     * נקודת הכניסה לאפליקציה
     * מאתחלת את Spring Boot ומתחילה את השירות
     * @param args ארגומנטים מהשורת פקודה
     */
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
} 