#!/usr/bin/env bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/_lib.sh"

echo "── Orders ────────────────────────────"
SUFFIX=$(rand_suffix)

register "O${SUFFIX}" "o_${SUFFIX}@test.com" "Sifre123!"
OWNER_TOKEN="$TOKEN"; OWNER_ID="$USER_ID"

# Empty cart → create order should 400
req POST /orders '{}'
assert_status "$HTTP_CODE" 400 "create with empty cart → 400"

# add 2 cart items
req POST /cart/items '{"product_id":"prd_001","size":"M","quantity":1}'
req POST /cart/items '{"product_id":"prd_004","size":"42","quantity":2}'

# create normal order
req POST /orders '{}'
assert_status "$HTTP_CODE" 201 "create order → 201"
ORDER_ID=$(printf "%s" "$BODY" | tr -d '\n' | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\(ord_[^"]*\)".*/\1/p')
assert_contains "$BODY" "order_number" "order has order_number"

req GET /orders
assert_status "$HTTP_CODE" 200 "list orders → 200"
assert_contains "$BODY" "$ORDER_ID" "list includes new order"

req GET "/orders/$ORDER_ID"
assert_status "$HTTP_CODE" 200 "detail → 200"
assert_contains "$BODY" "items" "detail has items"

req GET "/orders/$ORDER_ID/split-status"
assert_status "$HTTP_CODE" 400 "split-status on normal order → 400"

# Setup friend + split order
SUFFIX2=$(rand_suffix)
SAVED_T="$TOKEN"; SAVED_U="$USER_ID"
register "OF${SUFFIX2}" "of_${SUFFIX2}@test.com" "Sifre123!"
FRIEND_ID="$USER_ID"
TOKEN="$SAVED_T"; USER_ID="$SAVED_U"

req POST /friends "{\"friend_id\":\"$FRIEND_ID\"}"

req POST /cart/items '{"product_id":"prd_002","size":"42","quantity":1}'

req POST /orders/split "{\"friend_ids\":[\"$FRIEND_ID\"]}"
assert_status "$HTTP_CODE" 201 "create split order → 201"
SPLIT_ID=$(printf "%s" "$BODY" | tr -d '\n' | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\(ord_[^"]*\)".*/\1/p')
assert_contains "$BODY" "split_participants" "has split_participants"

req GET "/orders/$SPLIT_ID/split-status"
assert_status "$HTTP_CODE" 200 "split-status → 200"
assert_contains "$BODY" "all_paid" "status has all_paid"

summary
