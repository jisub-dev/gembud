# Changelog — 2026-04

## 2026-04-25 — Reverse Proxy + TLS (Caddy)

- Added Caddy as reverse proxy in docker-compose.prod.yml; backend/frontend now internal-only
- Auto-TLS via Let's Encrypt requires `DOMAIN` and `ACME_EMAIL` env vars
- HSTS (1 year), gzip/zstd compression, security headers (X-Content-Type-Options, X-Frame-Options, Referrer-Policy)
- Request body size capped at 10MB
- WebSocket and OAuth2 paths explicitly proxied
