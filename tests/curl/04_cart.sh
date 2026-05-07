#!/usr/bin/env bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/_lib.sh"

echo "── Cart ──────────────────────────────"
SUFFIX=$(rand_suffix)
register "K${SUFFIX}" "k_${SUFFIX}@test.com" "Sifre123!"

# empty cart
body=$(req GET /cart)
assert_status "$HTTP_CODE" 200 "empty cart → 200"
assert_contains "$body" "item_count" "cart has item_count"

# add item
body=$(req POST /cart/items '{"product_id":"prd_002","size":"42","quantity":1}')
assert_status "$HTTP_CODE" 201 "add cart item → 201"
ITEM_ID=$(printf "%s" "$body" | sed -n 's/.*"id":"\(crt_[^"]*\)".*/\1/p' | head -n1)
assert_contains "$body" "prd_002" "add item product id present"

# list with item
body=$(req GET /cart)
assert_status "$HTTP_CODE" 200 "cart list → 200"
assert_contains "$body" "$ITEM_ID" "list includes new item"

# update qty
body=$(req PUT "/cart/items/$ITEM_ID" '{"quantity":3}')
assert_status "$HTTP_CODE" 200 "update qty → 200"
assert_contains "$body" "\"quantity\":3" "qty updated to 3"

# bad qty
body=$(req PUT "/cart/items/$ITEM_ID" '{"quantity":0}')
assert_status "$HTTP_CODE" 400 "qty=0 → 400"

# delete
body=$(req DELETE "/cart/items/$ITEM_ID")
assert_status "$HTTP_CODE" 204 "delete cart item → 204"

# delete unknown
body=$(req DELETE "/cart/items/crt_doesnotexist")
assert_status "$HTTP_CODE" 404 "delete unknown → 404"

summary
