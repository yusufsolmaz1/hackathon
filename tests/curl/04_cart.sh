#!/usr/bin/env bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/_lib.sh"

echo "── Cart ──────────────────────────────"
SUFFIX=$(rand_suffix)
register "K${SUFFIX}" "k_${SUFFIX}@test.com" "Sifre123!"

req GET /cart
assert_status "$HTTP_CODE" 200 "empty cart → 200"
assert_contains "$BODY" "item_count" "cart has item_count"

req POST /cart/items '{"product_id":"prd_002","size":"42","quantity":1}'
assert_status "$HTTP_CODE" 201 "add cart item → 201"
ITEM_ID=$(printf "%s" "$BODY" | tr -d '\n' | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\(crt_[^"]*\)".*/\1/p')
assert_contains "$BODY" "prd_002" "add item product id present"

req GET /cart
assert_status "$HTTP_CODE" 200 "cart list → 200"
assert_contains "$BODY" "$ITEM_ID" "list includes new item"

req PUT "/cart/items/$ITEM_ID" '{"quantity":3}'
assert_status "$HTTP_CODE" 200 "update qty → 200"
assert_contains "$BODY" '"quantity": 3' "qty updated to 3"

req PUT "/cart/items/$ITEM_ID" '{"quantity":0}'
assert_status "$HTTP_CODE" 400 "qty=0 → 400"

req DELETE "/cart/items/$ITEM_ID"
assert_status "$HTTP_CODE" 204 "delete cart item → 204"

req DELETE "/cart/items/crt_doesnotexist"
assert_status "$HTTP_CODE" 404 "delete unknown → 404"

summary
