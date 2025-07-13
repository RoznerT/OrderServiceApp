FROM openjdk:17-jdk-slim

LABEL maintainer="Order Service Team"
LABEL version="1.0"
LABEL description="Order Service for microservices architecture"

# Install required packages
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Copy source code
COPY order-service/src order-service/src
COPY order-service/build.gradle.kts order-service/build.gradle.kts
COPY common common

# Make gradlew executable
RUN chmod +x gradlew

# Build the application
RUN ./gradlew :order-service:build -x test

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Run the application
CMD ["java", "-jar", "order-service/build/libs/order-service-0.0.1-SNAPSHOT.jar"] 