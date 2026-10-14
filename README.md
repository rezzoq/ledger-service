# Ledger Service

A small money-transfer service built around the core ideas of a financial ledger:

## Features

- **Double-entry ledger**: every transfer produces two immutable entries (debit and credit) that sum to zero
- **Idempotency**: repeated requests with the same `idempotencyKey` return `DUPLICATE` and never move money twice
- **Atomic transfers**: balances and ledger entries are written in a single transaction, so either everything is saved or nothing is
- **Optimistic locking**: `@Version` on accounts prevents lost updates
- **Schema migrations** with Flyway, DB-level constraints (`balance >= 0`, unique idempotency key)
- Money stored as `DECIMAL(19, 4)`, never floating point

## Stack

Java 21, Spring Boot 3, Spring Data JPA, MySQL, Flyway, Docker Compose

## Run locally

```bash
cp .env.example .env
docker compose up -d
./mvnw spring-boot:run
```

## API

Create an account:

```http
POST /api/accounts
{"name": "A", "balance": 1000}
```

Transfer money:

```http
POST /api/transfers
{"fromAccId": 1, "toAccId": 2, "amount": 300, "idempotencyKey": "k1"}
```

Response: `{"transferId": 1, "status": "OK"}`, or `"DUPLICATE"` on a repeated key.

## Roadmap

- [ ] Concurrency tests (100 parallel transfers on a hot account)
- [ ] Retry on optimistic lock failures / compare locking strategies
- [ ] Metrics with Prometheus and Grafana
- [ ] Proper error responses (400, 409, 422)
- [ ] Balance reconciliation job
