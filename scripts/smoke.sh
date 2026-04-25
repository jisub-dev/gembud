#!/usr/bin/env bash
# Post-deploy smoke check for Gembud.
# Verifies that the deployed stack is responding on its public surface.
#
# Usage: bash scripts/smoke.sh [BASE_URL]
#   BASE_URL defaults to http://localhost:8080
#
# Exit codes:
#   0 = all required checks passed (warnings allowed)
#   1 = one or more required checks failed

set -uo pipefail

BASE_URL="${1:-http://localhost:8080}"
BASE_URL="${BASE_URL%/}"

FAIL=0
WARN=0

green() { printf "\033[32m%s\033[0m" "$1"; }
red()   { printf "\033[31m%s\033[0m" "$1"; }
yellow(){ printf "\033[33m%s\033[0m" "$1"; }

# check <name> <required|optional> <method> <path> <expected_status> [body_grep_pattern]
check() {
    local name="$1"
    local mode="$2"   # required | optional
    local method="$3"
    local path="$4"
    local expected="$5"
    local body_pattern="${6:-}"

    local url="${BASE_URL}${path}"
    local body_file
    body_file="$(mktemp)"

    local http_code
    http_code="$(curl -sS -X "$method" -o "$body_file" -w "%{http_code}" \
        --max-time 10 \
        -H "Accept: application/json" \
        "$url" 2>/dev/null)"
    http_code="${http_code:-000}"

    local pass=1
    if [ "$http_code" != "$expected" ]; then
        pass=0
    fi

    if [ -n "$body_pattern" ] && [ "$pass" -eq 1 ]; then
        if ! grep -q "$body_pattern" "$body_file"; then
            pass=0
        fi
    fi

    if [ "$pass" -eq 1 ]; then
        echo "  $(green '✓') ${name} (${http_code})"
    else
        if [ "$mode" = "optional" ]; then
            echo "  $(yellow '!') ${name} (${http_code}) — optional, skipping"
            WARN=$((WARN + 1))
        else
            echo "  $(red '✗') ${name} (${http_code}) expected ${expected}"
            if [ -s "$body_file" ]; then
                echo "      body: $(head -c 200 "$body_file")"
            fi
            FAIL=$((FAIL + 1))
        fi
    fi

    rm -f "$body_file"
}

echo "Gembud smoke check against: ${BASE_URL}"
echo

echo "== Health =="
# Actuator may not be installed yet (pre-PR 1.2). Mark optional with warning.
check "actuator/health"        optional GET "/actuator/health"            200 "UP"

echo
echo "== Public API =="
check "GET /api/games"         required GET "/api/games"                  200
check "GET /api/auth/csrf"     required GET "/api/auth/csrf"              200

echo
echo "== Frontend =="
# Frontend may serve via separate origin; only check if BASE_URL targets it.
check "GET /"                  optional GET "/"                           200

echo
echo "Summary: ${FAIL} required failed, ${WARN} optional warnings"

if [ "$FAIL" -gt 0 ]; then
    echo "$(red 'FAIL'): ${FAIL} required check(s) failed"
    exit 1
fi

if [ "$WARN" -gt 0 ]; then
    echo "$(yellow 'OK with warnings'): all required checks passed, ${WARN} optional warnings"
else
    echo "$(green 'OK'): all checks passed"
fi
exit 0
