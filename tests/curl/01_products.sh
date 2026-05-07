#!/usr/bin/env bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/_lib.sh"

echo "── Products ──────────────────────────"
SUFFIX=$(rand_suffix)
register "P${SUFFIX}" "p_${SUFFIX}@test.com" "Sifre123!"

# list
body=$(req GET /products)
assert_status "$HTTP_CODE" 200 "GET /products → 200"
assert_contains "$body" "prd_001" "list contains seeded prd_001"

# detail
body=$(req GET /products/prd_001)
assert_status "$HTTP_CODE" 200 "GET /products/prd_001 → 200"
assert_contains "$body" "Zara" "detail brand=Zara"

# 404
body=$(req GET /products/prd_doesnotexist)
assert_status "$HTTP_CODE" 404 "GET unknown product → 404"

# search
body=$(req GET "/products/search?q=nike")
assert_status "$HTTP_CODE" 200 "search q=nike → 200"
assert_contains "$body" "Nike" "search returns Nike"

# favorite toggle on
body=$(req PUT /products/prd_002/favorite)
assert_status "$HTTP_CODE" 200 "favorite toggle → 200"
assert_contains "$body" "is_favorite" "favorite payload contains flag"

# favorites list
body=$(req GET /products/favorites)
assert_status "$HTTP_CODE" 200 "favorites list → 200"
assert_contains "$body" "prd_002" "favorites includes prd_002"

# like
body=$(req PUT /products/prd_002/like '{"status":"liked"}')
assert_status "$HTTP_CODE" 200 "like → 200"
assert_contains "$body" "liked" "like response status=liked"

# clear like
body=$(req PUT /products/prd_002/like '{"status":"none"}')
assert_status "$HTTP_CODE" 200 "clear like → 200"

summary
