# SupportPilot

Event-driven customer support platform with AI triage. Spring Boot, Kafka, PostgreSQL/pgvector, Angular, Ollama (RAG).

> Work in progress. See [`SPEC.md`](SPEC.md) for the full specification and roadmap.

## Repository layout

| Folder | What it is |
| --- | --- |
| `ticket-service/` | Spring Boot service: users, tickets, real-time updates (port 8080) |
| `ai-triage-service/` | Spring Boot service: AI classification and suggested replies (port 8081) |
| `frontend/` | Angular app (Stage 2) |
| `infra/` | Infrastructure config (Postgres init scripts) |

## Running the infrastructure

Requires Docker.

```bash
docker compose up -d --wait
```

- PostgreSQL 17 + pgvector on `localhost:5432` (db/user/password: `supportpilot`)
- Kafka 4.3 in KRaft mode on `localhost:9092` (other containers use `kafka:29092`)

## Building the services

Requires Java 21+. No Maven install needed (Maven Wrapper).

```bash
cd ticket-service && ./mvnw verify
```

Integration tests use Testcontainers, so Docker must be running.

## Running the ticket-service locally

```bash
docker compose up -d --wait
cd ticket-service && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

On Windows PowerShell, run `cd ticket-service` first, then `.\mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"`
(PowerShell 5.1 has no `&&`, and the quotes stop it from splitting the `-D` argument).

The `dev` profile points to the Compose database and creates a demo agent (`agent@supportpilot.dev` / `agent12345`).
Outside `dev`, configuration comes from env vars: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`SPRING_DATASOURCE_PASSWORD` and `APP_JWT_SECRET` (base64, at least 32 bytes: `openssl rand -base64 32`).

### API

| Method | Path | Who |
| --- | --- | --- |
| POST | `/api/auth/register` | public (always creates a CUSTOMER) |
| POST | `/api/auth/login` | public, returns a JWT |
| GET | `/api/auth/me` | authenticated |
| POST | `/api/users/agents` | AGENT |
| POST | `/api/tickets` | CUSTOMER |
| GET | `/api/tickets?status=&category=&priority=&page=&size=&sort=` | own tickets (CUSTOMER) or all (AGENT) |
| GET | `/api/tickets/{id}` | owner or AGENT |
| PATCH | `/api/tickets/{id}/status` | AGENT |
| GET / POST | `/api/tickets/{id}/messages` | owner or AGENT |

Send the token as `Authorization: Bearer <token>`. Errors follow RFC 9457 (`application/problem+json`).
