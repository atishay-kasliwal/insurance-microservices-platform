# Insurance Microservices Platform

> **Built by Atishay Kasliwal**
> End-to-end insurance data transfer platform between third-party providers and a payment gateway, built on event-driven microservices architecture.

---

## About

This project is a production-grade, distributed microservices system designed for the insurance industry. It handles real-time policy data ingestion, transformation, querying, and analytics — with every service communicating asynchronously through **Apache Kafka**. The platform integrates third-party insurance providers and a payment gateway, ensuring fault-tolerant, scalable data transfer across loosely coupled services.

The architecture follows **CQRS** and **Event Sourcing** patterns: write-side services publish events to Kafka, and read-side services consume and index those events into Elasticsearch for fast querying.

---

## Architecture

```
[Insurance Data Source]
        │
        ▼
[twitter-to-kafka-service]  ──Avro──►  [Kafka Cluster (3 brokers)]
                                                │
                        ┌───────────────────────┤
                        │                       │
                        ▼                       ▼
          [kafka-to-elastic-service]   [kafka-streams-service]
                        │               (State Store / Analytics)
                        ▼
              [Elasticsearch Cluster]
                        │
                        ▼
           [elastic-query-service]  ◄──── REST API (HATEOAS + Swagger)
                        │
                        ▼
         [elastic-query-web-client]  ◄──── Thymeleaf UI (Bootstrap)
                        │
         ┌──────────────┴──────────────┐
         ▼                             ▼
  [gateway-service]           [discovery-service]
  JWT Auth + Rate Limiter       Netflix Eureka
  Redis + Circuit Breaker
         │
         ▼
  [Prometheus + Grafana + ELK]
  Full Observability Stack
```

---

## Services

| Service | Port | Description |
|---|---|---|
| `config-server` | 8888 | Centralized Spring Cloud Config Server — all services fetch config from here |
| `discovery-service` | 8761 | Netflix Eureka Server — service registry for dynamic discovery and load balancing |
| `gateway-service` | 9090 | Spring Cloud API Gateway — JWT authentication, Redis rate limiting, circuit breaker |
| `twitter-to-kafka-service` | — | Ingests insurance data events and publishes to Kafka using Avro serialization |
| `kafka-to-elastic-service` | — | Consumes Kafka events and indexes them into Elasticsearch |
| `kafka-streams-service` | 8187 | Kafka Streams topology with persistent state store — real-time policy analytics |
| `elastic-query-service` | 8183 | REST API for querying Elasticsearch — HATEOAS, Swagger/OpenAPI v3, API versioning |
| `elastic-query-web-client` | 8184 | Reactive Thymeleaf web UI — calls elastic-query-service via WebClient |

---

## Tech Stack

**Backend**
- Java 11, Spring Boot 2.6.3, Spring Cloud 2021.0.0
- Apache Kafka (3-broker cluster), Apache Avro, Confluent Schema Registry
- Elasticsearch 7.15, Spring Data Elasticsearch
- Kafka Streams (state store with interactive queries)
- Spring Cloud Config Server (externalized configuration)

**Security**
- JWT authentication via Spring Cloud API Gateway (`JwtTokenProvider`, `JwtAuthenticationFilter`)
- BCrypt password encoding with in-memory user authentication on each service
- Spring Security on config-server, elastic-query-service, elastic-query-web-client, and discovery-service
- Per-IP Redis rate limiting (token bucket: 5 req/s, burst 10) on all gateway routes

**Infrastructure & Observability**
- Docker + Docker Compose (multi-container orchestration)
- Netflix Eureka (service discovery + client-side load balancing)
- Redis (rate limiter backing store)
- Resilience4j Circuit Breaker on gateway routes
- Prometheus (metrics scraping from all `/actuator/prometheus` endpoints)
- Grafana (auto-provisioned Prometheus datasource, visualisation dashboards)
- Zipkin (distributed tracing via Spring Cloud Sleuth)
- Logstash + Kibana (structured JSON logs via `logstash-logback-encoder`)

**API Design**
- HATEOAS (Spring HATEOAS) on elastic-query-service
- API versioning via Accept header / content negotiation
- OpenAPI v3 documentation via Springdoc (Swagger UI at `/swagger-ui.html`)

---

## Security Flow

```
Client
  │
  ▼
POST /auth/login  ──►  gateway-service  ──►  validates credentials
                                        ◄──  returns JWT (HS512, 24h expiry)
  │
  ▼
GET /elastic-query-service/**
  Authorization: Bearer <token>
  │
  ▼
JwtAuthenticationFilter (GlobalFilter, Order=-2)
  ├── validates JWT signature + expiry
  ├── extracts username + role
  ├── injects X-Auth-User / X-Auth-Role headers
  └── forwards to downstream service
```

---

## Getting Started

### Prerequisites
- Docker and Docker Compose
- Java 11+
- Maven 3.8+

### Run with Docker Compose

```bash
# Start infrastructure (Kafka, Elasticsearch, Zookeeper, Schema Registry)
docker-compose -f docker-compose/kafka_cluster.yml up -d
docker-compose -f docker-compose/elastic_cluster.yml up -d

# Start all microservices
docker-compose -f docker-compose/services.yml up -d

# Start monitoring stack (Prometheus, Grafana, Zipkin, Redis, Gateway, Discovery)
docker-compose -f docker-compose/monitoring.yml up -d
```

### Authenticate and Query

```bash
# 1. Get a JWT token
curl -X POST http://localhost:9090/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'

# 2. Query insurance policies via the gateway
curl http://localhost:9090/elastic-query-service/v1/documents \
  -H "Authorization: Bearer <your_token>"

# 3. Check real-time policy analytics (Kafka Streams state store)
curl http://localhost:9090/kafka-streams-service/kafka-streams/insurance/policy-counts/LIFE \
  -H "Authorization: Bearer <your_token>"
```

### Monitoring

| Tool | URL | Credentials |
|---|---|---|
| Eureka Dashboard | http://localhost:8761 | guest / guest |
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | — |
| Zipkin | http://localhost:9411 | — |
| Swagger UI | http://localhost:8183/elastic-query-service/swagger-ui.html | — |

---

## Configuration

All service configuration is externalized via **Spring Cloud Config Server**. Sensitive values (passwords, keys) are encrypted using Spring Cloud's symmetric encryption (`ENCRYPT_KEY`).

Key config files in `config-server-repository/`:
- `config-client.yml` — shared defaults across all services
- `config-client-twitter_to_kafka.yml` — ingestion service config

Set `ENCRYPT_KEY` environment variable before starting the config server.

---

## Project Structure

```
insurance-microservices-platform/
├── config-server/                  # Spring Cloud Config Server
├── discovery-service/              # Netflix Eureka Server
├── gateway-service/                # API Gateway + JWT + Redis rate limiter
├── twitter-to-kafka-service/       # Data ingestion → Kafka
├── kafka-to-elastic-service/       # Kafka consumer → Elasticsearch
├── kafka-streams-service/          # Kafka Streams state store + analytics API
├── elastic-query-service/          # REST query API (HATEOAS + Swagger)
├── elastic-query-web-client/       # Thymeleaf reactive web client
├── kafka/
│   ├── kafka-admin/                # Topic creation and admin client
│   ├── kafka-producer/             # Generic Avro Kafka producer
│   ├── kafka-consumer/             # Generic Kafka consumer config
│   └── kafka-model/                # Avro generated models
├── elastic/
│   ├── elastic-config/             # Elasticsearch client configuration
│   ├── elastic-model/              # Elasticsearch index models
│   ├── elastic-index-client/       # Index write client
│   └── elastic-query-client/       # Index query client
├── app-config-data/                # Shared @ConfigurationProperties beans
├── common-config/                  # Shared retry and common config beans
├── common-util/                    # Shared utilities
└── docker-compose/
    ├── kafka_cluster.yml           # Kafka + Zookeeper + Schema Registry
    ├── elastic_cluster.yml         # Elasticsearch cluster (3 nodes)
    ├── services.yml                # All microservices
    ├── monitoring.yml              # Prometheus, Grafana, Zipkin, Redis, Gateway
    └── prometheus.yml              # Prometheus scrape config
```

---

## Completed Features

- [x] Elastic query service — REST API with HATEOAS, Swagger, API versioning
- [x] Reactive elastic web client service — Thymeleaf UI with WebClient
- [x] Security — JWT auth on API gateway, BCrypt in-memory auth on services, Redis rate limiting
- [x] Kafka Streams state store — real-time insurance policy analytics with interactive queries
- [x] Netflix Eureka discovery service — dynamic service registration and load balancing
- [x] Spring API Gateway & Redis rate limiter — token bucket rate limiting, circuit breaker
- [x] Monitored microservices — Prometheus metrics, Grafana dashboards, Zipkin tracing, ELK structured logging

## In Progress

- [ ] Postgres analytics store — persist Kafka Streams aggregations to PostgreSQL for historical reporting
- [ ] OAuth2 / Keycloak — replace in-memory auth with a full authorization server

---

## License

MIT
