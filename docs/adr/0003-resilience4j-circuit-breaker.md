# ADR 0003: Resilience4j Circuit Breaker for Outbound HTTP Calls

**Date:** 2026-04-27  
**Status:** Accepted

## Context

The backend makes outbound HTTP calls to Slack webhooks for security alerting. Previously, `SlackAlertService` implemented a custom in-memory `PriorityQueue`-based retry scheduler with exponential backoff. This approach had several drawbacks:

- Thread synchronization was manual (`synchronized` blocks on the queue)
- Retry state was lost on restart — inflight alerts were silently dropped
- No circuit-breaker: a flapping Slack endpoint would saturate threads with retries
- Metrics were ad-hoc `AtomicLong` counters not wired into Micrometer

## Decision

Replace the hand-rolled retry mechanism with [Resilience4j](https://resilience4j.readme.io/) (`resilience4j-spring-boot3:2.2.0`).

`SlackAlertService.doSend()` is annotated with:
- `@CircuitBreaker(name = "slack", fallbackMethod = "sendFallback")` — opens after 50% failure rate over 10 calls (min 5), waits 60 s in OPEN state, then probes with 3 calls in HALF-OPEN
- `@Retry(name = "slack")` — 3 attempts with 2 s / 6 s / 18 s exponential backoff

Resilience4j is backed by `resilience4j-micrometer`, so circuit state and retry counts are automatically exposed via Prometheus at `/actuator/metrics`.

## Consequences

**Good:**
- No more manual synchronization or custom retry queues
- Circuit breaker prevents thread pile-up during Slack outages
- All metrics are Micrometer-native and visible in existing Prometheus scrape
- `@Retry` annotations can be applied to future outbound calls (OAuth2 token refresh, etc.) without new infrastructure

**Bad:**
- Retry state is still in-memory; an alert in a retry window is dropped on restart. For security alerts this is acceptable — the `SecurityEvent` entity persists the event to the database regardless of whether the Slack notification succeeds.
- Adds `spring-boot-starter-aop` to the classpath (needed for proxy-based annotations); this is a light dependency already used transitively by Spring Security.

## Alternatives Considered

| Option | Reason rejected |
|--------|----------------|
| Keep hand-rolled retry | No circuit-breaker semantics; harder to maintain |
| Spring Retry (`@Retryable`) | No circuit breaker; Resilience4j is the Spring Boot 3 standard |
| Persistent message queue (Redis Streams) | Overkill for a non-critical notification path; adds operational complexity |
