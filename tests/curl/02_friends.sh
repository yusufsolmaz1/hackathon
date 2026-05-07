#!/usr/bin/env bash
# Friends BFF — list, add (4 error code path'i), delete
set -u
cd "$(dirname "$0")"
. ./_lib.sh

section "2.1) GET /friends"
RESP=$(http_get /friends "X-User-Id: u_self")
assert_status 200 "$(status_of "$RESP")" "list friends"
N=$(jpath "$(body_of "$RESP")" "len(d['friends'])")
note "friends count = $N"

section "2.2) POST /friends/add — success (yeni@example.com)"
RESP=$(http_post /friends/add '{"email":"yeni@example.com"}' "X-User-Id: u_self")
assert_status 200 "$(status_of "$RESP")" "add (success)"
ID=$(jpath "$(body_of "$RESP")" "d['friend']['id']")
[[ "$ID" == "u_123" ]] && ok "friend.id=u_123" || fail "friend.id=$ID"

section "2.2) ALREADY_FRIEND (tekrar ekle)"
RESP=$(http_post /friends/add '{"email":"yeni@example.com"}' "X-User-Id: u_self")
assert_status 409 "$(status_of "$RESP")" "ALREADY_FRIEND → 409"
CODE=$(jpath "$(body_of "$RESP")" "d['errorCode']")
[[ "$CODE" == "ALREADY_FRIEND" ]] && ok "errorCode=ALREADY_FRIEND" || fail "errorCode=$CODE"

section "2.2) INVALID_EMAIL"
RESP=$(http_post /friends/add '{"email":"bozuk-mail"}' "X-User-Id: u_self")
assert_status 400 "$(status_of "$RESP")" "INVALID_EMAIL → 400"
CODE=$(jpath "$(body_of "$RESP")" "d['errorCode']")
[[ "$CODE" == "INVALID_EMAIL" ]] && ok "errorCode=INVALID_EMAIL" || fail "errorCode=$CODE"

section "2.2) USER_NOT_FOUND"
RESP=$(http_post /friends/add '{"email":"yok@example.com"}' "X-User-Id: u_self")
assert_status 404 "$(status_of "$RESP")" "USER_NOT_FOUND → 404"
CODE=$(jpath "$(body_of "$RESP")" "d['errorCode']")
[[ "$CODE" == "USER_NOT_FOUND" ]] && ok "errorCode=USER_NOT_FOUND" || fail "errorCode=$CODE"

section "2.2) SELF_ADD"
RESP=$(http_post /friends/add '{"email":"berna@example.com"}' "X-User-Id: u_self")
assert_status 400 "$(status_of "$RESP")" "SELF_ADD → 400"
CODE=$(jpath "$(body_of "$RESP")" "d['errorCode']")
[[ "$CODE" == "SELF_ADD" ]] && ok "errorCode=SELF_ADD" || fail "errorCode=$CODE"

section "2.3) DELETE /friends/u_123"
RESP=$(http_delete /friends/u_123 "X-User-Id: u_self")
assert_status 204 "$(status_of "$RESP")" "delete u_123"

section "2.3) DELETE again → 404"
RESP=$(http_delete /friends/u_123 "X-User-Id: u_self")
assert_status 404 "$(status_of "$RESP")" "delete missing edge"

summary
