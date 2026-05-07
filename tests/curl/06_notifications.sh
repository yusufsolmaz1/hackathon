#!/usr/bin/env bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/_lib.sh"

echo "── Notifications ─────────────────────"
SUFFIX=$(rand_suffix)

# Two users — A triggers events, B receives notifications
register "NA${SUFFIX}" "na_${SUFFIX}@test.com" "Sifre123!"
A_TOKEN="$TOKEN"; A_ID="$USER_ID"

register "NB${SUFFIX}" "nb_${SUFFIX}@test.com" "Sifre123!"
B_TOKEN="$TOKEN"; B_ID="$USER_ID"

# B'nin baslangic bildirim listesi (bos olmali)
TOKEN="$B_TOKEN"; USER_ID="$B_ID"
req GET /notifications
assert_status "$HTTP_CODE" 200 "list (empty) → 200"

# A → B arkadas ekle (B'ye friend_request bildirimi gitmeli)
TOKEN="$A_TOKEN"; USER_ID="$A_ID"
req POST /friends "{\"friend_id\":\"$B_ID\"}"
assert_status "$HTTP_CODE" 201 "A adds B → 201"

# Kisa bir bekleme — Supabase yazmasinin propagasyonu icin
sleep 1

TOKEN="$B_TOKEN"; USER_ID="$B_ID"
req GET /notifications
assert_status "$HTTP_CODE" 200 "B list → 200"
assert_contains "$BODY" "friend_request" "B has friend_request notification"
assert_contains "$BODY" "NA${SUFFIX}" "notification body mentions A's name"

# Ilk bildirimin id'sini al
NTF_ID=$(printf "%s" "$BODY" | tr -d '\n' | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\(ntf_[^"]*\)".*/\1/p')
[ -n "$NTF_ID" ] && echo -e "${C_GRN}PASS${C_RST} extracted notification id ($NTF_ID)" && PASSED=$((PASSED + 1)) \
  || { echo -e "${C_RED}FAIL${C_RST} could not extract ntf_ id"; FAILED=$((FAILED + 1)); }

# is_read kontrol — false olmali
assert_contains "$BODY" '"is_read": false' "notification is_read=false initially"

# Mark as read
req PUT "/notifications/$NTF_ID/read"
assert_status "$HTTP_CODE" 200 "mark read → 200"
assert_contains "$BODY" "okundu" "response message contains 'okundu'"

# Tekrar listele, is_read=true olmali
req GET /notifications
assert_contains "$BODY" '"is_read": true' "notification is_read=true after mark"

# Bilinmeyen id → 404
req PUT /notifications/ntf_doesnotexist/read
assert_status "$HTTP_CODE" 404 "mark unknown → 404"

# Baskasinin bildirimini okumaya calismak → 404 (gizleme)
TOKEN="$A_TOKEN"; USER_ID="$A_ID"
req PUT "/notifications/$NTF_ID/read"
assert_status "$HTTP_CODE" 404 "mark other user's ntf → 404"

# Auth header yok → 401
req_anon GET /notifications
assert_status "$HTTP_CODE" 401 "list without token → 401"

# after parametresi (gelecek tarih → bos donmeli)
TOKEN="$B_TOKEN"; USER_ID="$B_ID"
req GET "/notifications?after=2099-01-01T00:00:00Z"
assert_status "$HTTP_CODE" 200 "after=future → 200"
# Bos array
[ "$BODY" = "[]" ] || [[ "$BODY" =~ ^\[[[:space:]]*\]$ ]] && \
  echo -e "${C_GRN}PASS${C_RST} after=future returns empty array" && PASSED=$((PASSED + 1)) \
  || { echo -e "${C_RED}FAIL${C_RST} after=future returned: $(echo "$BODY" | head -c 100)"; FAILED=$((FAILED + 1)); }

# Sepete urun ekle, normal siparis ver → order_confirmed bildirimi
req POST /cart/items '{"product_id":"prd_001","size":"M","quantity":1}'
req POST /orders '{}'
assert_status "$HTTP_CODE" 201 "B creates order → 201"

sleep 1
req GET /notifications
assert_contains "$BODY" "order_confirmed" "B has order_confirmed notification"

# A'ya geri don, A'nin koleksiyonu B ile paylas → collection_shared
TOKEN="$A_TOKEN"; USER_ID="$A_ID"
req POST /collections '{"name":"Notif Test","is_shared":false,"product_ids":["prd_001"]}'
COL_ID=$(printf "%s" "$BODY" | tr -d '\n' | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\(col_[^"]*\)".*/\1/p')

req POST "/collections/$COL_ID/share" "{\"friend_ids\":[\"$B_ID\"]}"
assert_status "$HTTP_CODE" 200 "share collection → 200"

sleep 1
TOKEN="$B_TOKEN"; USER_ID="$B_ID"
req GET /notifications
assert_contains "$BODY" "collection_shared" "B has collection_shared notification"
assert_contains "$BODY" "Notif Test" "collection name in body"

# Split order — A B'ye split atar → split_payment_received
TOKEN="$A_TOKEN"; USER_ID="$A_ID"
req POST /cart/items '{"product_id":"prd_002","size":"42","quantity":1}'
req POST /orders/split "{\"friend_ids\":[\"$B_ID\"]}"
assert_status "$HTTP_CODE" 201 "A creates split order → 201"

sleep 1
TOKEN="$B_TOKEN"; USER_ID="$B_ID"
req GET /notifications
assert_contains "$BODY" "split_payment_received" "B has split_payment_received notification"

summary
