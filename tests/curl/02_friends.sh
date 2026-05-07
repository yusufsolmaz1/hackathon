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
TOKEN="$A_TOKEN"

# add B as friend
body=$(req POST /friends "{\"friend_id\":\"$B_ID\"}")
assert_status "$HTTP_CODE" 201 "add friend → 201"
assert_contains "$body" "$B_ID" "added friend id present"

# list
body=$(req GET /friends)
assert_status "$HTTP_CODE" 200 "list friends → 200"
assert_contains "$body" "$B_ID" "list contains B"

# self-add → 400
body=$(req POST /friends "{\"friend_id\":\"$A_ID\"}")
assert_status "$HTTP_CODE" 400 "self-add → 400"

# unknown → 404
body=$(req POST /friends '{"friend_id":"usr_doesnotexist"}')
assert_status "$HTTP_CODE" 404 "unknown friend → 404"

# sync via email
body=$(req POST /friends/sync "{\"contact_emails\":[\"$EMAIL_B\",\"missing_${SUFFIX}@nowhere.com\"]}")
assert_status "$HTTP_CODE" 200 "sync friends → 200"
assert_contains "$body" "not_found" "sync response has not_found"

# delete
body=$(req DELETE "/friends/$B_ID")
assert_status "$HTTP_CODE" 204 "delete friend → 204"

summary
