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

## 🔍 API Endpoints

### Order Management

#### 1. Create Order
```http
POST /api/orders
Content-Type: application/json

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

Example using curl:
```bash
curl -X POST 'https://order-processing-system-x02o.onrender.com/api/orders' \
-H "Content-Type: application/json" \
-d '{
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
}'
```

**Response:** `200 OK`
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

Example using curl:
```bash
curl https://order-processing-system-x02o.onrender.com/api/orders/{id}
```

#### 3. List All Orders
```http
GET /api/orders?status=PENDING
```

Example using curl:
```bash
# Get all orders
curl https://order-processing-system-x02o.onrender.com/api/orders

# Get orders with specific status
curl https://order-processing-system-x02o.onrender.com/api/orders?status=PENDING
```

#### 4. Cancel Order
```http
PATCH /api/orders/{id}/cancel
```

Example using curl:
```bash
curl -X PATCH https://order-processing-system-x02o.onrender.com/api/orders/{id}/cancel
```

### Using with JavaScript/Axios
```javascript
const axios = require('axios');

// Create new order
async function createOrder() {
  const response = await axios.post('https://order-processing-system-x02o.onrender.com/api/orders', {
    customerId: 1,
    items: [
      {
        itemId: 1,
        quantity: 2
      }
    ]
  });
  return response.data;
}

// Get all orders
async function getOrders() {
  const response = await axios.get('https://order-processing-system-x02o.onrender.com/api/orders');
  return response.data;
}
```

## 🔍 API Validations

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

## 🔧 Troubleshooting

### Common Issues

#### Request Failed
- Check if the request body is properly formatted JSON
- Verify the customer ID exists
- Verify the item IDs exist
- Check that quantities are positive numbers

#### Server Response Codes
- `200 OK`: Request successful
- `400 Bad Request`: Invalid input data
- `404 Not Found`: Resource not found (order/customer/item)
- `500 Internal Server Error`: Server-side error
