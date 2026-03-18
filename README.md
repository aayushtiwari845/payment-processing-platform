# Payment Processing Platform

Repository scaffold for an event-driven payment platform built around Spring Boot services, Kafka, PostgreSQL, Redis, and a Python-based fraud detection service.

## What is included now

- Week 1 baseline repository layout
- Local git repository initialized in this workspace
- Multi-module Maven parent build
- `account-service` CRUD skeleton with Redis cache hooks
- `transaction-service` pending-review flow with idempotency handling, fraud callback settlement, and Kafka events
- Flyway migrations owned by each Java service
- `api-gateway` route skeleton
- `notification-service` Kafka consumer with mock email delivery retries
- `fraud-detection-service` FastAPI starter
- Prometheus-ready metrics across Java services plus `/metrics` for fraud detection
- auto-provisioned Grafana datasource and starter dashboard
- persisted fraud decision metadata on transactions for auditability
- Multi-stage Dockerfiles for the application services
- Docker Compose infrastructure for PostgreSQL, Redis, ZooKeeper, Kafka, and Kafka UI
- Local docs that convert the pasted checklist into repo artifacts

## Repository layout

```text
.
|-- docs/
|   |-- checklists/
|   `-- setup/
|-- infrastructure/
|   |-- docker/
|   `-- kubernetes/
|-- services/
|   |-- account-service/
|   |-- api-gateway/
|   |-- fraud-detection-service/
|   |-- notification-service/
|   `-- transaction-service/
`-- pom.xml
```

## Quick start

1. Install Maven or generate Maven wrappers for the repo.
2. Fix the local Python launcher issue or install a working `python` on `PATH`.
3. Start shared infrastructure:

```bash
docker compose -f infrastructure/docker/docker-compose.yml up -d
```

To build and run the app stack inside containers:

```bash
docker compose -f infrastructure/docker/docker-compose.yml up -d --build
```

Prometheus is exposed at `http://localhost:9090` once the stack is running.
Grafana is exposed at `http://localhost:3000` with `admin` / `admin`.

4. Run Java services from the repo root after Maven is available:

```bash
mvn -pl services/account-service spring-boot:run
mvn -pl services/transaction-service spring-boot:run
mvn -pl services/api-gateway spring-boot:run
mvn -pl services/notification-service spring-boot:run
```

5. Run the fraud service after Python is working:

```bash
cd services/fraud-detection-service
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

## Immediate next steps

1. Install Maven and a working Python interpreter.
2. Add integration tests for REST, Kafka, and Postgres flows.
3. Generate Maven wrappers if you want repo-local commands like `./mvnw`.
4. Add authentication and RBAC at the gateway boundary.
5. Decide whether the remaining services should be Java or Node.js before Week 2 and Week 3 expansion.
