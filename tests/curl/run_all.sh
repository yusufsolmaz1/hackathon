#!/usr/bin/env bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE=${BASE:-http://localhost:8080}

echo "BASE = $BASE"
echo

TOTAL_PASS=0
TOTAL_FAIL=0

for suite in 00_auth.sh 01_products.sh 02_friends.sh 03_collections.sh 04_cart.sh 05_orders.sh 06_notifications.sh; do
    out=$(bash "$SCRIPT_DIR/$suite")
    echo "$out"
    p=$(echo "$out" | sed -n 's/.*passed: \([0-9]*\).*/\1/p' | tail -n1)
    f=$(echo "$out" | sed -n 's/.*failed: \([0-9]*\).*/\1/p' | tail -n1)
    TOTAL_PASS=$((TOTAL_PASS + ${p:-0}))
    TOTAL_FAIL=$((TOTAL_FAIL + ${f:-0}))
    echo
done

echo "═════════════════════════════════════"
echo "  TOTAL passed: $TOTAL_PASS    failed: $TOTAL_FAIL"
echo "═════════════════════════════════════"
[ "$TOTAL_FAIL" -eq 0 ]
