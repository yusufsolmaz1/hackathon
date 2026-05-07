#!/usr/bin/env bash
# Shared helpers for curl smoke tests.
# Usage:
#   source ./_lib.sh
#   BASE=${BASE:-http://localhost:8080}
#   register "$NAME" "$EMAIL" "$PASS"   → sets TOKEN, USER_ID
#   login_as "$EMAIL" "$PASS"           → sets TOKEN, USER_ID
#   req METHOD PATH [BODY]              → sets BODY, HTTP_CODE in current shell
#   req_anon METHOD PATH [BODY]         → same, but no auth header
#
# We intentionally do NOT use `set -u`, and we never wrap req() in $(...),
# because that would trap HTTP_CODE inside a subshell and the parent
# assertions would never see it.

PASSED=0
FAILED=0
BASE=${BASE:-http://localhost:8080}
TOKEN=${TOKEN:-}
USER_ID=${USER_ID:-}
TIMEOUT=${TIMEOUT:-25}
BODY=""
HTTP_CODE=""

C_RED='\033[0;31m'; C_GRN='\033[0;32m'; C_YEL='\033[1;33m'; C_RST='\033[0m'
TMP_BODY="$(mktemp -t trendsocial-body.XXXXXX)"
trap 'rm -f "$TMP_BODY"' EXIT

assert_status() {
    local got="$1" want="$2" label="$3"
    if [ "$got" = "$want" ]; then
        echo -e "${C_GRN}PASS${C_RST} $label ($got)"
        PASSED=$((PASSED + 1))
    else
        echo -e "${C_RED}FAIL${C_RST} $label (got $got, want $want)"
        FAILED=$((FAILED + 1))
    fi
}

assert_contains() {
    local body="$1" needle="$2" label="$3"
    if printf "%s" "$body" | grep -q "$needle"; then
        echo -e "${C_GRN}PASS${C_RST} $label"
        PASSED=$((PASSED + 1))
    else
        echo -e "${C_RED}FAIL${C_RST} $label (missing: $needle)"
        echo "    body: $(printf "%s" "$body" | head -c 200)"
        FAILED=$((FAILED + 1))
    fi
}

# req METHOD PATH [BODY] — sets globals BODY, HTTP_CODE
req() {
    local method="$1" path="$2" body_in="${3:-}"
    local args=(-sS -X "$method" -m "$TIMEOUT" -o "$TMP_BODY" -w "%{http_code}" -H "Content-Type: application/json")
    [ -n "$TOKEN" ] && args+=(-H "Authorization: Bearer $TOKEN")
    [ -n "$body_in" ] && args+=(-d "$body_in")
    HTTP_CODE=$(curl "${args[@]}" "$BASE$path")
    BODY=$(cat "$TMP_BODY")
}

req_anon() {
    local method="$1" path="$2" body_in="${3:-}"
    local args=(-sS -X "$method" -m "$TIMEOUT" -o "$TMP_BODY" -w "%{http_code}" -H "Content-Type: application/json")
    [ -n "$body_in" ] && args+=(-d "$body_in")
    HTTP_CODE=$(curl "${args[@]}" "$BASE$path")
    BODY=$(cat "$TMP_BODY")
}

register() {
    local name="$1" email="$2" pass="$3"
    req_anon POST /auth/register "{\"name\":\"$name\",\"email\":\"$email\",\"password\":\"$pass\"}"
    if [ "$HTTP_CODE" = "201" ]; then
        TOKEN=$(printf "%s" "$BODY" | tr -d '\n' | sed -n 's/.*"token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
        USER_ID=$(printf "%s" "$BODY" | tr -d '\n' | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\(usr_[^"]*\)".*/\1/p')
    else
        echo "register failed ($HTTP_CODE): $BODY" >&2
        return 1
    fi
}

login_as() {
    local email="$1" pass="$2"
    req_anon POST /auth/login "{\"email\":\"$email\",\"password\":\"$pass\"}"
    if [ "$HTTP_CODE" = "200" ]; then
        TOKEN=$(printf "%s" "$BODY" | tr -d '\n' | sed -n 's/.*"token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
        USER_ID=$(printf "%s" "$BODY" | tr -d '\n' | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\(usr_[^"]*\)".*/\1/p')
    else
        echo "login failed ($HTTP_CODE): $BODY" >&2
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

rand_suffix() {
    printf "%s%04d" "$(date +%s)" "$RANDOM" | tail -c 10
}
