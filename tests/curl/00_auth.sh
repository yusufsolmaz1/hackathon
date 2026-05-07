#!/usr/bin/env bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/_lib.sh"

echo "── Auth ──────────────────────────────"
SUFFIX=$(rand_suffix)
EMAIL="smoke_${SUFFIX}@test.com"
PASS="Sifre123!"

# register
register "Smoke ${SUFFIX}" "$EMAIL" "$PASS"
[ -n "$TOKEN" ] && PASSED=$((PASSED + 1)) && echo -e "${C_GRN}✓${C_RST} register returned token" \
    || { echo -e "${C_RED}✗${C_RST} register failed"; FAILED=$((FAILED + 1)); }

# duplicate register
body=$(req_anon POST /auth/register "{\"name\":\"x\",\"email\":\"$EMAIL\",\"password\":\"$PASS\"}")
assert_status "$HTTP_CODE" 409 "duplicate register → 409"

# login wrong pass
body=$(req_anon POST /auth/login "{\"email\":\"$EMAIL\",\"password\":\"wrong\"}")
assert_status "$HTTP_CODE" 401 "wrong password → 401"

# login good
login_as "$EMAIL" "$PASS"
[ -n "$TOKEN" ] && PASSED=$((PASSED + 1)) && echo -e "${C_GRN}✓${C_RST} login returned token" \
    || { echo -e "${C_RED}✗${C_RST} login failed"; FAILED=$((FAILED + 1)); }

# profile (auth required)
body=$(req GET /auth/profile)
assert_status "$HTTP_CODE" 200 "profile GET → 200"
assert_contains "$body" "$EMAIL" "profile contains email"

# profile no token
SAVED_TOKEN="$TOKEN"; TOKEN=""
body=$(req GET /auth/profile)
assert_status "$HTTP_CODE" 401 "profile without token → 401"
TOKEN="$SAVED_TOKEN"

# profile update (with birthday)
body=$(req PUT /auth/profile '{"name":"Updated Name","birthday":"1998-05-15T00:00:00Z"}')
assert_status "$HTTP_CODE" 200 "profile PUT → 200"
assert_contains "$body" "Updated Name" "profile name updated"
assert_contains "$body" "1998-05-15" "birthday persisted"

# bad birthday
body=$(req PUT /auth/profile '{"birthday":"not-a-date"}')
assert_status "$HTTP_CODE" 400 "bad birthday → 400"
assert_contains "$body" "INVALID_DATE" "error code INVALID_DATE"

# register with birthday
SUFFIX2=$(rand_suffix)
EMAIL2="bday_${SUFFIX2}@test.com"
body=$(req_anon POST /auth/register "{\"name\":\"BDay $SUFFIX2\",\"email\":\"$EMAIL2\",\"password\":\"Sifre123!\",\"birthday\":\"2000-01-01T00:00:00Z\"}")
assert_status "$HTTP_CODE" 201 "register with birthday → 201"
assert_contains "$body" "2000-01-01" "register response includes birthday"

summary
