# Order Processing System

A monorepo containing two microservices built with **Java 17** and **Spring Boot 3.2**.

---

## Services

| Service | Port | Description |
|---|---|---|
| `order-service` | 8080 | Create, track, and cancel orders |
| `inventory-service` | 8081 | Stock reservation and release |

---

## Quick start — run both services locally

```bash
docker-compose up --build
```

| URL | Description |
|---|---|
| http://localhost:8080/swagger-ui.html | Order Service API docs |
| http://localhost:8081/swagger-ui.html | Inventory Service API docs |
| http://localhost:8080/actuator/health | Order Service health |
| http://localhost:8081/actuator/health | Inventory Service health |

> The scheduler is set to 10 seconds in docker-compose for demo purposes.
> Create a PENDING order and watch it move to PROCESSING automatically.

---

## Without Docker (Maven)

```bash
# Terminal 1 — inventory must start first
cd inventory-service
mvn spring-boot:run

# Terminal 2
cd order-service
mvn spring-boot:run
```

---

## Run tests

```bash
# All tests for both services
mvn verify

# One service only
mvn -pl order-service -am verify

# Skip tests (compile + package only)
mvn package -DskipTests
```

---

## API — order-service

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/orders` | Place a new order |
| `GET` | `/api/v1/orders/{id}` | Get order details |
| `GET` | `/api/v1/orders?status=PENDING` | List orders (status filter optional) |
| `PATCH` | `/api/v1/orders/{id}/status` | Update status manually |
| `POST` | `/api/v1/orders/{id}/cancel` | Cancel an order (PENDING only) |

### Example: create an order

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust-123",
    "items": [
      { "productId": "prod-1", "productName": "Widget A", "quantity": 2, "unitPrice": 29.99 }
    ]
  }'
```

### Example: cancel an order

```bash
curl -X POST http://localhost:8080/api/v1/orders/<ORDER_ID>/cancel
```

---

## API — inventory-service

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/inventory/reserve` | Reserve stock for an order |
| `POST` | `/api/v1/inventory/release` | Release a reservation |
| `GET` | `/api/v1/inventory/stock/{productId}` | Check stock levels |

### Seeded products

| Product ID | Name | Initial Stock |
|---|---|---|
| prod-1 | Widget A | 100 |
| prod-2 | Widget B | 50 |
| prod-3 | Gadget X | 25 |
| prod-4 | Gadget Y | 10 |

---

## Architecture

```
order-processing-system/
├── pom.xml                    ← parent POM (Java 21, Spring Boot 3.2, JaCoCo)
├── docker-compose.yml
├── .github/workflows/ci.yml  ← path-filtered CI, GHCR push on main
├── order-service/
│   └── src/main/java/.../order/
│       ├── domain/            Order · OrderItem · OrderStatus (state machine)
│       ├── repository/        OrderRepository interface + InMemoryOrderRepository
│       ├── service/           OrderService interface + OrderServiceImpl (Saga)
│       ├── controller/        OrderController (REST)
│       ├── scheduler/         OrderStatusScheduler (PENDING → PROCESSING)
│       ├── client/            InventoryClient interface + HttpInventoryClient
│       ├── dto/               Request/Response records
│       ├── mapper/            OrderMapper
│       └── exception/         GlobalExceptionHandler (RFC 7807 ProblemDetail)
└── inventory-service/
    └── src/main/java/.../inventory/
        ├── domain/            StockItem · Reservation
        ├── repository/        InventoryRepository + InMemoryInventoryRepository
        ├── service/           InventoryService interface + InventoryServiceImpl
        ├── controller/        InventoryController
        ├── dto/               Request/Response records
        └── exception/         GlobalExceptionHandler
```

---

## Design decisions

### 1. In-memory storage (ConcurrentHashMap)

Data lives in the JVM. Restart = data gone. This is intentional for a POC. The `OrderRepository` interface means swapping to JPA (PostgreSQL) means adding one class — zero changes to the service layer.

### 2. State machine (OrderStatus enum)

Each status constant declares its own allowed transitions. The service calls `canTransitionTo()` — no if-else chains, no scattered business rules.

```
PENDING → PROCESSING, CANCELLED
PROCESSING → SHIPPED
SHIPPED → DELIVERED
DELIVERED, CANCELLED → (terminal)
```

### 3. Thread-safe transitions (ConcurrentHashMap.compute)

The scheduler promotes PENDING→PROCESSING every 5 minutes. A customer can cancel at the same time. Both go through `repository.transition(id, expected, next)` which uses `compute()` — an atomic check-and-set. Exactly one wins. The other gets an empty Optional back.

### 4. Saga pattern (order-service → inventory-service)

Creating an order is two steps across two services:
1. Reserve stock in inventory-service
2. Persist the order in order-service

If step 2 fails, a compensating call releases the reservation. This is a simplified Saga. In production: use an event bus (Kafka) + outbox pattern to survive crashes between steps.

### 5. Repository pattern (Dependency Inversion)

`OrderServiceImpl` depends on `OrderRepository` (interface), not on `InMemoryOrderRepository` (class). To add a real database: create `JpaOrderRepository implements OrderRepository`. Zero changes to service or controller.

### 6. RFC 7807 error format

All errors return `ProblemDetail` (Spring Boot 3 built-in):
```json
{
  "type": "https://api.example.com/errors/order-not-found",
  "title": "Order not found",
  "status": 404,
  "detail": "No order found with id: <uuid>"
}
```

---

## CI/CD (GitHub Actions)

- **detect-changes**: uses `dorny/paths-filter` — only builds the module(s) whose files changed.
- **build-test**: runs `mvn verify` (compile + test + JaCoCo coverage gate at 70%).
- **docker-push**: on `main` branch only — builds Docker images and pushes to `ghcr.io/<owner>/order-service:latest`.

No Jenkins. No manual secrets setup — `GITHUB_TOKEN` is provided automatically.

---

## Production next steps

- Replace `ConcurrentHashMap` with PostgreSQL (JPA) behind the existing repository interfaces
- Add **ShedLock** so the scheduler only runs on one instance in a cluster
- Add **Kafka** and the **Outbox pattern** for the Saga instead of a direct HTTP compensating call
- Add **Spring Security** (JWT / OAuth2)
- Add an **API Gateway** (Spring Cloud Gateway) in front of both services
- Add a **Service Registry** (Eureka) for dynamic service discovery
