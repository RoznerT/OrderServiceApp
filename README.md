# Home Assignment - Microservices Architecture

## Overview
This project implements a distributed microservices system that simulates an order processing workflow with asynchronous communication, temporary data storage, and varying logic based on product types.

## Architecture

The system consists of three main microservices:

### 1. Order Service (Port: 8081)
- **Responsibilities:**
  - Exposes REST API for order creation
  - Validates order data 
  - Stores orders in Redis with local cache fallback
  - Publishes order events to Kafka with DLQ support
  - Provides order status and cache monitoring endpoints

### 2. Inventory Service (Port: 8082)
- **Responsibilities:**
  - Listens to order-created events from Kafka
  - Implements Strategy Pattern for different product categories:
    - **Digital**: Always available
    - **Perishable**: Checks expiration date
    - **Standard**: Checks stock levels
  - Publishes inventory check results to Kafka
  - Uses Dead Letter Queue for failed processing

### 3. Notification Service (Port: 8083)
- **Responsibilities:**
  - Listens to inventory-check-result events from Kafka
  - Retrieves full order details from Redis
  - Displays order notifications to console
  - Handles both approved and rejected orders
  - Uses Dead Letter Queue for failed notifications

## Technology Stack

- **Framework**: Spring Boot 3.1.5 with WebFlux (Reactive Programming)
- **Message Broker**: Apache Kafka with KafkaTemplate and Dead Letter Queue
- **Cache**: Redis for temporary data storage with local fallback cache
- **API Documentation**: OpenAPI 3.0 with Swagger UI
- **Build Tool**: Gradle with Kotlin DSL
- **Containerization**: Docker with Docker Compose
- **Monitoring**: Actuator endpoints, Kafka UI, Redis Commander

## Key Features

### API-First Approach
- OpenAPI YAML specifications for all services
- Swagger UI available at `/swagger-ui.html` for each service
- Code generation from OpenAPI specs

### Reactive Programming
- WebFlux for non-blocking, reactive operations
- Reactive Redis templates for asynchronous data access
- Reactive Kafka integration where applicable

### Error Handling & Resilience
- **Kafka Error Handling:**
  - Automatic retry mechanism with exponential backoff (3 attempts)
  - Dead Letter Queue (DLQ) for permanently failed messages
  - Comprehensive logging for failed events
- **Redis Error Handling:**
  - Local cache fallback when Redis is unavailable
  - Automatic health checks every 30 seconds
  - Seamless sync back to Redis when recovered
  - Graceful degradation with transparent operation

### Data Validation
- Custom ValidationUtils class for consistent validation
- JSR-303 Bean Validation annotations
- Null-safe operations using Objects.isNull, Objects.nonNull
- StringUtils.hasText for string validation
- CollectionUtils.isEmpty for collection validation

### Fallback Mechanisms
- **Redis Fallback:**
  - In-memory local cache (ConcurrentHashMap)
  - TTL-based expiration (30 minutes)
  - Size-limited cache (1000 entries)
  - Automatic cleanup of expired entries
  - Background sync when Redis becomes available

## Project Structure

```
home-assign/
├── common/                    # Shared models and events
│   ├── src/main/java/com/example/common/
│   │   ├── models/           # Order, OrderItem, OrderRequest
│   │   ├── events/           # OrderCreatedEvent, InventoryCheckResultEvent
│   │   ├── enums/            # Category, OrderStatus
│   │   └── utils/            # ValidationUtils
├── order-service/            # Order management service
│   ├── src/main/java/com/example/orderservice/
│   │   ├── controller/       # REST controllers
│   │   ├── service/          # Business logic with fallback
│   │   ├── config/           # Kafka, Redis configuration
│   │   └── exception/        # Exception handlers
│   ├── src/main/resources/
│   │   ├── openapi.yaml      # API specification
│   │   └── application.yaml  # Configuration
│   └── Dockerfile
├── inventory-service/        # Inventory management service
│   ├── src/main/java/com/example/inventoryservice/
│   │   ├── strategy/         # Strategy pattern implementations
│   │   ├── service/          # Business logic
│   │   ├── listener/         # Kafka event listeners with DLQ
│   │   └── config/           # Configuration
│   └── Dockerfile
├── notification-service/     # Notification service
│   ├── src/main/java/com/example/notificationservice/
│   │   ├── service/          # Business logic
│   │   ├── listener/         # Kafka event listeners with DLQ
│   │   └── config/           # Configuration
│   └── Dockerfile
├── docker-compose.yml        # Full system orchestration
├── build.gradle.kts         # Root build configuration
└── README.md
```

## Getting Started

### Prerequisites
- Java 17+
- Docker and Docker Compose
- (Optional) Gradle 7.x for local development

### Quick Start with Docker

1. **Clone the repository:**
```bash
git clone <repository-url>
cd home-assign
```

2. **Start the entire system:**
```bash
docker-compose up --build
```

3. **Wait for services to start:**
   - The system will automatically start Kafka, Redis, and all microservices
   - Health checks ensure services start in correct order
   - Check logs for startup completion

4. **Access the services:**
   - Order Service: http://localhost:8081
   - Inventory Service: http://localhost:8082 
   - Notification Service: http://localhost:8083
   - Swagger UI: http://localhost:8081/swagger-ui.html
   - Kafka UI: http://localhost:8090
   - Redis Commander: http://localhost:8091

### Local Development

1. **Start infrastructure services:**
```bash
docker-compose up zookeeper kafka redis
```

2. **Build the project:**
```bash
./gradlew build
```

3. **Run services individually:**
```bash
# Terminal 1 - Order Service
./gradlew :order-service:bootRun

# Terminal 2 - Inventory Service  
./gradlew :inventory-service:bootRun

# Terminal 3 - Notification Service
./gradlew :notification-service:bootRun
```

## API Usage Examples

### 1. Basic Order Creation - Returns APPROVED

#### Digital Products (DIGITAL)
```bash
curl -X POST http://localhost:8081/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "דוד כהן",
    "items": [
      {
        "productId": "DIGITAL-001",
        "quantity": 1,
        "category": "DIGITAL"
      }
    ],
    "requestId": "2025-01-13T10:00:00Z",
    "requestDateTime": "2025-01-13T10:00:00"
  }'
```

#### Standard Products (STANDARD) - Available Stock
```bash
curl -X POST http://localhost:8081/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "שרה לוי",
    "items": [
      {
        "productId": "P1001",
        "quantity": 2,
        "category": "STANDARD"
      }
    ],
    "requestId": "2025-01-13T10:00:00Z",
    "requestDateTime": "2025-01-13T10:00:00"
  }'
```

#### Perishable Products (PERISHABLE) - Valid
```bash
curl -X POST http://localhost:8081/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "משה ישראלי",
    "items": [
      {
        "productId": "P1003",
        "quantity": 1,
        "category": "PERISHABLE"
      }
    ],
    "requestId": "2025-01-13T10:00:00Z",
    "requestDateTime": "2025-01-13T10:00:00"
  }'
```

### 2. Mixed Order Some Products Unavailable —Returns REJECTED

```bash
curl -X POST http://localhost:8081/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "רחל גולדברג",
    "items": [
      {
        "productId": "P1001",
        "quantity": 1,
        "category": "STANDARD"
      },
      {
        "productId": "P1002",
        "quantity": 3,
        "category": "STANDARD"
      },
      {
        "productId": "DIGITAL-001",
        "quantity": 1,
        "category": "DIGITAL"
      }
    ],
    "requestId": "2025-01-13T10:00:00Z",
    "requestDateTime": "2025-01-13T10:00:00"
  }'
```

### 3. Perishable Product Expired — Returns REJECTED

```bash
curl -X POST http://localhost:8081/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "יוסי כהן",
    "items": [
      {
        "productId": "P1005",
        "quantity": 2,
        "category": "PERISHABLE"
      }
    ],
    "requestId": "2025-01-13T10:00:00Z",
    "requestDateTime": "2025-01-13T10:00:00"
  }'
```

### 3. Get Order Status
```bash
curl http://localhost:8081/api/v1/orders/{orderId}/status
```

### 4. Update Order Status
```bash
curl -X PUT "http://localhost:8081/api/v1/orders/{orderId}/status?status=APPROVED"
```

### 5. Cache Status Monitoring
```bash
# Check cache status and Redis availability
curl http://localhost:8081/api/v1/orders/cache/status
```

### 6. Health Check
```bash
# Get service health with Redis status
curl http://localhost:8081/api/v1/orders/health
```

## Monitoring and Troubleshooting

### System Health Checks
```bash
# Order Service health
curl http://localhost:8081/api/v1/orders/health

# Standard actuator health checks
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

### Cache Monitoring
```bash
# Check cache status (Redis availability, fallback mode, cache size)
curl http://localhost:8081/api/v1/orders/cache/status

# Expected response when Redis is available:
{
  "redisAvailable": true,
  "fallbackMode": false,
  "localCacheSize": 5,
  "maxCacheSize": 1000,
  "cacheTtlMinutes": 30
}

# Expected response when Redis is down:
{
  "redisAvailable": false,
  "fallbackMode": true,
  "localCacheSize": 12,
  "maxCacheSize": 1000,
  "cacheTtlMinutes": 30
}
```

### Dead Letter Queue

#### Kafka
- Main topics: `order-created`, `inventory-check-result`
- DLQ topics: `order-created-dlq`, `inventory-check-result-dlq`
- Check message counts and failures

#### Service Logs
```bash
# Check for DLQ messages in logs
docker logs homeassign-inventory-service-1 | grep "DEAD LETTER QUEUE"
docker logs homeassign-notification-service-1 | grep "DEAD LETTER QUEUE"
```

## Error Scenarios and Recovery

### Scenario 1: Redis Failure
**What happens:**
- Order Service automatically switches to local cache fallback
- New orders are saved only to local cache
- Existing orders are served from local cache
- Service continues operating normally

**Recovery:**
- Redis health check runs every 30 seconds
- When Redis comes back online, local cache syncs automatically
- System transparently switches back to Redis

**Testing:**
```bash
# Stop Redis
docker-compose stop redis

# Create orders (will use local cache)
curl -X POST http://localhost:8081/api/v1/orders ...

# Check fallback status
curl http://localhost:8081/api/v1/orders/cache/status

# Restart Redis
docker-compose start redis

# Monitor logs for auto-recovery
docker logs homeassign-order-service-1 | grep "Redis is back online"
```

### Scenario 2: Kafka Processing Failure
**What happens:**
- Failed messages are automatically retried (3 attempts with exponential backoff)
- After exhausting retries, messages go to Dead Letter Queue
- System continues processing other messages

**Recovery:**
- Monitor DLQ topics in Kafka UI
- Manually reprocess DLQ messages if needed
- Check service logs for failure patterns

### Scenario 3: Service Restart
**What happens:**
- Local cache is lost (by design)
- Redis data persists and is immediately available
- In-flight Kafka messages are handled by retry mechanism

## Configuration

### Redis Fallback Settings
```yaml
# Configurable in OrderService
CACHE_TTL: 30 minutes
MAX_CACHE_SIZE: 1000 entries
HEALTH_CHECK_INTERVAL: 30 seconds
REDIS_TIMEOUT: 5 seconds
```

### Kafka DLQ Settings
```yaml
# Configurable in @RetryableTopic
attempts: 3
backoff: delay=1000ms, multiplier=2.0
dltTopicSuffix: "-dlq"
```

## Performance Characteristics

### Normal Operation (Redis Available)
- Order creation: ~50ms
- Order retrieval: ~10ms
- Full workflow: ~200ms

### Fallback Mode (Redis Unavailable)
- Order creation: ~5ms (local cache)
- Order retrieval: ~1ms (local cache)
- Full workflow: ~150ms

### Recovery Time
- Redis failure detection: ~5 seconds
- Redis recovery detection: ~30 seconds
- Cache sync time: ~1-2 seconds per 100 orders
