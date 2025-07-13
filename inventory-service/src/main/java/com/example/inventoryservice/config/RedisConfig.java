package com.example.inventoryservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * קונפיגורציה ל-Redis
 * מגדיר את הסדרנים והתבניות
 */
@Configuration
@Slf4j
public class RedisConfig {

    /**
     * יצירת ObjectMapper עם תמיכה בתאריכים ו-Java 17+ collections
     * מאפשר סדרן נכון של אוביקטים עם תאריכים ו-immutable collections
     *
     * @return ObjectMapper instance
     */
        @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.findAndRegisterModules();
        
        // הגדרות בסיסיות
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
        
        // הגדרות לטיפול בimmutable collections
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        
        // הגדרות לטיפול ב-Java 17+ collections
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true);
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        
        // הגדרות נוספות לטיפול ב-ImmutableCollections
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL, true);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        
        // תמיכה ב-collections מורכבים
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
        
        // הגדרות קריטיות לטיפול ב-ImmutableCollections
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_SELF_REFERENCES, false);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false);
        
        // הוספת מודול Guava אם זמין
        try {
            Class.forName("com.fasterxml.jackson.datatype.guava.GuavaModule");
            objectMapper.registerModule(new com.fasterxml.jackson.datatype.guava.GuavaModule());
            log.info("Guava module registered for better collection support");
        } catch (ClassNotFoundException e) {
            log.debug("Guava module not available, using standard configuration");
        }
       
        log.info("ObjectMapper configured with Java 17+ collections support");
        
        return objectMapper;
    }

    /**
     * יצירת ReactiveRedisTemplate
     * מגדיר את הסדרנים עבור מפתחות וערכים
     *
     * @param connectionFactory חיבור ל-Redis (מוגדר אוטומטית על ידי Spring Boot)
     * @param objectMapper     ObjectMapper לסדרן JSON
     * @return ReactiveRedisTemplate
     */
    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {
        
        // יצירת סדרן JSON מותאם
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        
        // הגדרת קונטקסט הסדרן
        RedisSerializationContext<String, Object> serializationContext = 
                RedisSerializationContext.<String, Object>newSerializationContext()
                        .key(StringRedisSerializer.UTF_8)
                        .value(jsonSerializer)
                        .hashKey(StringRedisSerializer.UTF_8)
                        .hashValue(jsonSerializer)
                        .build();

        ReactiveRedisTemplate<String, Object> template = new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
        
        log.info("ReactiveRedisTemplate configured successfully with enhanced serializer");
        
        return template;
    }
} 