#!/usr/bin/env bash
# Split Payment BFF — initiate, pay, request, reject, tracking, remind, cancel
set -u
cd "$(dirname "$0")"
. ./_lib.sh

section "0) /health"
RESP=$(http_get /health)
assert_status 200 "$(status_of "$RESP")" "GET /health"

section "1.1) POST /split-payment/initiate"
RESP=$(http_post /split-payment/initiate '{
  "totalKurus": 12000,
  "splitMethod": "EQUAL",
  "participants": [
    {"participantId":"u_self","shareKurus":4000,"isInitiator":true},
    {"participantId":"u_42","shareKurus":4000,"isInitiator":false},
    {"participantId":"u_99","shareKurus":4000,"isInitiator":false}
  ]
}')
assert_status 201 "$(status_of "$RESP")" "initiate (EQUAL)"
SPLIT_ID=$(jpath "$(body_of "$RESP")" "d['splitId']")
note "splitId = $SPLIT_ID"

section "1.1) validation: CUSTOM toplam tutmuyor"
RESP=$(http_post /split-payment/initiate '{
  "totalKurus":12000,"splitMethod":"CUSTOM","participants":[
    {"participantId":"u_self","shareKurus":5000,"isInitiator":true},
    {"participantId":"u_42","shareKurus":5000,"isInitiator":false}
  ]}')
assert_status 400 "$(status_of "$RESP")" "CUSTOM mismatch → 400"

section "1.3) GET /split-payment/request/{splitId}/u_42"
RESP=$(http_get /split-payment/request/$SPLIT_ID/u_42)
assert_status 200 "$(status_of "$RESP")" "request (u_42)"
STATUS=$(jpath "$(body_of "$RESP")" "d['status']")
[[ "$STATUS" == "PENDING" ]] && ok "status=PENDING" || fail "status=$STATUS (expected PENDING)"

section "1.5) GET tracking — initiator gözünden"
RESP=$(http_get /split-payment/$SPLIT_ID "X-User-Id: u_self")
assert_status 200 "$(status_of "$RESP")" "tracking (u_self)"
ROLE=$(jpath "$(body_of "$RESP")" "d['role']")
[[ "$ROLE" == "INITIATOR" ]] && ok "role=INITIATOR" || fail "role=$ROLE"

section "1.5) GET tracking — participant gözünden"
RESP=$(http_get /split-payment/$SPLIT_ID "X-User-Id: u_42")
assert_status 200 "$(status_of "$RESP")" "tracking (u_42)"
ROLE=$(jpath "$(body_of "$RESP")" "d['role']")
[[ "$ROLE" == "PARTICIPANT" ]] && ok "role=PARTICIPANT" || fail "role=$ROLE"

section "1.2) POST /split-payment/initiator-pay/{splitId}"
RESP=$(http_post /split-payment/initiator-pay/$SPLIT_ID '{"orderId":"ORD-7700219"}')
assert_status 200 "$(status_of "$RESP")" "initiator-pay"
DEEPLINK=$(jpath "$(body_of "$RESP")" "d['trackingDeeplink']")
[[ "$DEEPLINK" == "ty://?Page=OrtakOdemeTakip&SplitId=$SPLIT_ID" ]] \
    && ok "deeplink doğru" || fail "deeplink=$DEEPLINK"

section "1.4) POST /split-payment/reject/{splitId} — u_99 reddeder"
RESP=$(http_post /split-payment/reject/$SPLIT_ID '' "X-User-Id: u_99")
assert_status 204 "$(status_of "$RESP")" "reject (u_99)"

section "1.4) reject PAID participant → 400"
RESP=$(http_post /split-payment/reject/$SPLIT_ID '' "X-User-Id: u_self")
assert_status 400 "$(status_of "$RESP")" "reject u_self (PAID)"

section "1.6) POST /{splitId}/remind/{participantId}"
RESP=$(http_post /split-payment/$SPLIT_ID/remind/u_42 '')
assert_status 204 "$(status_of "$RESP")" "remind u_42 (PENDING)"

section "1.6) remind non-PENDING → 400"
RESP=$(http_post /split-payment/$SPLIT_ID/remind/u_99 '')   # u_99 REJECTED
assert_status 400 "$(status_of "$RESP")" "remind u_99 (REJECTED)"

section "1.7) POST /{splitId}/cancel"
RESP=$(http_post /split-payment/$SPLIT_ID/cancel '')
assert_status 204 "$(status_of "$RESP")" "cancel"

section "1.7) cancel tekrar → 400"
RESP=$(http_post /split-payment/$SPLIT_ID/cancel '')
assert_status 400 "$(status_of "$RESP")" "double cancel"

section "Order detail chip — splitId dolu"
RESP=$(http_get /orders/ORD-7700219)
assert_status 200 "$(status_of "$RESP")" "GET /orders/ORD-7700219"
CHIP=$(jpath "$(body_of "$RESP")" "d['paymentInfo'].get('splitId')")
[[ -n "$CHIP" && "$CHIP" != "None" ]] \
    && ok "paymentInfo.splitId='$CHIP'" \
    || fail "paymentInfo.splitId boş"

summary
