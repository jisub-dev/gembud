# Changelog — 2026-04

## 2026-04-26 — Sentry Error Tracking

- Backend: sentry-spring-boot-starter-jakarta + sentry-logback wired into prod profile
- Frontend: @sentry/react with ErrorBoundary at App root
- Disabled by default (empty DSN) — set SENTRY_DSN/VITE_SENTRY_DSN env vars to enable
- 10% trace sampling, send-default-pii=false (GDPR-friendly)
- Frontend Sentry config requires VITE_* env vars at BUILD time (Dockerfile ARGs)

## 2026-04-26 — Actuator + Structured JSON Logging

- Added spring-boot-starter-actuator with prometheus registry
- Exposed /actuator/health (public), /actuator/info (public), others require ROLE_ADMIN
- Liveness/readiness probes split (K8s-ready): liveness=livenessState, readiness=db+redis+readinessState
- Graceful shutdown enabled (20s phase timeout)
- logback-spring.xml: JSON to stdout in prod, plain text in dev/test
- docker-compose.prod.yml: backend healthcheck via /actuator/health/liveness

## 2026-04-25 — Reverse Proxy + TLS (Caddy)

- Added Caddy as reverse proxy in docker-compose.prod.yml; backend/frontend now internal-only
- Auto-TLS via Let's Encrypt requires `DOMAIN` and `ACME_EMAIL` env vars
- HSTS (1 year), gzip/zstd compression, security headers (X-Content-Type-Options, X-Frame-Options, Referrer-Policy)
- Request body size capped at 10MB
- WebSocket and OAuth2 paths explicitly proxied
