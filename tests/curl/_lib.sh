#!/usr/bin/env bash
# Shared helpers for curl smoke tests.
# Usage:
#   source ./_lib.sh
#   BASE=${BASE:-http://localhost:8080}
#   register "$NAME" "$EMAIL" "$PASS"   → sets TOKEN, USER_ID
#   login_as "$EMAIL" "$PASS"           → sets TOKEN, USER_ID
#   req METHOD PATH [BODY]              → curl with auth header

set -u
PASSED=0
FAILED=0
BASE=${BASE:-http://localhost:8080}
TOKEN=${TOKEN:-}
USER_ID=${USER_ID:-}
TIMEOUT=${TIMEOUT:-25}

C_RED='\033[0;31m'; C_GRN='\033[0;32m'; C_YEL='\033[1;33m'; C_RST='\033[0m'

assert_status() {
    local got="$1" want="$2" label="$3"
    if [ "$got" = "$want" ]; then
        echo -e "${C_GRN}✓${C_RST} $label ($got)"
        PASSED=$((PASSED + 1))
    else
        echo -e "${C_RED}✗${C_RST} $label (got $got, want $want)"
        FAILED=$((FAILED + 1))
    fi
}

assert_contains() {
    local body="$1" needle="$2" label="$3"
    if printf "%s" "$body" | grep -q "$needle"; then
        echo -e "${C_GRN}✓${C_RST} $label"
        PASSED=$((PASSED + 1))
    else
        echo -e "${C_RED}✗${C_RST} $label (missing: $needle)"
        echo "    body: $(printf "%s" "$body" | head -c 200)"
        FAILED=$((FAILED + 1))
    fi
}

# Generic auth'd request: req METHOD PATH [BODY] → echoes body, sets HTTP_CODE
req() {
    local method="$1" path="$2" body="${3:-}"
    local args=(-sS -X "$method" -m "$TIMEOUT" -w "\n%{http_code}" -H "Content-Type: application/json")
    [ -n "$TOKEN" ] && args+=(-H "Authorization: Bearer $TOKEN")
    [ -n "$body" ] && args+=(-d "$body")
    local raw
    raw=$(curl "${args[@]}" "$BASE$path")
    HTTP_CODE=$(printf "%s" "$raw" | tail -n1)
    printf "%s" "$raw" | sed '$d'
}

# Auth-less variant for register/login
req_anon() {
    local method="$1" path="$2" body="${3:-}"
    local args=(-sS -X "$method" -m "$TIMEOUT" -w "\n%{http_code}" -H "Content-Type: application/json")
    [ -n "$body" ] && args+=(-d "$body")
    local raw
    raw=$(curl "${args[@]}" "$BASE$path")
    HTTP_CODE=$(printf "%s" "$raw" | tail -n1)
    printf "%s" "$raw" | sed '$d'
}

register() {
    local name="$1" email="$2" pass="$3"
    local body
    body=$(req_anon POST /auth/register "{\"name\":\"$name\",\"email\":\"$email\",\"password\":\"$pass\"}")
    if [ "$HTTP_CODE" = "201" ]; then
        TOKEN=$(printf "%s" "$body" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
        USER_ID=$(printf "%s" "$body" | sed -n 's/.*"id":"\(usr_[^"]*\)".*/\1/p' | head -n1)
    else
        echo "register failed ($HTTP_CODE): $body" >&2
        return 1
    fi
}

login_as() {
    local email="$1" pass="$2"
    local body
    body=$(req_anon POST /auth/login "{\"email\":\"$email\",\"password\":\"$pass\"}")
    if [ "$HTTP_CODE" = "200" ]; then
        TOKEN=$(printf "%s" "$body" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
        USER_ID=$(printf "%s" "$body" | sed -n 's/.*"id":"\(usr_[^"]*\)".*/\1/p' | head -n1)
    else
        echo "login failed ($HTTP_CODE): $body" >&2
        return 1
    fi
}

summary() {
    echo
    echo "─────────────────────────────────────"
    echo -e "  ${C_GRN}passed: $PASSED${C_RST}    ${C_RED}failed: $FAILED${C_RST}"
    echo "─────────────────────────────────────"
    [ "$FAILED" -eq 0 ]
}

# Random suffix for unique emails
rand_suffix() {
    printf "%s%04d" "$(date +%s)" "$RANDOM" | tail -c 10
}
