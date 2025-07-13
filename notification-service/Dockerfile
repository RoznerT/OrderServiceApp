FROM openjdk:17-jdk-slim

LABEL maintainer="Notification Service Team"
LABEL version="1.0"
LABEL description="Notification Service for microservices architecture"

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
COPY notification-service/src notification-service/src
COPY notification-service/build.gradle.kts notification-service/build.gradle.kts
COPY common common

# Make gradlew executable
RUN chmod +x gradlew

# Build the application
RUN ./gradlew :notification-service:build -x test

# Expose port
EXPOSE 8083

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1

# Run the application
CMD ["java", "-jar", "notification-service/build/libs/notification-service-0.0.1-SNAPSHOT.jar"] 