#!/usr/bin/env bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/_lib.sh"

echo "── Auth ──────────────────────────────"
SUFFIX=$(rand_suffix)
EMAIL="smoke_${SUFFIX}@test.com"
PASS="Sifre123!"

# register
register "Smoke ${SUFFIX}" "$EMAIL" "$PASS"
if [ -n "$TOKEN" ]; then
    echo -e "${C_GRN}PASS${C_RST} register returned token"
    PASSED=$((PASSED + 1))
else
    echo -e "${C_RED}FAIL${C_RST} register failed"
    FAILED=$((FAILED + 1))
fi

# duplicate register
req_anon POST /auth/register "{\"name\":\"x\",\"email\":\"$EMAIL\",\"password\":\"$PASS\"}"
assert_status "$HTTP_CODE" 400 "duplicate register → 400 (EMAIL_EXISTS)"

# login wrong pass
req_anon POST /auth/login "{\"email\":\"$EMAIL\",\"password\":\"wrong\"}"
assert_status "$HTTP_CODE" 401 "wrong password → 401"

# login good
login_as "$EMAIL" "$PASS"
if [ -n "$TOKEN" ]; then
    echo -e "${C_GRN}PASS${C_RST} login returned token"
    PASSED=$((PASSED + 1))
else
    echo -e "${C_RED}FAIL${C_RST} login failed"
    FAILED=$((FAILED + 1))
fi

# profile
req GET /auth/profile
assert_status "$HTTP_CODE" 200 "profile GET → 200"
assert_contains "$BODY" "$EMAIL" "profile contains email"

# profile no token
SAVED_TOKEN="$TOKEN"; TOKEN=""
req GET /auth/profile
assert_status "$HTTP_CODE" 401 "profile without token → 401"
TOKEN="$SAVED_TOKEN"

# profile update with birthday
req PUT /auth/profile '{"name":"Updated Name","birthday":"1998-05-15T00:00:00Z"}'
assert_status "$HTTP_CODE" 200 "profile PUT → 200"
assert_contains "$BODY" "Updated Name" "profile name updated"
assert_contains "$BODY" "1998-05-15" "birthday persisted"

# bad birthday
req PUT /auth/profile '{"birthday":"not-a-date"}'
assert_status "$HTTP_CODE" 400 "bad birthday → 400"
assert_contains "$BODY" "INVALID_DATE" "error code INVALID_DATE"

# register with birthday
SUFFIX2=$(rand_suffix)
EMAIL2="bday_${SUFFIX2}@test.com"
req_anon POST /auth/register "{\"name\":\"BDay $SUFFIX2\",\"email\":\"$EMAIL2\",\"password\":\"Sifre123!\",\"birthday\":\"2000-01-01T00:00:00Z\"}"
assert_status "$HTTP_CODE" 201 "register with birthday → 201"
assert_contains "$BODY" "2000-01-01" "register response includes birthday"

summary
