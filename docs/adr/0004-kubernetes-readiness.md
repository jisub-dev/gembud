# ADR 0004: Kubernetes Readiness

**Date:** 2026-04-27  
**Status:** Accepted

## Context

The current deployment target is a single Docker host behind Caddy (see infra commit `2e9a162`). As traffic grows the team will want to move to a managed Kubernetes cluster (EKS, GKE, or k3s self-hosted). This ADR documents the decisions already in place and the remaining gaps before the backend can run correctly on Kubernetes.

## What Is Already K8s-Ready

| Concern | Current state |
|---------|---------------|
| **Liveness / readiness probes** | `management.endpoint.health.probes.enabled=true` exposes `/actuator/health/liveness` and `/actuator/health/readiness` |
| **Graceful shutdown** | `server.shutdown=graceful` with 30 s drain window (commit `776ad45`) |
| **Structured logs** | JSON via `logstash-logback-encoder` — compatible with Fluentd/Loki sidecar |
| **Metrics** | Micrometer Prometheus endpoint at `/actuator/prometheus` — scrapeable by a `ServiceMonitor` |
| **Stateless auth** | HTTP-only cookie JWT, no server-side session state — safe to scale horizontally |
| **Redis externalized** | Single Redis instance, already external to the app container |

## Remaining Gaps

### 1. Secret management
All secrets are injected via environment variables (database URL, JWT secret, Slack webhook). On K8s these must come from `Secret` objects (or an external secrets operator like External Secrets Operator). **Action:** Add a sample `k8s/secret.yaml.example` and document required keys in the runbook.

### 2. Database connection pool sizing
`HikariCP` defaults (10 connections) must be tuned per-pod to avoid exhausting PostgreSQL connection limits. With N pods at 10 connections each the ceiling is hit quickly. **Action:** Set `spring.datasource.hikari.maximum-pool-size` to `5` and document the formula `max_connections = N_pods × pool_size + headroom`.

### 3. Redis client and CLUSTER mode
The current `LettuceConnectionFactory` is configured for standalone Redis. If the cluster moves to Redis Cluster or Redis Sentinel the config must switch. **Action:** Parameterise via `REDIS_MODE` env var in `RedisConfig`.

### 4. WebSocket sticky sessions
STOMP over SockJS requires that a client always reaches the same pod during the handshake phase (SockJS long-polling fallback). **Action:** Enable ingress session affinity (`nginx.ingress.kubernetes.io/affinity: cookie`) or switch to a WebSocket-aware load balancer that supports connection-level routing.

### 5. Flyway migration races
On rolling restart, multiple pods can start simultaneously and all attempt Flyway migrations. **Action:** Enable `spring.flyway.out-of-order=false` (already the default) and configure `spring.flyway.lock-retry-count` to a safe value (10); this is safe because Flyway uses the database-level advisory lock.

## Decision

Proceed with existing Docker/Caddy deployment. Treat this ADR as the authoritative checklist for a future K8s migration sprint. Items 1–5 above become the acceptance criteria for that sprint.

## Consequences

- No code changes required today — the ADR creates visibility and prevents rework.
- The team can migrate incrementally: secrets first (lowest risk), then connection pool tuning, then sticky sessions.
