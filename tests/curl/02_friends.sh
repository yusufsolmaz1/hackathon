#!/usr/bin/env bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/_lib.sh"

echo "── Friends ───────────────────────────"
SUFFIX=$(rand_suffix)
EMAIL_A="a_${SUFFIX}@test.com"; EMAIL_B="b_${SUFFIX}@test.com"

# user A
register "Alice ${SUFFIX}" "$EMAIL_A" "Sifre123!"
A_TOKEN="$TOKEN"; A_ID="$USER_ID"

# user B
register "Bob ${SUFFIX}" "$EMAIL_B" "Sifre123!"
B_ID="$USER_ID"

# back to A
TOKEN="$A_TOKEN"; USER_ID="$A_ID"

req POST /friends "{\"friend_id\":\"$B_ID\"}"
assert_status "$HTTP_CODE" 201 "add friend → 201"
assert_contains "$BODY" "$B_ID" "added friend id present"

req GET /friends
assert_status "$HTTP_CODE" 200 "list friends → 200"
assert_contains "$BODY" "$B_ID" "list contains B"

req POST /friends "{\"friend_id\":\"$A_ID\"}"
assert_status "$HTTP_CODE" 400 "self-add → 400"

req POST /friends '{"friend_id":"usr_doesnotexist"}'
assert_status "$HTTP_CODE" 400 "unknown friend → 400 (USER_NOT_FOUND)"

req POST /friends/sync "{\"contact_emails\":[\"$EMAIL_B\",\"missing_${SUFFIX}@nowhere.com\"]}"
assert_status "$HTTP_CODE" 200 "sync friends → 200"
assert_contains "$BODY" "not_found" "sync response has not_found"

req DELETE "/friends/$B_ID"
assert_status "$HTTP_CODE" 204 "delete friend → 204"

summary
