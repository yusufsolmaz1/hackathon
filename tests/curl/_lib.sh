#!/usr/bin/env bash
# Shared helpers for curl smoke tests.
# Source from script root: . "$(dirname "$0")/_lib.sh"

set -u
BASE="${BASE:-http://localhost:8080}"

# .env'i otomatik yükle (Supabase seed scriptleri için)
if [[ -z "${SUPABASE_URL:-}" ]] && [[ -f "$(dirname "${BASH_SOURCE[0]}")/../../.env" ]]; then
    set -a
    # shellcheck disable=SC1091
    source "$(dirname "${BASH_SOURCE[0]}")/../../.env"
    set +a
fi

# Renkli çıktı (terminal değilse no-op)
if [[ -t 1 ]]; then
    GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[0;33m'; BOLD='\033[1m'; NC='\033[0m'
else
    GREEN=''; RED=''; YELLOW=''; BOLD=''; NC=''
fi

PASS=0; FAIL=0

section() { printf "\n${BOLD}==> %s${NC}\n" "$1"; }
ok()      { printf "  ${GREEN}✓${NC} %s\n" "$1"; PASS=$((PASS+1)); }
fail()    { printf "  ${RED}✗${NC} %s\n" "$1"; FAIL=$((FAIL+1)); }
note()    { printf "  ${YELLOW}·${NC} %s\n" "$1"; }

# usage: assert_status <expected> <actual> <message>
assert_status() {
    local expected=$1 actual=$2 msg=$3
    if [[ "$actual" == "$expected" ]]; then
        ok "$msg → HTTP $actual"
    else
        fail "$msg → HTTP $actual (expected $expected)"
    fi
}

# usage: http_get <path> [header...]   →  echoes "BODY|||STATUS"
http_get() {
    local path=$1; shift
    local headers=()
    while (( $# )); do headers+=(-H "$1"); shift; done
    curl -s -w "|||%{http_code}" "${BASE}${path}" ${headers[@]+"${headers[@]}"}
}

http_post() {
    local path=$1 body=$2; shift 2
    local headers=(-H "Content-Type: application/json")
    while (( $# )); do headers+=(-H "$1"); shift; done
    curl -s -w "|||%{http_code}" -X POST "${BASE}${path}" "${headers[@]}" -d "$body"
}

http_delete() {
    local path=$1; shift
    local headers=()
    while (( $# )); do headers+=(-H "$1"); shift; done
    curl -s -w "|||%{http_code}" -X DELETE "${BASE}${path}" ${headers[@]+"${headers[@]}"}
}

# split "BODY|||STATUS"
body_of()   { echo "${1%|||*}"; }
status_of() { echo "${1##*|||}"; }

# usage: jpath <json> <python-expression-on-d>
jpath() {
    python3 -c "import json,sys;d=json.loads(sys.argv[1]);print($2)" "$1"
}

summary() {
    printf "\n${BOLD}Sonuç:${NC} ${GREEN}%d passed${NC}, ${RED}%d failed${NC}\n" "$PASS" "$FAIL"
    [[ $FAIL -eq 0 ]] || exit 1
}
