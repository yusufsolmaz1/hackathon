#!/usr/bin/env bash
# Supabase seed: users / friends / orders.
# Idempotent — defalarca çalıştırılabilir (Prefer: resolution=ignore-duplicates).

set -eu
cd "$(dirname "$0")"
. ./_lib.sh

[[ -n "${SUPABASE_URL:-}" ]] || { echo "SUPABASE_URL yok (.env ayarla)"; exit 1; }
[[ -n "${SUPABASE_KEY:-}" ]] || { echo "SUPABASE_KEY yok (.env ayarla)"; exit 1; }

H_AUTH=(-H "apikey: $SUPABASE_KEY" -H "Authorization: Bearer $SUPABASE_KEY"
        -H "Content-Type: application/json"
        -H "Prefer: return=representation,resolution=ignore-duplicates")

section "Seed users"
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST "$SUPABASE_URL/rest/v1/users" "${H_AUTH[@]}" -d '[
  {"id":"u_self","name":"Berna Tek","email":"berna@example.com","initials":"BT"},
  {"id":"u_42","name":"Ali Yılmaz","email":"ali@example.com","initials":"AY"},
  {"id":"u_99","name":"Zeynep Kaya","email":"zeynep@example.com","initials":"ZK"},
  {"id":"u_123","name":"Yeni Kişi","email":"yeni@example.com","initials":"YK"}
]'

section "Seed friends (u_self ↔ u_42, u_self ↔ u_99)"
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST "$SUPABASE_URL/rest/v1/friends" "${H_AUTH[@]}" -d '[
  {"user_id":"u_self","friend_id":"u_42"},
  {"user_id":"u_42","friend_id":"u_self"},
  {"user_id":"u_self","friend_id":"u_99"},
  {"user_id":"u_99","friend_id":"u_self"}
]'

section "Seed order ORD-7700219"
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST "$SUPABASE_URL/rest/v1/orders" "${H_AUTH[@]}" -d '{
  "id":"ORD-7700219",
  "user_id":"u_self",
  "payment_info":{
    "cardImageUrl":"https://img/card.png",
    "paymentDescription":"Bonus Kart **** 1234",
    "totalPrice":"120,00 TL",
    "paymentItems":[],
    "paymentType":"CREDIT_CARD",
    "isCargoBundleUsed":false,
    "umicoInfoItems":[]
  }
}'

# u_123 başlangıçta arkadaş değil — ALREADY_FRIEND testi pekişsin diye listeyi temizle
section "u_123 friends-state reset (delete edge if exists)"
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X DELETE \
  "$SUPABASE_URL/rest/v1/friends?or=(and(user_id.eq.u_self,friend_id.eq.u_123),and(user_id.eq.u_123,friend_id.eq.u_self))" \
  -H "apikey: $SUPABASE_KEY" -H "Authorization: Bearer $SUPABASE_KEY"

echo
ok "Seed tamamlandı"
