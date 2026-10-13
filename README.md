# Ledger Service

A small money-transfer service built around the core ideas of a financial ledger:
double-entry bookkeeping and idempotent operations.

## Features

- Every transfer writes two immutable ledger entries (debit and credit) that sum to zero
- Idempotency: a repeated `idempotencyKey` returns `DUPLICATE` and never moves money twice
- Balances and ledger entries are saved in one transaction
- Optimistic locking (`@Version`), DB constraints (`balance >= 0`), Flyway migrations
- Money stored as `DECIMAL(19, 4)`

## Stack

Java, Spring Boot 3, Spring Data JPA, MySQL, Flyway, Docker Compose

## Run locally

```bash
cp .env.example .env
docker compose up -d
mvn spring-boot:run
```

## API

```http
POST /api/accounts
{"name": "A", "balance": 1000}

POST /api/transfers
{"fromAccId": 1, "toAccId": 2, "amount": 300, "idempotencyKey": "k1"}
```

## Roadmap

- [ ] Concurrency tests on a hot account
- [ ] Metrics with Prometheus and Grafana
- [ ] Proper error responses (400, 409, 422)
- [ ] Compare locking strategies under load