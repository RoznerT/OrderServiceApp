package com.example.orderservice.service;

import com.example.common.enums.OrderStatus;
import com.example.common.models.Order;
import com.example.common.models.OrderRequest;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * שירות לייצור קובץ OpenAPI YAML מתיעוד הקוד
 * יוצר קובץ YAML מפורט עבור ה-API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenApiYamlGenerator {

    private final OpenAPI openAPI;
    private final YAMLMapper yamlMapper = new YAMLMapper();

    /**
     * יצירת קובץ OpenAPI YAML מלא עבור ה-API
     * @param filePath הנתיב לקובץ היעד
     * @throws IOException אם יש בעיה בכתיבת הקובץ
     */
    public void generateOpenApiYaml(String filePath) throws IOException {
        log.info("Generating OpenAPI YAML file: {}", filePath);
        openAPI.paths(createPaths());
        try (FileWriter writer = new FileWriter(filePath)) {
            yamlMapper.writeValue(writer, openAPI);
            log.info("OpenAPI YAML file generated successfully: {}", filePath);
        }
    }

    /**
     * יצירת כל ה-paths עבור ה-API
     * @return Paths object עם כל הנתיבים
     */
    private Paths createPaths() {
        Paths paths = new Paths();
        paths.addPathItem("/api/v1/orders", new PathItem().post(createOrderOperation()));
        
        paths.addPathItem("/api/v1/orders/{orderId}", new PathItem().get(getOrderOperation()));
        
        paths.addPathItem("/api/v1/orders/{orderId}/status", new PathItem().get(getOrderStatusOperation()));
        
        paths.addPathItem("/api/v1/orders/{orderId}/status", new PathItem().put(updateOrderStatusOperation()));
        
        paths.addPathItem("/api/v1/orders/cache/status", new PathItem().get(getCacheStatusOperation()));
        
        paths.addPathItem("/api/v1/orders/health", new PathItem().get(healthCheckOperation()));
        
        return paths;
    }

    /**
     * יצירת Operation עבור יצירת הזמנה
     */
    private Operation createOrderOperation() {
        return new Operation()
                .summary("יצירת הזמנה חדשה")
                .description("מקבל פרטי הזמנה ויוצר הזמנה חדשה במערכת")
                .operationId("createOrder")
                .requestBody(new RequestBody()
                        .required(true)
                        .content(new Content()
                                .addMediaType("application/json", new MediaType()
                                        .schema(new Schema<>().$ref("#/components/schemas/OrderRequest")))))
                .responses(new ApiResponses()
                        .addApiResponse("201", new ApiResponse()
                                .description("הזמנה נוצרה בהצלחה")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()
                                                .schema(new Schema<>().$ref("#/components/schemas/Order")))))
                        .addApiResponse("400", new ApiResponse()
                                .description("נתונים לא תקינים")));
    }

    /**
     * יצירת Operation עבור שליפת הזמנה
     */
    private Operation getOrderOperation() {
        return new Operation()
                .summary("שליפת הזמנה")
                .description("מחזיר פרטי הזמנה לפי מזהה")
                .operationId("getOrder")
                .addParametersItem(new Parameter()
                        .name("orderId")
                        .in("path")
                        .required(true)
                        .description("מזהה ההזמנה")
                        .schema(new Schema<String>().type("string")))
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse()
                                .description("פרטי ההזמנה")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()
                                                .schema(new Schema<>().$ref("#/components/schemas/Order")))))
                        .addApiResponse("404", new ApiResponse()
                                .description("הזמנה לא נמצאה")));
    }

    /**
     * יצירת Operation עבור שליפת סטטוס הזמנה
     */
    private Operation getOrderStatusOperation() {
        return new Operation()
                .summary("שליפת סטטוס הזמנה")
                .description("מחזיר את הסטטוס הנוכחי של ההזמנה")
                .operationId("getOrderStatus")
                .addParametersItem(new Parameter()
                        .name("orderId")
                        .in("path")
                        .required(true)
                        .description("מזהה ההזמנה")
                        .schema(new Schema<String>().type("string")))
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse()
                                .description("סטטוס ההזמנה")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()
                                                .schema(new Schema<Map<String, Object>>().type("object")))))
                        .addApiResponse("404", new ApiResponse()
                                .description("הזמנה לא נמצאה")));
    }

    /**
     * יצירת Operation עבור עדכון סטטוס הזמנה
     */
    private Operation updateOrderStatusOperation() {
        return new Operation()
                .summary("עדכון סטטוס הזמנה")
                .description("מעדכן את הסטטוס של הזמנה קיימת")
                .operationId("updateOrderStatus")
                .addParametersItem(new Parameter()
                        .name("orderId")
                        .in("path")
                        .required(true)
                        .description("מזהה ההזמנה")
                        .schema(new Schema<String>().type("string")))
                .addParametersItem(new Parameter()
                        .name("status")
                        .in("query")
                        .required(true)
                        .description("הסטטוס החדש")
                        .schema(new Schema<String>().type("string")._enum(java.util.Arrays.asList("PENDING", "APPROVED", "REJECTED"))))
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse()
                                .description("ההזמנה עודכנה בהצלחה")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()
                                                .schema(new Schema<>().$ref("#/components/schemas/Order")))))
                        .addApiResponse("404", new ApiResponse()
                                .description("הזמנה לא נמצאה")));
    }

    /**
     * יצירת Operation עבור בדיקת מצב המטמון
     */
    private Operation getCacheStatusOperation() {
        return new Operation()
                .summary("בדיקת מצב המטמון")
                .description("מחזיר מידע על מצב המטמון המקומי וחיבור Redis")
                .operationId("getCacheStatus")
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse()
                                .description("מידע על מצב המטמון")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()
                                                .schema(new Schema<Map<String, Object>>().type("object")))))
                        .addApiResponse("500", new ApiResponse()
                                .description("שגיאה פנימית")));
    }

    /**
     * יצירת Operation עבור בדיקת זמינות השירות
     */
    private Operation healthCheckOperation() {
        return new Operation()
                .summary("בדיקת זמינות השירות")
                .description("מחזיר מידע על מצב השירות")
                .operationId("healthCheck")
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse()
                                .description("השירות זמין")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()
                                                .schema(new Schema<Map<String, Object>>().type("object")))))
                        .addApiResponse("503", new ApiResponse()
                                .description("השירות לא זמין")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()
                                                .schema(new Schema<Map<String, Object>>().type("object"))))));
    }
} 