# Gembud Operations Runbook

Audience: on-call engineer with no prior project context.
Last updated: 2026-04-25 (PR 1.4)

This runbook covers deploy, rollback, environment configuration, common
failure modes, and how to interpret Slack security alerts. The current
production topology is a single-host deployment driven by
`docker-compose.prod.yml` (Postgres + Redis + Spring Boot backend +
Vite/Nginx frontend container). A reverse proxy / TLS termination layer
(Caddy) is planned but not yet present.

---

## 1. Deployment

### 1.1 Prerequisites

- Linux host with Docker Engine >= 24 and the `docker compose` v2 plugin.
- A populated `.env` file in the repo root. See section 2 for the full
  variable list — copy `.env.example` to `.env` and fill in real values.
- DNS A/AAAA record for the public hostname pointing at the host's
  public IP (once a TLS-terminating proxy lands).
- Outbound network access to:
  - `accounts.google.com` (Google OAuth)
  - `discord.com` (Discord OAuth)
  - the configured Slack webhook host (security alerts)
- Inbound: ports `${BACKEND_PORT}` (default 8080) and
  `${FRONTEND_PORT}` (default 3000) reachable from the proxy or the
  internet directly.

### 1.2 First-time deployment

```bash
# 1. Clone and enter repo
git clone https://github.com/<owner>/gembud.git
cd gembud

# 2. Configure environment
cp .env.example .env
$EDITOR .env  # fill in DB/Redis passwords, OAuth secrets, JWT secret

# 3. Build and start the stack
docker compose -f docker-compose.prod.yml up -d --build

# 4. Watch the backend boot (Flyway will run migrations on first start)
docker compose -f docker-compose.prod.yml logs -f backend
```

The first boot runs Flyway migrations against an empty database. If
Flyway fails, the backend container will exit. Read the logs, fix the
underlying issue, and let the container restart — Flyway is idempotent
for already-applied versions.

### 1.3 Update deployment

The standard "ship a new version" loop:

```bash
# 1. Pull the new code on the host
git fetch origin
git checkout <release-tag-or-commit>

# 2. Rebuild images and restart only what changed
docker compose -f docker-compose.prod.yml up -d --build

# 3. Verify health (see 1.4) and run the smoke script
bash scripts/smoke.sh https://<your-domain>
```

If you build images on a separate CI host and push them to a registry,
substitute `docker compose pull` for `--build`.

### 1.4 Verifying a healthy deployment

After `up -d` completes:

1. `docker compose -f docker-compose.prod.yml ps` — every service
   should be `Up (healthy)`. Postgres and Redis have built-in
   healthchecks. The backend container has a healthcheck that pings
   `http://localhost:8080/api/actuator/health`; this will fail until
   the actuator dependency lands (PR 1.2). Until then, treat the
   `unhealthy` state from the backend container as expected and rely
   on the smoke script and direct log inspection.
2. `bash scripts/smoke.sh https://<your-domain>` exits 0.
3. `docker compose -f docker-compose.prod.yml logs --tail=200 backend`
   shows no `ERROR`/`WARN` patterns and a `Started GembudApplication`
   line.

---

## 2. Environment Variables Checklist

These come from `.env.example` (root) which is the file actually
consumed by `docker-compose.prod.yml`. The `backend/.env.example` and
`frontend/.env.example` files are reference defaults for local
development only.

Legend: **R** = required for production, **O** = optional /
has-a-default.

### 2.1 Database (Postgres)

| Var                  | Req | Notes                                        |
| -------------------- | --- | -------------------------------------------- |
| `DATABASE_NAME`      | O   | Defaults to `gembud`.                        |
| `DATABASE_USERNAME`  | R   | App role; do not reuse the `postgres` superuser. |
| `DATABASE_PASSWORD`  | R   | High-entropy random string.                  |
| `DATABASE_PORT`      | O   | Host-side port mapping. Default `5432`.      |

### 2.2 Redis

| Var              | Req | Notes                                        |
| ---------------- | --- | -------------------------------------------- |
| `REDIS_HOST`     | O   | Inside the compose network use `redis`.      |
| `REDIS_PORT`     | O   | Default `6379`.                              |
| `REDIS_PASSWORD` | R   | Required for production — Redis is exposed on the host port. |

### 2.3 JWT

| Var          | Req | Notes                                                                                            |
| ------------ | --- | ------------------------------------------------------------------------------------------------ |
| `JWT_SECRET` | R   | Must be >= 32 bytes (256 bits) for HS256. Treat as a top-tier secret. See section 4.4 for rotation. |

### 2.4 OAuth — Google

| Var                    | Req | Notes |
| ---------------------- | --- | ----- |
| `GOOGLE_CLIENT_ID`     | R   | From Google Cloud Console > APIs & Services > Credentials. |
| `GOOGLE_CLIENT_SECRET` | R   | Same source. Rotate via the Google console; update `.env`; `docker compose up -d backend`. |

### 2.5 OAuth — Discord

| Var                     | Req | Notes |
| ----------------------- | --- | ----- |
| `DISCORD_CLIENT_ID`     | R   | From the Discord Developer Portal > OAuth2. |
| `DISCORD_CLIENT_SECRET` | R   | Same. Rotate and redeploy backend on suspicion of compromise. |

### 2.6 Server / Frontend wiring

| Var                  | Req | Notes                                              |
| -------------------- | --- | -------------------------------------------------- |
| `BACKEND_PORT`       | O   | Host port for backend. Default `8080`.             |
| `FRONTEND_PORT`      | O   | Host port for frontend. Default `3000`.            |
| `VITE_API_BASE_URL`  | R   | Baked into the frontend at build time. Set to the public API URL (e.g. `https://api.example.com/api`). |
| `VITE_WS_BASE_URL`   | R   | WebSocket URL. Use `wss://` in production.         |

### 2.7 Backend security / behaviour (set on the backend service env)

These are not in the root `.env.example` but are read by the backend
via `application.yml`. Set them in the `backend` service `environment:`
block of `docker-compose.prod.yml` (or extend the root `.env`):

| Var                                       | Req | Notes |
| ----------------------------------------- | --- | ----- |
| `SECURITY_ALERT_WEBHOOK_URL`              | R\* | Slack incoming webhook for security alerts. R\* = required if you want to receive HIGH/CRITICAL alerts; if blank, alerts are silently dropped. |
| `APP_ADMIN_EMAIL`                         | O   | Email address that is auto-promoted to `ADMIN` on first login. Leave blank to manage admin role manually. |
| `COOKIE_SECURE`                           | R   | `true` in production (HTTPS required for `Secure` cookies). |
| `COOKIE_SAMESITE`                         | O   | Default `Lax`. Use `None` only if frontend and backend are on different origins and you understand the CSRF implications. |
| `WEBSOCKET_ALLOWED_ORIGINS`               | R   | Comma-separated list of origins allowed to connect to `/ws`. |
| `OAUTH2_REDIRECT_URI`                     | R   | Must match the registered callback URL at Google/Discord. |
| `LOGIN_LOCK_THRESHOLD`                    | O   | Failed attempts before account lock. Default `10`. |
| `LOGIN_LOCK_WINDOW_MINUTES`               | O   | Window for counting failures. Default `10`. |
| `LOGIN_LOCK_DURATION_MINUTES`             | O   | How long an account stays locked. Default `10`. |
| `LOGIN_FAIL_BURST_HIGH_THRESHOLD`         | O   | Burst alert threshold for HIGH severity. Default `10`. |
| `LOGIN_FAIL_BURST_CRITICAL_THRESHOLD`     | O   | Burst alert threshold for CRITICAL severity. Default `30`. |
| `SECURITY_EVENT_RETENTION_DAYS`           | O   | Daily purge cutoff. Default `90`. |
| `FEATURE_PREMIUM_ENABLED`                 | O   | Default `false`. See `PREMIUM_FEATURE_TOGGLE_RUNBOOK_2026-03-05.md`. |
| `ADS_DAILY_VIEW_LIMIT`                    | O   | Default `5`. |

### 2.8 TLS / Reverse proxy

Caddy (or another TLS terminator) is not yet wired in. When it lands,
expect at minimum:

- `CADDY_DOMAIN` — public hostname.
- `CADDY_EMAIL` — ACME contact email for Let's Encrypt.

These will be tracked in a follow-up runbook update.

### 2.9 Secret rotation procedure

General rule: every secret in section 2 is rotatable by editing `.env`
and running `docker compose up -d <service>` to recreate the container.
Specific cases:

- **`JWT_SECRET`**: rotating this invalidates **every** issued access
  and refresh token. All users will be logged out on their next request.
  Procedure:
  1. Schedule a maintenance window (or accept the user-visible logout).
  2. Generate a new secret: `openssl rand -base64 48`.
  3. Update `.env` and `docker compose up -d backend`.
  4. Watch logs for a spike in `LOGIN_FAIL` / `REFRESH_FAIL` events
     (expected, not actionable).
- **DB / Redis passwords**: changing these requires updating the value
  inside Postgres / Redis itself first (`ALTER USER ... WITH PASSWORD`
  / `CONFIG SET requirepass`), then updating `.env`, then restarting
  dependent containers. Do this during low-traffic windows.
- **OAuth client secrets**: rotate at the provider, update `.env`,
  restart backend. Existing logged-in users keep their session
  (OAuth tokens are exchanged once; we issue our own JWTs).

---

## 3. Rollback

### 3.1 Image / code rollback (safe)

If the bad change is **code-only** (no schema migration in the new
version):

```bash
# 1. Find the previous good ref
git log --oneline

# 2. Check it out and rebuild
git checkout <previous-good-ref>
docker compose -f docker-compose.prod.yml up -d --build

# 3. Smoke
bash scripts/smoke.sh https://<your-domain>
```

This is the fast, low-risk path. Use it whenever possible.

### 3.2 Schema rollback (unsafe — manual forward migration required)

**Flyway is configured forward-only.** There is no automatic `migrate
down` / `undo` step. The migration history table tracks applied
versions; rolling the application image back to a version that doesn't
know about a newer migration will leave that migration applied in the
DB but unreferenced by code. That is usually fine for additive changes,
but breaks if:

- a column was made `NOT NULL` and the old code writes `NULL`,
- a column or table the old code reads was dropped or renamed,
- a constraint was added that the old code violates.

If the new release contains a destructive or breaking migration and you
need to roll back:

1. **Do not** restore the DB from a snapshot taken before the migration
   without first stopping all writers — you will lose any data written
   after the snapshot.
2. Write a **new forward migration** (e.g. `V36__revert_V35.sql`) that
   undoes the breaking change — re-add the dropped column, drop the
   constraint, etc. Test it on a staging copy of production data.
3. Bundle that migration with the rollback build, deploy that build.

Rule of thumb when authoring migrations: prefer additive,
backward-compatible changes (new nullable columns, new tables, new
indexes). Drops and renames should land in two-step releases — first
add the new shape, ship it, deprecate the old shape, then drop in a
later release once nothing reads it.

### 3.3 Rollback decision matrix

| Change in the bad release         | Safe to image-only rollback? |
| --------------------------------- | ---------------------------- |
| Backend code only                 | Yes                          |
| Frontend code only                | Yes                          |
| Config / env var change           | Yes (revert env)             |
| New Flyway migration, additive    | Usually yes                  |
| New Flyway migration, destructive | No — write forward fix       |

The current production schema head is `V34__add_chat_room_public_id`.
Before deploying anything that adds a `V35__*` or higher, eyeball the
SQL against this table.

---

## 4. Common Failure Modes & Fixes

### 4.1 Postgres down

**Symptom**: backend logs spam
`org.postgresql.util.PSQLException: Connection refused` or
`FATAL: password authentication failed`. Health endpoint returns 503.
Smoke script fails on `/api/games`.

**Diagnose**:

```bash
docker compose -f docker-compose.prod.yml ps postgres
docker compose -f docker-compose.prod.yml logs --tail=100 postgres
docker compose -f docker-compose.prod.yml exec postgres pg_isready -U "$DATABASE_USERNAME"
```

**Resolve**:

- Container exited: check logs for OOM or disk-full. Restart with
  `docker compose -f docker-compose.prod.yml up -d postgres`.
- Auth failure: confirm `DATABASE_PASSWORD` in `.env` matches what the
  Postgres container was initialized with. Note: changing
  `DATABASE_PASSWORD` does **not** change the existing role's password
  — you must `ALTER USER ... WITH PASSWORD '...'` inside Postgres.
- Disk full: see 4.5.

### 4.2 Redis down

**Symptom**: rate limiting, login lockout, Slack alert dedupe, and the
STOMP/WebSocket broker stop working. Backend logs:
`RedisConnectionFailureException` or `Could not get a resource from the
pool`. Users report chat messages not delivering.

**Diagnose**:

```bash
docker compose -f docker-compose.prod.yml ps redis
docker compose -f docker-compose.prod.yml logs --tail=100 redis
docker compose -f docker-compose.prod.yml exec redis redis-cli -a "$REDIS_PASSWORD" ping
```

**Resolve**:

- Restart: `docker compose -f docker-compose.prod.yml up -d redis`.
- If the AOF (append-only file) is corrupt, the container will fail to
  start. Logs will say so. As a last resort,
  `docker compose -f docker-compose.prod.yml exec redis redis-check-aof --fix /data/appendonly.aof`.
  This is **lossy** for any non-fsynced writes.
- Authentication failures: confirm `REDIS_PASSWORD` matches what the
  container was started with.

### 4.3 OAuth provider down

**Symptom**: only Google logins fail (or only Discord). The other
provider still works. Frontend shows the OAuth callback erroring out;
backend logs `OAuth2AuthenticationException`.

**Diagnose**:

- Check provider status:
  - Google: <https://status.cloud.google.com/>
  - Discord: <https://discordstatus.com/>
- `docker compose -f docker-compose.prod.yml logs backend | grep -i oauth`
  to confirm the failure is at the provider exchange step and not at
  callback validation (which would point at our config).

**Resolve**:

- Provider outage: nothing to do but wait. Do **not** disable the
  affected provider on a whim — users who only know that login method
  will be locked out. Post a status update instead.
- Callback URL mismatch (after a domain change): update
  `OAUTH2_REDIRECT_URI` and the registered callback in the provider
  console.

### 4.4 JWT secret rotation needed (force-logout all users)

**When**: confirmed `JWT_SECRET` leak, or post-incident hygiene after a
suspected admin compromise.

**Procedure**: see section 2.9. The user-visible effect is that every
session is invalidated; users must re-log-in. Refresh tokens stored
client-side will also fail, triggering the
`SessionExpiredModal` flow.

After rotation, monitor:

- Dashboard / DB query: count of `LOGIN_SUCCESS` events should recover
  to baseline within the typical session length.
- Slack channel: a brief spike of `REFRESH_FAIL` events is expected and
  not actionable.

### 4.5 Disk full

**Symptom**: container restarts, `No space left on device` in logs,
Postgres refuses writes. Smoke script fails on writes (login, room
creation), reads may still work briefly.

**Diagnose**:

```bash
df -h
docker system df
du -sh /var/lib/docker/volumes/* | sort -h | tail -20
```

**Resolve**:

- Prune dangling images/build cache:
  `docker system prune -a --volumes` (read the prompt — this deletes
  unused volumes too).
- Truncate large container logs (Docker logs grow without bound by
  default). Consider configuring the daemon with `log-opts: { max-size:
  "100m", max-file: "3" }`.
- Postgres data growth: check for runaway `chat_messages` /
  `security_events` tables. The latter is auto-purged daily at 03:00
  by `SecurityEventService.purgeOldEvents` based on
  `SECURITY_EVENT_RETENTION_DAYS`. If that scheduler hasn't been
  running, manually:

  ```sql
  DELETE FROM security_events WHERE created_at < NOW() - INTERVAL '90 days';
  VACUUM (ANALYZE) security_events;
  ```

### 4.6 Container OOM (out of memory)

**Symptom**: backend container is restarted by Docker with exit code
137 or `OOMKilled` in `docker inspect`. Frontend or smoke script sees
intermittent 502/connection-refused errors.

**Diagnose**:

```bash
docker compose -f docker-compose.prod.yml ps
docker inspect gembud-backend-prod --format '{{.State.OOMKilled}} {{.State.ExitCode}}'
docker stats --no-stream
```

**Resolve**:

- The backend image is launched with
  `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`, so the JVM
  heap auto-sizes against the container limit. If no limit is set, it
  falls back to host memory and can starve Postgres/Redis.
- Add a `mem_limit` to the `backend` service in
  `docker-compose.prod.yml` (e.g. `mem_limit: 1g`) and redeploy.
- Confirm the heap is healthy via
  `docker compose exec backend jcmd 1 GC.heap_info` (works only if a
  JDK image is used; the current Dockerfile uses a JRE — consider
  installing `jcmd` ad-hoc or temporarily swapping to a JDK image when
  diagnosing).
- For Postgres OOM, tune `shared_buffers` and `work_mem` downward, or
  give the host more RAM.

---

## 5. Security Event Slack Alerts

`SecurityEventService` records events into the `security_events` table.
Events tagged `HIGH` or `CRITICAL` also fire a Slack message via
`SlackAlertService` to the webhook in `SECURITY_ALERT_WEBHOOK_URL`.
Slack messages are deduped per `(eventType, ip)` for 10 minutes via
Redis, so a single attacker hammering one endpoint produces one Slack
alert per 10-minute bucket, not thousands.

The full enum lives in
`backend/src/main/java/com/gembud/entity/SecurityEvent.java`. Below is
the operational meaning of each value.

| Event type                | Typical risk      | Source                         | What it means                                                                                  | What to do |
| ------------------------- | ----------------- | ------------------------------ | ---------------------------------------------------------------------------------------------- | ---------- |
| `LOGIN_SUCCESS`           | LOW (no alert)    | `AuthService.login`            | Normal successful login.                                                                       | Nothing — informational only. Not paged. |
| `LOGIN_FAIL`              | LOW (no alert)    | `AuthService.login`            | Bad password or unknown email. Single occurrence is benign.                                    | Auto-resolves. Drives the burst detector below. |
| `LOGIN_FAIL_BURST`        | HIGH or CRITICAL  | `SecurityEventService` (auto)  | A given IP exceeded the failure threshold within a 5-minute window. HIGH at `LOGIN_FAIL_BURST_HIGH_THRESHOLD` (default 10), CRITICAL at `LOGIN_FAIL_BURST_CRITICAL_THRESHOLD` (default 30). | Investigate the IP. If clearly malicious (datacenter range, distributed across many user IDs), block it at the proxy / firewall. If targeting a single user, consider notifying them. |
| `LOGIN_LOCKED`            | HIGH              | `AuthService.login`            | An individual user account was auto-locked after exceeding `LOGIN_LOCK_THRESHOLD` failures. The user must wait `LOGIN_LOCK_DURATION_MINUTES` or have an admin unlock them. | Confirm the lock was attacker-driven (high failure count in `security_events`) vs the user fat-fingering. If attacker-driven, see `LOGIN_FAIL_BURST` actions. If the user is locked out and pressuring support, an admin can clear the lock via the admin API (which emits `ADMIN_UNLOCK_LOGIN`). |
| `REFRESH_SUCCESS`         | LOW (no alert)    | `AuthService.refresh`          | Normal access-token refresh.                                                                   | Nothing. |
| `REFRESH_FAIL`            | LOW (no alert)    | `AuthService.refresh`          | A refresh token was rejected (expired, missing, malformed). Common after `JWT_SECRET` rotation or a long idle period. | Auto-resolves. A sustained spike is interesting — cross-reference with `JWT_SECRET` rotation timing. |
| `REFRESH_REUSE_DETECTED`  | HIGH              | `AuthService.refresh`          | A refresh token that had already been rotated was presented again. Strong signal of token theft. The associated user's session family is revoked. | Page on-call. Notify the user via in-app / email. Force them to re-authenticate. Check for malware on the user's device. |
| `SESSION_REVOKED`         | LOW–MEDIUM        | (admin or self-initiated)      | A session was deliberately revoked.                                                            | Informational unless paired with a `REFRESH_REUSE_DETECTED` for the same user. |
| `WS_CONNECT_DENIED`       | HIGH              | `WebSocketConfig` STOMP interceptor | A client attempted to upgrade to WebSocket without valid auth, or with a revoked session.    | Single occurrences usually mean a stale tab. Bursts from one IP suggest someone probing the WS layer; treat like `LOGIN_FAIL_BURST`. |
| `RATE_LIMIT_HIT`          | MEDIUM (no Slack alert in current wiring) | `RateLimitService` callers     | A request was rejected because it exceeded the per-endpoint rate limit. Counted in the summary endpoint. | Spot-check `security_events` for the offending IP / user. If not abusive, consider raising the limit. |
| `ADMIN_UNLOCK_LOGIN`      | LOW (audit)       | Admin user-management endpoint | An admin manually cleared an account lock.                                                     | Audit only. Confirm the admin was acting on a real support ticket. |
| `REPORT_WARNED`           | LOW (audit)       | `ReportService`                | A user received an automated warning after report thresholds were exceeded.                    | Audit only. Sustained spikes may indicate report abuse. |

**Reading a Slack alert**: the message format is
`*[<RISK>]* Security event: <eventType> | Risk: <RISK> | User: <userId|anonymous> | IP: <ip> | Endpoint: <endpoint>`.
First action for any HIGH/CRITICAL alert: query the DB for surrounding
context — same IP, same user, same time window:

```sql
SELECT created_at, event_type, user_id, ip, endpoint, result, risk_score
FROM security_events
WHERE created_at > NOW() - INTERVAL '30 minutes'
  AND (ip = '<ip from alert>' OR user_id = <userId from alert>)
ORDER BY created_at DESC;
```

The admin security UI (`AdminSecurityController`) wraps the same data
behind a UI for non-DB-savvy responders.

---

## 6. Useful Commands

| Goal                                  | Command |
| ------------------------------------- | ------- |
| Tail backend logs                     | `docker compose -f docker-compose.prod.yml logs -f backend` |
| Tail all logs                         | `docker compose -f docker-compose.prod.yml logs -f` |
| Service status                        | `docker compose -f docker-compose.prod.yml ps` |
| Restart backend only                  | `docker compose -f docker-compose.prod.yml restart backend` |
| Recreate backend with new env         | `docker compose -f docker-compose.prod.yml up -d backend` |
| Rebuild and restart backend           | `docker compose -f docker-compose.prod.yml up -d --build backend` |
| psql into Postgres                    | `docker compose -f docker-compose.prod.yml exec postgres psql -U "$DATABASE_USERNAME" "$DATABASE_NAME"` |
| Redis CLI                             | `docker compose -f docker-compose.prod.yml exec redis redis-cli -a "$REDIS_PASSWORD"` |
| Smoke test                            | `bash scripts/smoke.sh https://<your-domain>` |
| Tail security events                  | `docker compose -f docker-compose.prod.yml exec postgres psql -U "$DATABASE_USERNAME" "$DATABASE_NAME" -c "SELECT created_at, event_type, risk_score, ip FROM security_events ORDER BY created_at DESC LIMIT 50;"` |
| Show Flyway migration history         | `docker compose -f docker-compose.prod.yml exec postgres psql -U "$DATABASE_USERNAME" "$DATABASE_NAME" -c "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 20;"` |
| Inspect container resource usage      | `docker stats --no-stream` |
| Check disk usage                      | `df -h && docker system df` |

The smoke script lives at `scripts/smoke.sh` and is safe to re-run on
demand; it only performs read-only requests.

---

## 7. Contacts / Escalation

> **TBD — to be filled in by the project owner.**

- **Owner / primary on-call**: TBD
- **Backup on-call**: TBD
- **Slack channel for ops**: `#gembud-ops` (TBD)
- **Slack channel for security alerts**: configured via
  `SECURITY_ALERT_WEBHOOK_URL` (TBD which channel)
- **Provider support**:
  - Hosting: TBD
  - DNS: TBD
  - OAuth — Google: <https://console.cloud.google.com/> > Support
  - OAuth — Discord: <https://support-dev.discord.com/>
- **Severity escalation policy**: TBD. Suggested starting point —
  CRITICAL alerts page on-call within 15 minutes; HIGH alerts get a
  Slack ping that on-call should ack within 1 business hour; MEDIUM /
  LOW are review-during-business-hours.

This section is a placeholder. Edit it as soon as real contacts are
known.
