# API Contracts - OpenAPI Specifications

## סקירה כללית

הפרויקט כולל קבצי OpenAPI מפורטים עבור כל שירות במערכת. כל קובץ מכיל תיעוד מלא של ה-API, schemas, enums, דוגמאות, ומידע על הארכיטקטורה.

## קבצי OpenAPI

### 1. Order Service API
**קובץ:** `order-service/order-service-api.yaml`
**פורט:** 8081
**תיאור:** שירות ניהול הזמנות עם REST API מלא

**Endpoints עיקריים:**
- `POST /api/v1/orders` - יצירת הזמנה חדשה
- `GET /api/v1/orders/{orderId}` - שליפת הזמנה
- `GET /api/v1/orders/{orderId}/status` - שליפת סטטוס הזמנה
- `PUT /api/v1/orders/{orderId}/status` - עדכון סטטוס הזמנה
- `GET /api/v1/orders/cache/status` - בדיקת מצב המטמון
- `GET /api/v1/orders/health` - בדיקת זמינות השירות

**מאפיינים:**
- תמיכה מלאה ב-CRUD operations
- מנגנון fallback עם מטמון מקומי
- פרסום אירועים ל-Kafka
- ניטור מצב Redis

### 2. Inventory Service API
**קובץ:** `inventory-service/inventory-service-api.yaml`
**פורט:** 8082
**תיאור:** שירות בדיקת מלאי מבוסס אירועים

**Endpoints עיקריים:**
- `GET /actuator/health` - בדיקת זמינות
- `GET /actuator/info` - מידע על השירות
- `GET /actuator/metrics` - מטריקות ביצועים
- `GET /actuator/prometheus` - מטריקות Prometheus

**מאפיינים:**
- Event-driven architecture
- Strategy Pattern לבדיקת מלאי
- 3 אסטרטגיות: DIGITAL, PERISHABLE, STANDARD
- מנגנון DLQ עבור Kafka
- מטריקות מפורטות לניטור

### 3. Notification Service API
**קובץ:** `notification-service/notification-service-api.yaml`
**פורט:** 8083
**תיאור:** שירות הודעות מבוסס אירועים

**Endpoints עיקריים:**
- `GET /actuator/health` - בדיקת זמינות
- `GET /actuator/info` - מידע על השירות
- `GET /actuator/metrics` - מטריקות ביצועים
- `GET /actuator/prometheus` - מטריקות Prometheus

**מאפיינים:**
- הצגת הודעות בקונסול
- שליפת פרטי הזמנות מ-Redis
- מנגנון fallback במקרה של כשל
- תמיכה בהודעות מאושרות ונדחות

## Schemas משותפים

### מודלים עיקריים

**Order:**
- `orderId` (UUID) - מזהה ייחודי
- `customerName` (string) - שם הלקוח
- `items` (array) - רשימת פריטים
- `status` (enum) - סטטוס ההזמנה
- `createdAt` (datetime) - זמן יצירה
- `lastUpdated` (datetime) - זמן עדכון אחרון

**OrderItem:**
- `productId` (string) - מזהה מוצר
- `quantity` (integer) - כמות
- `category` (enum) - קטגוריית המוצר

**OrderRequest:**
- `customerName` (string) - שם הלקוח
- `items` (array) - רשימת פריטים
- `requestId` (string) - מזהה בקשה
- `requestDateTime` (datetime) - זמן בקשה

### Enums

**OrderStatus:**
- `PENDING` - הזמנה ממתינה
- `PROCESSING` - בעיבוד
- `APPROVED` - מאושרת
- `REJECTED` - נדחתה

**Category:**
- `DIGITAL` - מוצרים דיגיטליים (תמיד זמינים)
- `PERISHABLE` - מוצרים מתכלים (בדיקת תאריך)
- `STANDARD` - מוצרים רגילים (בדיקת מלאי)

### אירועי Kafka

**OrderCreatedEvent:**
- נשלח על ידי Order Service
- מתקבל על ידי Inventory Service
- מכיל פרטי הזמנה לבדיקת מלאי

**InventoryCheckResultEvent:**
- נשלח על ידי Inventory Service
- מתקבל על ידי Notification Service
- מכיל תוצאות בדיקת מלאי

## שימוש בקבצים

### 1. הצגת תיעוד

```bash
# הצגת Swagger UI עבור Order Service
npx swagger-ui-serve order-service/order-service-api.yaml

# או שימוש בכלי אחר
swagger-codegen-cli generate -i order-service/order-service-api.yaml -l html2 -o docs/
```

### 2. יצירת client code

```bash
# יצירת Java client
swagger-codegen-cli generate -i order-service/order-service-api.yaml -l java -o clients/java/

# יצירת TypeScript client
swagger-codegen-cli generate -i order-service/order-service-api.yaml -l typescript-axios -o clients/typescript/

# יצירת Python client
swagger-codegen-cli generate -i order-service/order-service-api.yaml -l python -o clients/python/
```

### 3. אימות תקינות

```bash
# אימות syntax
swagger-codegen-cli validate -i order-service/order-service-api.yaml

# או שימוש ב-OpenAPI validator
openapi-generator-cli validate -i order-service/order-service-api.yaml
```

## מאפיינים מתקדמים

### 1. מטריקות ואמצעי ניטור

כל שירות כולל מטריקות מפורטות:
- מספר בקשות ותגובות
- זמני תגובה
- שיעורי שגיאות
- מצב משאבים (Redis, Kafka)

### 2. מנגנוני חוסן

**Order Service:**
- Fallback למטמון מקומי
- Health checks מתקדמים
- Retry mechanisms

**Inventory Service:**
- Strategy Pattern גמיש
- DLQ לטיפול בכשלים
- מטריקות לפי קטגוריה

**Notification Service:**
- Fallback display
- Redis failure handling
- Consumer group management

### 3. תמיכה בפיתוח

**דוגמאות מלאות:**
- כל endpoint כולל דוגמאות request/response
- תרחישים שונים (הצלחה/כשל)
- מקרי קיצון

**תיעוד מפורט:**
- הסברים בעברית
- תיאור תהליכים
- מידע על תלויות

## הערות נוספות

### API-First Development
הקבצים מיועדים לפיתוח מבוסס API-First:
1. עיצוב הספציפיקציה קודם
2. יצירת mock servers
3. פיתוח במקביל של client ו-server
4. אימות אוטומטי

### עדכון וגירסאות
- כל שירות מנהל גירסה עצמאית
- שינויים breaking דורשים עדכון גירסה
- תמיכה לאחור בגירסאות קודמות

### אבטחה
- מוכן לתמיכה ב-API Keys
- הגדרות security schemes
- תמיכה ב-OAuth2 בעתיד

### למידע נוסף
- [OpenAPI Specification](https://swagger.io/specification/)
- [Swagger Tools](https://swagger.io/tools/)
- [API Design Best Practices](https://swagger.io/resources/articles/best-practices-in-api-design/)

---

**עדכון אחרון:** 2025-01-13
**גירסה:** 1.0.0 