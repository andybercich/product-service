# Product Service

## Overview

The Product Service is responsible for managing products, categories, packs, and inventory operations within the system.

It provides CRUD operations for products, categories, and packs, supports advanced searches, manages stock movements, and exposes internal operations used by the Order Service to check and modify inventory.

The service also publishes stock movement events through Kafka using a Transactional Outbox pattern. These events are consumed by the Analytics Service, which maintains the analytical representation of inventory activity.

The Product Service does not consume Kafka events.

## Responsibilities

The Product Service is responsible for:

* Managing products.
* Managing categories.
* Managing packs and their product composition.
* Searching products and packs using multiple filters.
* Managing product stock.
* Processing stock movements.
* Validating stock availability before decreasing inventory.
* Providing internal stock operations for the Order Service.
* Publishing stock movement events through Kafka.
* Persisting Outbox events when stock movements are performed.
* Maintaining reliable event publication through retries and DLT/DLQ handling.

## Technology Stack

* Java 17
* Spring Boot
* Spring Security
* Spring Data JPA
* MySQL
* Spring Cloud Eureka
* Apache Kafka
* Transactional Outbox
* Gradle
* Docker

## Product Management

The service provides CRUD operations for products.

Products contain information such as their name, code, category, purchase price, sale price, description, current stock, minimum stock level, and active state.

The service supports:

* Product creation.
* Product updates.
* Product deletion.
* Product lookup.
* Product listing.
* Product search using multiple filters.

Product searches can filter by:

* Product code.
* Product name.
* Category.
* Sale price range.
* Active state.
* Whether the product is a parent product.

## Category Management

Categories are managed by the Product Service through CRUD operations.

Products can be associated with a category, and category information is also used by product search and stock movement events.

## Pack Management

The Product Service also manages packs.

A pack does not maintain an independent stock value. Instead, a pack is composed of multiple `PackItem` entities, where each item references a product and a quantity.

Conceptually:

```text
Pack
 ├── Product A × quantity
 ├── Product B × quantity
 └── Product C × quantity
```

For example:

```text
Pack Coca Cola x6
 └── Coca Cola 1L × 6
```

Pack operations therefore affect the stock of the products that compose the pack.

The service provides CRUD operations for packs and supports pack searches using multiple criteria.

Pack searches can filter by:

* Denomination.
* Active state.
* Product name.
* Product code.

## Stock Management

The Product Service manages the stock of products and supports stock increases and decreases.

Stock can be modified through two different flows.

The first flow is a stock movement explicitly performed through the Product Service:

```text
User
  ↓
API Gateway
  ↓
Product Service
  ↓
Stock Movement Request
  ↓
Validation
  ↓
Stock Update
  ↓
Stock Movement Event
  ↓
Transactional Outbox
  ↓
Kafka
  ↓
Analytics Service
```

The second flow is an internal operation initiated by the Order Service:

```text
Order Service
      ↓
/internal/stock/**
      ↓
Product Service
      ↓
Product / Pack stock update
```

The Order Service can request the Product Service to:

* Check product information.
* Check pack information.
* Decrease product stock.
* Increase product stock.
* Decrease pack stock.
* Increase pack stock.

Pack stock operations ultimately modify the stock of the products contained in the pack.

These internal stock operations do not generate the Product Service stock movement event.

## Stock Movement Types

The Product Service supports the following stock movement types:

```text
RESTOCK
LOSS
RETURN
CORRECTION
```

A stock movement changes the product's current stock and generates a stock movement event.

For example:

```text
RESTOCK

stockBefore      = 10
quantityChanged  = 5
stockAfter       = 15
```

or:

```text
LOSS

stockBefore      = 10
quantityChanged  = 3
stockAfter       = 7
```

## Stock Validation

The service does not allow a stock decrease that would result in negative stock.

For example, if a product has:

```text
Current stock = 5
```

and a loss of:

```text
Quantity = 6
```

is requested, the operation is rejected with an `IllegalArgumentException`.

When an invalid stock operation is rejected, the product stock remains unchanged and no new Outbox event is created.

The product also contains a minimum stock value.

The minimum stock does not prevent stock operations. It can be used by the client application to identify products that have reached or fallen below their configured minimum stock level and display an inventory alert.

## Stock Movement Events

The Product Service publishes stock movement events only when a stock movement is performed through its stock movement functionality.

It does not publish Kafka events for product creation, product updates, or product deletion.

The stock movement event contains:

```java
UUID eventId;
Long productId;
String productName;
StockMovementType movementType;
Integer stockBefore;
Integer stockAfter;
Integer quantityChanged;
String category;
LocalDateTime timestamp;
```

The `eventId` uniquely identifies the event.

The event provides both the previous and resulting stock values, allowing the Analytics Service to reconstruct the inventory movement without directly accessing the Product Service database.

## Transactional Outbox

Stock changes and their corresponding Outbox events are persisted within the same transaction.

The flow is:

```text
Stock Movement
      ↓
Stock validation
      ↓
Stock update
      ↓
Outbox event creation
      ↓
Database transaction
      ↓
Transaction committed
```

A scheduled Outbox publisher then attempts to publish pending events to Kafka.

This prevents a situation where the stock is successfully modified but the corresponding event is lost because Kafka is temporarily unavailable.

The publisher:

1. Retrieves pending Outbox events.
2. Attempts to publish them to Kafka.
3. Updates the event state after successful publication.
4. Retries failed publications.
5. Tracks retry attempts.
6. Marks an event as `FAILED` after three failed attempts.
7. Uses DLT/DLQ handling for events that cannot be successfully published.

## Kafka Integration

The Product Service acts as a Kafka producer.

It publishes stock movement events to Kafka so that other services can process inventory changes asynchronously.

The Product Service does not consume Kafka events.

The Analytics Service consumes the published stock movement events and stores its own analytical representation of inventory activity.

This keeps the Product Service independent from the Analytics Service.

## Analytics Integration

The Product Service does not directly call the Analytics Service.

The integration is asynchronous:

```text
Product Service
      ↓
StockMovementEvent
      ↓
Transactional Outbox
      ↓
Kafka
      ↓
Analytics Service
```

The Analytics Service receives the event and uses it to maintain its analytical stock information.

The Product Service therefore does not need to know whether Analytics is currently available.

## Order Service Integration

The Order Service communicates with Product Service through internal stock operations.

Before processing an order, the Order Service can request product or pack information to verify the required inventory.

After the order is processed, it can request stock increases or decreases through the internal API.

The internal operations include:

```text
Product stock decrease
Product stock increase
Pack stock decrease
Pack stock increase
Product lookup
Pack lookup
```

These operations are separate from user-generated stock movements.

The Product Service therefore has two distinct stock modification contexts:

```text
User Stock Movement
        ↓
Product Service
        ↓
StockMovementEvent
        ↓
Kafka
```

and:

```text
Order Processing
        ↓
Order Service
        ↓
Internal Product Service API
        ↓
Stock modification
```

The latter does not generate the Product Service `StockMovementEvent`.

## Security

The Product Service uses stateless Spring Security and validates JWT tokens independently.

The API Gateway also validates JWTs before routing external requests to the service, providing two security layers.

The authorization rules are:

```text
GET /products/**
GET /categories/**
GET /packs/**
        ↓
Authenticated users
```

Product, category, and pack creation, modification, and deletion require the `ADMIN` role.

```text
POST /products/**
POST /categories/**
POST /packs/**

PUT /products/**
PUT /categories/**
PUT /packs/**

DELETE /products/**
DELETE /categories/**
DELETE /packs/**
        ↓
ADMIN
```

The health endpoint is publicly accessible.

Internal stock endpoints are handled separately from the normal authenticated CRUD endpoints because they are intended for service-to-service communication.

## Internal Service Communication

The Order Service communicates with Product Service through dedicated internal endpoints under:

```text
/internal/**
```

These endpoints support inventory operations required during order processing.

Access to these endpoints is protected by an internal service authentication mechanism.

The service also propagates the `X-Trace-Id` header.

If a request does not contain a trace ID, Product Service generates a new UUID and stores it in the logging context.

This allows requests and service interactions to be correlated through logs.

## Failure Handling

### Kafka unavailable

If Kafka becomes unavailable after a stock movement, the database transaction has already persisted both the stock change and the Outbox event.

The event therefore remains in the Outbox.

```text
Stock updated
      ↓
Outbox saved
      ↓
Kafka unavailable
      ↓
Event remains in Outbox
      ↓
Retry
      ↓
Kafka available
      ↓
Event published
```

The stock operation does not depend on Kafka being immediately available.

### Analytics unavailable

If Analytics is unavailable after the event has been published to Kafka, Product Service continues operating normally.

The event remains in Kafka until Analytics is able to consume it.

```text
Product Service
      ↓
Kafka
      ↓
Analytics unavailable
      ↓
Event remains in Kafka
      ↓
Analytics recovers
      ↓
Event consumed
```

This decouples inventory operations from analytical processing.

### Failed Outbox publication

Outbox publication attempts are retried.

After three unsuccessful attempts, the event is marked as failed and DLT/DLQ handling is used to prevent an indefinitely failing event from blocking normal processing.

## Event Identification

Every stock movement event contains a UUID `eventId`.

This identifier provides a unique identity for each event and allows downstream consumers such as Analytics Service to detect and prevent duplicate event processing.

The Product Service generates the event identifier when creating the stock movement event.

## Testing

The Product Service includes tests covering its main business operations and failure cases.

The test suite covers:

* Product creation.
* Product updates.
* Product deletion.
* Product search.
* `RESTOCK` movements.
* `RETURN` movements.
* `CORRECTION` movements.
* Insufficient stock validation.
* Stock decrease operations.
* Outbox event creation and related behavior.

One important stock validation test verifies that attempting to create a `LOSS` greater than the available stock:

```text
LOSS > available stock
        ↓
IllegalArgumentException
        ↓
Stock remains unchanged
        ↓
No new Outbox event
```

## Docker and Deployment

Product Service is containerized using Docker.

In the deployed architecture, it is an internal microservice and does not need to expose its application port directly to the public internet.

External HTTP traffic should enter through the API Gateway.

The service communicates with other components through the internal Docker/AWS network, including:

* Eureka.
* Kafka.
* MySQL.
* API Gateway.
* Order Service.
* Analytics Service.

This keeps the Product Service isolated from direct external access.

## Configuration

Environment-specific values are externalized from the application code.

The service requires configuration for its infrastructure dependencies and security settings, such as:

* Database connection.
* Kafka connection.
* Eureka connection.
* JWT configuration.
* Internal service authentication.

Secrets and environment-specific credentials are not stored in the source code.

## Architecture Role

The Product Service is the inventory and product management component of the system.

It owns product, category, pack, and stock operations while remaining independent from the analytical storage maintained by Analytics Service.

Its main architectural responsibilities are:

```text
                ┌─────────────────┐
                │   API Gateway   │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ Product Service │
                └───────┬─┬───────┘
                        │ │
              ┌─────────┘ └──────────┐
              ▼                      ▼
        ┌──────────┐           ┌──────────┐
        │  MySQL   │           │  Kafka   │
        └──────────┘           └────┬─────┘
                                    │
                                    ▼
                            ┌───────────────┐
                            │   Analytics   │
                            │    Service    │
                            └───────────────┘
```

The service follows a transactional and event-driven approach for stock movements. Database changes are persisted together with their corresponding Outbox events, while Kafka provides asynchronous communication with the Analytics Service.

This allows inventory operations to remain available even when downstream analytical processing or Kafka is temporarily unavailable.
