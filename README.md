# 🛍️ Order Processing System

A robust Spring Boot microservice for managing e-commerce orders with automated status updates.

```ascii
┌─────────────────┐         ┌──────────────┐         ┌─────────────┐
│   Client Apps   │ ───────▶│  Order API   │ ───────▶│  Database   │
└─────────────────┘   HTTP  └──────────────┘   JPA   └─────────────┘
                                 │
                                 │
                          ┌─────────────┐
                          │  Scheduler  │
                          └──��────────��─┘
```

## 🚀 Tech Stack

```
├── Backend Framework
│   ├── Spring Boot 3.x
│   ├── Java 17
│   └── Maven
│
├── Database
│   └── PostgreSQL
│
├── Documentation
│   └── OpenAPI (Swagger)
│
├── Monitoring
│   └── Spring Actuator
│
└── Testing
    ├── JUnit 5
    ├── Mockito
    └── H2 Database
```

## 📊 System Architecture

```mermaid
graph TD
    A[Client] -->|HTTP Request| B[Controller Layer]
    B --> C[Service Layer]
    C --> D[Repository Layer]
    D -->|JPA| E[(PostgreSQL)]
    F[Scheduler] -->|Every 5min| C
    G[Actuator] -->|Monitoring| B
```

## 🛠️ Setup & Installation

### Prerequisites
- Java 17
- Maven
- PostgreSQL
- Docker (optional)

### Local Development

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd processing.system
   ```

2. **Configure Database**
   - Create PostgreSQL database
   - Update application.yml if needed

3. **Build the application**
   ```bash
   mvn clean install
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

### 🐳 Docker Deployment

1. **Build Docker image**
   ```bash
   docker build -t order-processing-system .
   ```

2. **Run container**
   ```bash
   docker run -p 8080:8080 \
   -e SPRING_PROFILES_ACTIVE=prod \
   -e DB_URL=jdbc:postgresql://host.docker.internal:5432/order_processing \
   -e DB_USERNAME=your_username \
   -e DB_PASSWORD=your_password \
   order-processing-system
   ```

## 📊 Database Schema

### Tables Structure

#### 1. Customers
```sql
CREATE TABLE customers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(50),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 2. Items
```sql
CREATE TABLE items (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    description VARCHAR(255)
);
```

#### 3. Orders
```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    customer_id INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);
```

#### 4. OrderItems
```sql
CREATE TABLE order_items (
    id SERIAL PRIMARY KEY,
    order_id UUID NOT NULL,
    item_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (item_id) REFERENCES items(id)
);
```

### Entity Relationships
```mermaid
erDiagram
    CUSTOMERS ||--o{ ORDERS : places
    ORDERS ||--o{ ORDER_ITEMS : contains
    ITEMS ||--o{ ORDER_ITEMS : included_in
```

## 🔒 Security Implementation

### CSRF Protection
The API is secured with CSRF (Cross-Site Request Forgery) protection. For all POST, PUT, PATCH, and DELETE requests, you need to:

1. First get the CSRF token from the cookie (XSRF-TOKEN)
2. Include the token in the request header (X-XSRF-TOKEN)

### Making API Calls

#### GET Requests
GET requests don't require CSRF tokens:
```http
GET /api/orders
```

#### POST/PUT/PATCH/DELETE Requests
For modifying requests, include the CSRF token:

```http
// 1. First get the CSRF token cookie
GET /api/any-endpoint
// The response will include a cookie named XSRF-TOKEN

// 2. Include the token in subsequent requests
POST /api/orders
Headers:
  Content-Type: application/json
  X-XSRF-TOKEN: your-csrf-token-here
Body:
{
  "customerId": 1,
  "items": [
    {
      "itemId": 1,
      "quantity": 2
    }
  ]
}
```

### Example using cURL
```bash
# 1. Get the CSRF token
curl -c cookie.txt http://localhost:8080/api/orders

# 2. Make a POST request with the token
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "X-XSRF-TOKEN: $(grep XSRF-TOKEN cookie.txt | cut -f 7)" \
  -b cookie.txt \
  -d '{"customerId":1,"items":[{"itemId":1,"quantity":2}]}'
```

### Example using JavaScript/Axios
```javascript
// Configure axios to include credentials and handle CSRF
axios.defaults.withCredentials = true;

// First request will receive the CSRF token cookie
await axios.get('/api/orders');

// Subsequent requests will automatically include the CSRF token
await axios.post('/api/orders', {
  customerId: 1,
  items: [{
    itemId: 1,
    quantity: 2
  }]
});
```

## 🔍 API Endpoints

### Customer Management
- GET `/api/customers` - List all customers
- GET `/api/customers/{id}` - Get customer by ID
- POST `/api/customers` - Create new customer

### Item Management

#### 1. List All Items
```http
GET /api/items
```
**Response:**
```json
[
  {
    "id": 1,
    "name": "Smartphone",
    "price": 699.99,
    "description": "Latest model smartphone"
  },
  {
    "id": 2,
    "name": "Laptop",
    "price": 1299.99,
    "description": "High-performance laptop"
  }
]
```

#### 2. Get Item by ID
```http
GET /api/items/{id}
```
**Response:**
```json
{
  "id": 1,
  "name": "Smartphone",
  "price": 699.99,
  "description": "Latest model smartphone"
}
```

#### 3. Create Single Item
```http
POST /api/items
```
**Request Body:**
```json
{
  "name": "Smartphone",
  "price": 699.99,
  "description": "Latest model smartphone"
}
```
**Response:** Returns created item with status code 201

#### 4. Create Multiple Items
```http
POST /api/items/batch
```
**Request Body:**
```json
[
  {
    "name": "Smartphone",
    "price": 699.99,
    "description": "Latest model smartphone"
  },
  {
    "name": "Laptop",
    "price": 1299.99,
    "description": "High-performance laptop"
  }
]
```
**Response:** Returns list of created items with status code 201

#### 5. Delete Item
```http
DELETE /api/items/{id}
```
**Response:** Status code 204 (No Content)

#### 6. Delete Multiple Items
```http
DELETE /api/items/batch
```
**Request Body:**
```json
[1, 2, 3]
```
**Response:** Status code 204 (No Content)

### Order Management

#### 1. Create Order
```http
POST /api/orders
```
**Request Body:**
```json
{
  "customerId": 1,
  "items": [
    {
      "itemId": 1,
      "quantity": 2
    },
    {
      "itemId": 2,
      "quantity": 1
    }
  ]
}
```
**Response:**
```json
{
  "id": "uuid",
  "status": "PENDING",
  "customer": {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com"
  },
  "items": [
    {
      "itemId": 1,
      "itemName": "Smartphone",
      "itemPrice": 699.99,
      "quantity": 2,
      "subtotal": 1399.98
    }
  ],
  "totalAmount": 1399.98,
  "createdAt": "2025-11-02T10:00:00",
  "updatedAt": "2025-11-02T10:00:00"
}
```

#### 2. Get Order Details
```http
GET /api/orders/{id}
```
**Response:**
```json
{
  "id": "uuid",
  "status": "PENDING",
  "customer": {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com"
  },
  "items": [
    {
      "itemId": 1,
      "itemName": "Smartphone",
      "itemPrice": 699.99,
      "quantity": 2,
      "subtotal": 1399.98
    }
  ],
  "totalAmount": 1399.98,
  "createdAt": "2025-11-02T10:00:00",
  "updatedAt": "2025-11-02T10:00:00"
}
```

#### 3. List All Orders
```http
GET /api/orders?status=PENDING
```
Query Parameters:
- `status` (optional): Filter by order status

**Response:**
```json
[
  {
    "id": "uuid",
    "status": "PENDING",
    "customer": {
      "id": 1,
      "name": "John Doe",
      "email": "john@example.com"
    },
    "items": [...],
    "totalAmount": 1399.98,
    "createdAt": "2025-11-02T10:00:00",
    "updatedAt": "2025-11-02T10:00:00"
  }
]
```

#### 4. Cancel Order
```http
PATCH /api/orders/{id}/cancel
```
**Response:**
```json
{
  "id": "uuid",
  "status": "CANCELLED",
  "customer": {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com"
  },
  "items": [...],
  "totalAmount": 1399.98,
  "createdAt": "2025-11-02T10:00:00",
  "updatedAt": "2025-11-02T10:00:00"
}
```

### 📊 Monitoring Endpoints

```
├── Health Check
│   └── GET /actuator/health
│
├── Metrics
│   └── GET /actuator/metrics
│
├── Environment Info
│   └── GET /actuator/env
│
└── Logs
    └── GET /actuator/loggers
```

## 📝 API Validations

### Item Request Validation
- `name`: Required, non-blank string
- `price`: Required, positive number
- `description`: Optional string

### Order Request Validation
- `customerId`: Required, must reference an existing customer
- `items`: Required, non-empty array of order items
- `items[].itemId`: Required, must reference an existing item
- `items[].quantity`: Required, must be positive

## 🔄 Business Flow

```ascii
Item Management ─────┐
                    │
                    ▼
Order Creation ──▶ PENDING ──┬──▶ PROCESSING ──▶ SHIPPED ──▶ DELIVERED
                            │
                            └──▶ CANCELLED
```

## 🚦 Order States

1. **PENDING**
   - Initial state when order is created
   - Can be cancelled
   - Auto-transitions to PROCESSING after 5 minutes

2. **PROCESSING**
   - Order is being processed
   - Cannot be cancelled
   - Can transition to SHIPPED

3. **SHIPPED**
   - Order is in transit
   - Cannot be cancelled

4. **DELIVERED**
   - Final successful state
   - Order completed

5. **CANCELLED**
   - Final cancelled state
   - Only possible from PENDING state

## 🔍 API Documentation

Access Swagger UI: `http://localhost:8080/swagger-ui.html`

## 🔧 Configuration

### Application Profiles

1. **dev**
   - Local development
   - H2 in-memory database
   - SQL logging enabled

2. **prod**
   - Production settings
   - PostgreSQL database
   - Minimal logging

### Environment Variables

```properties
DB_URL=jdbc:postgresql://host:port/dbname
DB_USERNAME=username
DB_PASSWORD=password
```

## 🗄️ Database Configuration

### Development (PostgreSQL)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://host:port/dbname
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: none
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

### Testing (H2)
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    username: sa
    password: 
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
```

## 📈 Monitoring

### Health Checks
```http
GET /actuator/health
```
Response:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

### Metrics
```http
GET /actuator/metrics
```

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

The project includes comprehensive tests for:
- OrderService
- CustomerService
- ItemService
- Controllers
- Database operations

### Test Data
Sample data is automatically loaded for testing:
- Customers: 3 sample customers
- Items: 5 sample products
- Orders: Generated during tests

## 🌟 Features

- [x] RESTful API
- [x] Automated status updates
- [x] Database persistence
- [x] Docker support
- [x] Health monitoring
- [x] API documentation
- [x] Comprehensive testing
- [x] Transaction management

## 📝 License

This project is licensed under the MIT License - see the LICENSE.md file for details

## 🤝 Contributing

1. Fork the Project
2. Create your Feature Branch
3. Commit your Changes
4. Push to the Branch
5. Open a Pull Request
