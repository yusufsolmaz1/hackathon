#!/usr/bin/env bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/_lib.sh"

echo "── Collections ───────────────────────"
SUFFIX=$(rand_suffix)
register "C${SUFFIX}" "c_${SUFFIX}@test.com" "Sifre123!"

# create
body=$(req POST /collections '{"name":"Yaz Listesi","description":"sezonluk","product_ids":["prd_001","prd_002"],"is_shared":false}')
assert_status "$HTTP_CODE" 201 "create collection → 201"
COL_ID=$(printf "%s" "$body" | sed -n 's/.*"id":"\(col_[^"]*\)".*/\1/p' | head -n1)
assert_contains "$body" "Yaz Listesi" "create returns name"

# list
body=$(req GET /collections)
assert_status "$HTTP_CODE" 200 "list collections → 200"
assert_contains "$body" "$COL_ID" "list includes new collection"

# detail
body=$(req GET "/collections/$COL_ID")
assert_status "$HTTP_CODE" 200 "get collection → 200"
assert_contains "$body" "prd_001" "detail includes products"

# create a friend to share with
SUFFIX2=$(rand_suffix)
SAVED_TOKEN="$TOKEN"; SAVED_ID="$USER_ID"
register "Friend${SUFFIX2}" "f_${SUFFIX2}@test.com" "Sifre123!"
FRIEND_ID="$USER_ID"
TOKEN="$SAVED_TOKEN"; USER_ID="$SAVED_ID"

# share
body=$(req POST "/collections/$COL_ID/share" "{\"friend_ids\":[\"$FRIEND_ID\"]}")
assert_status "$HTTP_CODE" 200 "share collection → 200"
assert_contains "$body" "is_shared" "share response includes flag"

summary
