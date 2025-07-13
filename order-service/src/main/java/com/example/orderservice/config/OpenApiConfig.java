package com.example.orderservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * קונפיגורציה עבור OpenAPI 3.0
 * מגדיר את המידע הבסיסי על ה-API
 */
@Configuration
public class OpenApiConfig {

    /**
     * יצירת אובייקט OpenAPI עבור תיעוד ה-API
     * @return OpenAPI configuration
     */
    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Service API")
                        .version("1.0.0")
                        .description("שירות לניהול הזמנות במערכת מיקרו-שירותים")
                        .contact(new Contact()
                                .name("Order Service Team")
                                .url("https://github.com/example/order-service")
                                .email("support@example.com")
                        )
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("Development server"),
                        new Server()
                                .url("http://order-service:8081")
                                .description("Docker container")
                ));
    }
} 