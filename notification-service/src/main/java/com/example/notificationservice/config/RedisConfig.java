package com.example.notificationservice.config;

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
     * יצירת ObjectMapper עם תמיכה בתאריכים
     * מאפשר סדרן נכון של אוביקטים עם תאריכים
     *
     * @return ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.findAndRegisterModules();
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true);
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return objectMapper;
    }

    /**
     * יצירת ReactiveRedisTemplate
     * מגדיר את הסדרנים עבור מפתחות וערכים
     *
     * @param connectionFactory חיבור ל-Redis (מוגדר אוטומטית על ידי Spring Boot)
     * @param objectMapper      ObjectMapper לסדרן JSON
     * @return ReactiveRedisTemplate
     */
    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        RedisSerializationContext<String, Object> serializationContext =
                RedisSerializationContext.<String, Object>newSerializationContext()
                        .key(StringRedisSerializer.UTF_8)
                        .value(jsonSerializer)
                        .hashKey(StringRedisSerializer.UTF_8)
                        .hashValue(jsonSerializer)
                        .build();

        ReactiveRedisTemplate<String, Object> template = new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
        log.info("ReactiveRedisTemplate configured successfully");
        return template;
    }
} 