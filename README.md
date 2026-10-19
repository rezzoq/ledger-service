# Ledger Service

A small money-transfer service built around the core ideas of a financial ledger:
double-entry bookkeeping, idempotent operations and safe concurrent updates.
The repository history follows the work: a baseline, a load test that exposed its
limits, and a fix that was measured against it.

## Features

- **Double-entry ledger**: every transfer writes two immutable entries (debit and credit) that sum to zero
- **Idempotency**: repeated requests with the same `idempotencyKey` return `DUPLICATE` and never move money twice
- **Atomic transfers**: balances and ledger entries are saved in one transaction
- **Pessimistic locking with ordered acquisition**: accounts are locked (`SELECT ... FOR UPDATE`) in ascending id order, so opposite transfers (A to B and B to A) cannot deadlock
- **Optimistic locking** (`@Version`) as a second safety net
- **Error mapping**: 409 on concurrency conflicts, 422 on insufficient funds, 400 on bad input
- **Metrics**: Spring Boot Actuator and Micrometer, scraped by Prometheus (:9090), visualised in Grafana (:3001)
- Flyway migrations, DB-level constraints (`balance >= 0`, unique idempotency key), money as `DECIMAL(19, 4)`

## Stack

Java 21, Spring Boot 3, Spring Data JPA, MySQL, Flyway, Docker Compose, Prometheus, Grafana

## Run locally

```bash
cp .env.example .env        # set your own password
docker compose up -d        # MySQL, Prometheus, Grafana
mvn spring-boot:run
```

## API

```http
POST /api/accounts
{"name": "A", "balance": 1000}

GET /api/accounts/1

POST /api/transfers
{"fromAccId": 1, "toAccId": 2, "amount": 300, "idempotencyKey": "k1"}
```

Response: `{"transferId": 1, "status": "OK"}`, or `"DUPLICATE"` on a repeated key.

## Concurrency experiments

`ConcurrentTransferLoadIT` sends 500 transfers from 50 parallel threads.
It needs the app and MySQL running, so run it from the IDE (it is skipped by `mvn package`).
Everything ran on one local machine with MySQL in Docker.

| Scenario | Result |
|---|---|
| One hot account, optimistic locking only | 50 of 500 succeeded, the rest returned 409 (single cold run) |
| One hot account, pessimistic locking | 500 of 500 succeeded, median 4.6 s over 3 runs after 1 warm-up run |
| Opposite transfers (A to B, B to A), locks in arbitrary order | 249 of 500 succeeded, 251 failed with `Deadlock found` |
| Opposite transfers, locks in id order | 500 of 500 succeeded |

After every run the tests check that no money was lost: balances add up exactly.
The optimistic-locking row is a single run without warm-up, so its time is not comparable
with the other rows. What matters there is the success rate.

## Roadmap

- [x] Concurrency tests (hot account, opposite transfers)
- [x] Pessimistic locking with ordered lock acquisition
- [x] Prometheus and Grafana
- [x] Error responses (400, 409, 422)
- [ ] Test concurrent requests with the same idempotency key
- [ ] Retry-on-conflict variant under the same load
- [ ] Balance reconciliation job