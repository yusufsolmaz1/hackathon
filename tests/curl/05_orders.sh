#!/usr/bin/env bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/_lib.sh"

echo "── Orders ────────────────────────────"
SUFFIX=$(rand_suffix)

# Create owner with cart
register "O${SUFFIX}" "o_${SUFFIX}@test.com" "Sifre123!"
OWNER_TOKEN="$TOKEN"; OWNER_ID="$USER_ID"

# Empty cart → create order should 400
body=$(req POST /orders '{}')
assert_status "$HTTP_CODE" 400 "create with empty cart → 400"

# add 2 cart items
req POST /cart/items '{"product_id":"prd_001","size":"M","quantity":1}' >/dev/null
req POST /cart/items '{"product_id":"prd_004","size":"42","quantity":2}' >/dev/null

# create normal order
body=$(req POST /orders '{}')
assert_status "$HTTP_CODE" 201 "create order → 201"
ORDER_ID=$(printf "%s" "$body" | sed -n 's/.*"id":"\(ord_[^"]*\)".*/\1/p' | head -n1)
assert_contains "$body" "order_number" "order has order_number"

# list orders
body=$(req GET /orders)
assert_status "$HTTP_CODE" 200 "list orders → 200"
assert_contains "$body" "$ORDER_ID" "list includes new order"

# detail
body=$(req GET "/orders/$ORDER_ID")
assert_status "$HTTP_CODE" 200 "detail → 200"
assert_contains "$body" "items" "detail has items"

# split-status on non-split → 400
body=$(req GET "/orders/$ORDER_ID/split-status")
assert_status "$HTTP_CODE" 400 "split-status on normal order → 400"

# Setup friend + split order
SUFFIX2=$(rand_suffix)
SAVED_T="$TOKEN"; SAVED_U="$USER_ID"
register "OF${SUFFIX2}" "of_${SUFFIX2}@test.com" "Sifre123!"
FRIEND_ID="$USER_ID"
TOKEN="$SAVED_T"; USER_ID="$SAVED_U"

# add friend (must be friends to split with them)
req POST /friends "{\"friend_id\":\"$FRIEND_ID\"}" >/dev/null

# add cart item again (previous cart was consumed)
req POST /cart/items '{"product_id":"prd_002","size":"42","quantity":1}' >/dev/null

# split order
body=$(req POST /orders/split "{\"friend_ids\":[\"$FRIEND_ID\"]}")
assert_status "$HTTP_CODE" 201 "create split order → 201"
SPLIT_ID=$(printf "%s" "$body" | sed -n 's/.*"id":"\(ord_[^"]*\)".*/\1/p' | head -n1)
assert_contains "$body" "split_participants" "has split_participants"

# split-status
body=$(req GET "/orders/$SPLIT_ID/split-status")
assert_status "$HTTP_CODE" 200 "split-status → 200"
assert_contains "$body" "all_paid" "status has all_paid"

summary
