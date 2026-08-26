# Wallet Service

A production-ready REST API for managing digital wallets and money transfers between them, built with Spring Boot.

[![CI](https://github.com/indalec/wallet-service/actions/workflows/ci.yml/badge.svg)](https://github.com/indalec/wallet-service/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-23-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8.4-blue)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED)

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture & Decisions](#architecture--decisions)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [Running with Docker Compose (Recommended)](#running-with-docker-compose-recommended)
  - [Running Locally without Docker](#running-locally-without-docker)
- [API Reference](#api-reference)
  - [Wallets](#wallets)
  - [Transfers](#transfers)
- [Testing](#testing)
- [Monitoring & Observability](#monitoring--observability)
- [Production Scenario: Investigating Latency Spikes](#production-scenario-investigating-latency-spikes)
- [Future Improvements](#future-improvements)
- [License](#license)

---

## Overview

**Wallet Service** is a RESTful backend service that provides:

- **Wallet management** — create wallets, retrieve wallet details, and check balances.
- **Money transfers** — move funds between wallets with full atomicity, idempotency, and concurrency control.

The service is designed with production-grade concerns in mind: **consistency**, **reliability**, **observability**, and **easy operation** via Docker Compose.

This project was developed as a learning exercise to explore Spring Boot, JPA, Docker, and monitoring stacks in a realistic backend scenario.

---

## Features

### Core Functionality

- ✅ Create a wallet with an owner name, currency, and initial balance
- ✅ Retrieve wallet details by ID
- ✅ Check wallet balance
- ✅ Transfer money between two wallets
- ✅ Automatic validation of business rules:
  - Amount must be greater than zero
  - Source and destination wallets must exist
  - Wallet cannot transfer to itself
  - Source wallet must have sufficient funds
  - Currencies must match
- ✅ Atomic transactions (`@Transactional`)
- ✅ Idempotency via `idempotencyKey` to prevent duplicate processing
- ✅ Concurrency control with pessimistic locking (`SELECT ... FOR UPDATE`)

### Production Ready

- ✅ OpenAPI / Swagger UI documentation
- ✅ Health and readiness probes (Spring Boot Actuator)
- ✅ Prometheus metrics endpoint (`/actuator/prometheus`)
- ✅ Structured logging for audits and debugging
- ✅ Docker Compose setup (app + MySQL + Prometheus + Grafana)
- ✅ CI pipeline with GitHub Actions (runs tests against MySQL)

---

## Architecture & Decisions

### Application Structure

The application follows a **layered architecture**:

- **Controller layer** — REST endpoints (validation, request/response mapping)
- **Service layer** — business logic
- **Repository layer** — JPA for data persistence
- **Exception handling** — global advice with meaningful HTTP status codes

### Transaction Consistency

Transfer operations are wrapped in a `@Transactional` method (`TransferTransactionService.executeTransfer`). This ensures that either **all changes are committed** (both wallets updated + transfer record saved) or **none are**, preserving atomicity.

### Concurrency Handling

To prevent race conditions when multiple transfers target the same wallet concurrently, the repository uses **pessimistic locking**:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT w FROM Wallet w WHERE w.id = :id")
Optional<Wallet> findByIdForUpdate(UUID id);
```

This acquires a row-level lock on the wallet records, ensuring that two concurrent transfers cannot both read the same balance and overspend. The test `shouldNotAllowTwoConcurrentTransfersToOverspendWallet` verifies this behavior.

### Idempotency (Duplicate Request Handling)

Clients may retry requests if they don't receive a response. To prevent the same logical transfer from being executed twice, the API requires an `idempotencyKey` field in the request body.

- The `Transfer` entity has a **unique constraint** on `idempotencyKey`.
- Before processing, the service checks if a transfer with that key already exists and returns it immediately.
- If a concurrent request attempts to insert the same key, a `DataIntegrityViolationException` triggers a fallback lookup.

This approach guarantees that **each logical transfer is processed exactly once**, even under retries or concurrent requests with the same key.

### Trade-offs

1. **Pessimistic locking vs. Optimistic locking**  
   Pessimistic locking was chosen for its simplicity and guaranteed correctness under high contention. While it can reduce throughput compared to optimistic locking with retries, it avoids complex retry logic and is easier to reason about for a financial transfer service.

2. **Idempotency key storage in the same table**  
   Storing the idempotency key in the `transfers` table (with a unique constraint) keeps the design simple and leverages the database's ACID guarantees. An alternative would be a separate idempotency store (e.g., Redis), but that would add operational complexity unnecessary for this scope.

3. **DDL auto-update**  
   Using `spring.jpa.hibernate.ddl-auto=update` simplifies local development but is not recommended for production. In a real deployment, we would use a migration tool like Flyway or Liquibase.

---

## Technology Stack

| Component         | Technology                     |
| ----------------- | ------------------------------ |
| Language          | Java 23                        |
| Framework         | Spring Boot 4.1.1              |
| Persistence       | Spring Data JPA + Hibernate    |
| Database          | MySQL 8.4                      |
| API Documentation | Springdoc OpenAPI (Swagger UI) |
| Metrics           | Micrometer + Prometheus        |
| Monitoring        | Grafana                        |
| Containerization  | Docker + Docker Compose        |
| CI                | GitHub Actions                 |
| Build Tool        | Maven                          |

---

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java 23** (or later)
- **Docker** and **Docker Compose** (recommended)
  - *On Windows*: Docker Desktop requires **WSL2** to be installed and enabled. The project has been tested on Windows with the WSL2 backend.
- **Git** (to clone the repository)
- **Maven** (only if running without Docker)

---

## Getting Started

### Running with Docker Compose (Recommended)

This is the easiest way to run the entire stack (application + MySQL + Prometheus + Grafana).

1. **Clone the repository:**

   ```bash
   git clone https://github.com/indalec/wallet-service.git
   cd wallet-service
   ```

2. **Start all services:**

   ```bash
   docker-compose up -d
   ```

   

   This command starts:
   - MySQL on port `3307`
   - Wallet Service on port `8080`
   - Prometheus on port `9090`
   - Grafana on port `3000`

3. **Verify the application is running:**

   - Health check: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
   - Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
   - Prometheus: [http://localhost:9090](http://localhost:9090)
   - Grafana: [http://localhost:3000](http://localhost:3000) (default credentials: `admin` / `admin`)

4. **Stop all services:**

   ```bash
   docker-compose down
   ```

   To also remove volumes (database data):

   ```bash
   docker-compose down -v
   ```

   

### Running Locally without Docker

If you prefer to run the application directly on your host machine:

1. **Start a MySQL instance** (e.g., via Docker):

   ```bash
   docker run -d --name mysql-wallet -p 3306:3306 -e MYSQL_ROOT_PASSWORD=admin -e MYSQL_DATABASE=wallet_service mysql:8.4
   ```

2. **Update `application.properties`** if needed (default credentials are `root`/`admin`).

3. **Build and run the application:**

   ```bash
   ./mvnw clean package
   java -jar target/wallet-service-0.0.1-SNAPSHOT.jar
   ```

4. The application will be available at `http://localhost:8080`.

---

## API Reference

### Wallets

#### Create a Wallet

```http
POST /wallets
```

**Request body:**

```json
{
  "ownerName": "Alice",
  "currency": "EUR",
  "balance": 100.00
}
```

**Response:** `201 Created` with the created wallet object (including generated UUID).

#### Get Wallet by ID

```http
GET /wallets/{id}
```

**Response:** `200 OK` with wallet details.

#### Get Wallet Balance

```http
GET /wallets/{id}/balance
```

**Response:** `200 OK` with the balance as a number.

---

### Transfers

#### Create a Transfer

```http
POST /transfers
```

**Request body:**

```json
	{
  "sourceWalletId": "550e8400-e29b-41d4-a716-446655440000",
  "destinationWalletId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "amount": 25.50,
  "currency": "EUR",
  "idempotencyKey": "unique-client-generated-key-123"
}
```

**Response:** `200 OK` with the transfer details (including status, timestamps, and generated ID).

**Error responses:**

| Status                      | Description                                             |
| --------------------------- | ------------------------------------------------------- |
| `400 Bad Request`           | Invalid input (negative amount, missing fields, etc.)   |
| `400 Bad Request`           | Insufficient funds, currency mismatch, or self-transfer |
| `404 Not Found`             | Source or destination wallet not found                  |
| `409 Conflict`              | Concurrent modification (retry recommended)             |
| `500 Internal Server Error` | Unexpected error                                        |

---

## Testing

The project includes both **unit tests** and **integration tests**.

### Run all tests

```bash
./mvnw test
```



### Run only unit tests

```bash
./mvnw test -Dtest="*Test"
```



### Run only integration tests

```bash
./mvnw test -Dtest="*IntegrationTest"
```

**Key tests:**

- `TransferServiceTest` — unit tests covering all business rules (Mockito)
- `TransferIntegrationTest` — end-to-end tests with a real MySQL database, covering:
  - Successful transfer and balance persistence
  - Concurrent transfers preventing overspending
  - Idempotency with repeated `idempotencyKey`
  - Concurrent requests with the **same** idempotency key (only one execution)

---

## Monitoring & Observability

The application is instrumented for production observability:

| Endpoint               | Description                                                  |
| ---------------------- | ------------------------------------------------------------ |
| `/actuator/health`     | Health and readiness probes (includes database connectivity) |
| `/actuator/info`       | Application information                                      |
| `/actuator/metrics`    | List of available metrics                                    |
| `/actuator/prometheus` | Prometheus-formatted metrics (scraped by Prometheus)         |

### Metrics Pipeline

### Metrics Pipeline

    Wallet Service (/actuator/prometheus)
            ↓
      Prometheus (scrapes every 5s)
            ↓
        Grafana (visualization)


- **Prometheus** is configured to scrape metrics from the service every 5 seconds.
- **Grafana** is pre-configured with a Prometheus data source (`http://prometheus:9090`). You can import or create dashboards to visualize JVM metrics, request rates, latency, and more.

To access Grafana: [http://localhost:3000](http://localhost:3000) (default: `admin`/`admin`).

---

## Production Scenario: Investigating Latency Spikes

> *"A few weeks after deployment, monitoring shows that some transfer requests occasionally take 5–10 seconds instead of the usual 100 ms. How would you investigate the problem?"*

### Step 1: Gather Data from Monitoring

- **Check Grafana dashboards** for:
  - **Request latency** (p95, p99) — identify if the spike is isolated to specific endpoints (e.g., `POST /transfers` vs `GET /wallets`).
  - **Database query duration** — if the database is the bottleneck.
  - **JVM metrics** — GC pauses, heap usage, thread count.
  - **Error rates** — correlate with latency spikes.

- **Check Prometheus** for:
  - `http_server_requests_seconds` — endpoint latency histograms.
  - `jvm_gc_pause_seconds` — garbage collection pauses.
  - `hikaricp_connection_timeout_total` — connection pool exhaustion.

### Step 2: Examine Logs

- Look for **WARN** or **ERROR** logs during the time windows of the spikes.
- Check for:
  - Database lock contention warnings (pessimistic locking timeouts).
  - Slow query logs from MySQL.
  - Any exceptions or retries being logged.

### Step 3: Correlate with Infrastructure

- Check **CPU / memory usage** of the container (Docker stats or Kubernetes metrics).
- Verify **network latency** between the application and the database.
- Check if the database is under load from other services (if shared).

### Step 4: Deep Dive

- **Enable slow query logging** in MySQL to capture queries that exceed a threshold.
- **Add distributed tracing** (e.g., Micrometer Tracing + Zipkin) to see where time is spent across service boundaries.
- If the issue is intermittent, **increase logging verbosity** temporarily for the affected endpoints.
- Consider **profiling** the application in a non-production environment to reproduce the issue.

### Step 5: Remediation

Based on findings, possible actions include:

- **Database**: Add missing indexes, optimize queries, or increase connection pool size.
- **Application**: Review locking strategies (e.g., consider optimistic locking with retries for lower contention).
- **Infrastructure**: Scale horizontally (more replicas) or vertically (more resources).
- **Caching**: Introduce caching for read-heavy endpoints (e.g., wallet balance).

---

## Future Improvements

If the service needs to handle significantly more traffic, the following changes would be considered:

1. **Database scaling**
   - Read replicas for wallet queries.
   - Sharding by wallet ID to distribute write load.
   - Use a connection pool with higher limits.

2. **Caching**
   - Cache wallet balances (with short TTL) to reduce database reads.
   - Use Redis for distributed caching.

3. **Asynchronous processing**
   - Offload transfer processing to a message queue (e.g., RabbitMQ, Kafka) for non-critical transfers.
   - Notify clients via webhooks or polling.

4. **Optimistic locking with retries**
   - Replace pessimistic locking with optimistic locking (`@Version`) + retry logic to improve throughput under low contention.

5. **Separate idempotency store**
   - Use Redis for idempotency keys to reduce database load.

6. **Observability**
   - Add distributed tracing (e.g., Zipkin, Jaeger).
   - Implement structured logging with correlation IDs.

---

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## Author

Indalecio Valenzuela

- GitHub: [indalec](https://github.com/indalec)  
- LinkedIn: [Indaleci](https://www.linkedin.com/in/daleci/)

---
