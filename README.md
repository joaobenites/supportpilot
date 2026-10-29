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
