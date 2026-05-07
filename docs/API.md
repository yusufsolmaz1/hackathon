# TrendSocial API

Base URL (Render): `https://hackathon-dkpy.onrender.com`
Base URL (Local):  `http://localhost:8080`

OpenAPI: [`openapi.yaml`](./openapi.yaml) — Swagger UI: [`swagger.html`](./swagger.html)

## Auth

Tüm endpoint'ler (login/register hariç) `Authorization: Bearer <token>` ister.

| Method | Path | Desc |
|---|---|---|
| POST | `/auth/login` | E-posta + şifre ile giriş, token döner |
| POST | `/auth/register` | Yeni kullanıcı oluştur, token döner (201). Opsiyonel `birthday` (ISO 8601). |
| GET  | `/auth/profile` | Mevcut kullanıcı profilini döndür (`birthday` dahil) |
| PUT  | `/auth/profile` | Profil günceller (name, email, avatar_url, birthday) |

`birthday` formatı ISO 8601 — örn. `"1998-05-15T00:00:00Z"`. Yalnızca tarih (`"1998-05-15"`) gönderilirse UTC midnight olarak normalize edilir. Geçersiz formatta `400 INVALID_DATE` döner.

## Products

| Method | Path | Desc |
|---|---|---|
| GET  | `/products` | Tüm ürünler (is_favorite alanı kullanıcıya göre) |
| GET  | `/products/{id}` | Tek ürün |
| GET  | `/products/search?q=...` | Ad/marka içinde arama |
| GET  | `/products/favorites` | Kullanıcının favori ürünleri |
| PUT  | `/products/{id}/favorite` | Favori toggle (yoksa ekle, varsa çıkar) |
| PUT  | `/products/{id}/like` | `{"status":"liked"|"disliked"|"none"}` ile beğen/dislike |

## Friends

| Method | Path | Desc |
|---|---|---|
| GET    | `/friends` | Arkadaş listesi |
| POST   | `/friends` | `{"friend_id":"usr_..."}` — bidirectional kayıt (201) |
| DELETE | `/friends/{id}` | Bidirectional sil (204) |
| POST   | `/friends/sync` | `{"contact_emails":[...]}` — eşleşen kullanıcıları arkadaş ekler |

## Collections

| Method | Path | Desc |
|---|---|---|
| GET  | `/collections` | Sahip olunan + katılınan koleksiyonlar |
| POST | `/collections` | Yeni koleksiyon (201) — `{name, description?, image_name?, product_ids?, is_shared?}` |
| GET  | `/collections/{id}` | Detay (products + participants) |
| POST | `/collections/{id}/share` | `{"friend_ids":[...]}` ile paylaş, is_shared=true yapar |

## Cart

| Method | Path | Desc |
|---|---|---|
| GET    | `/cart` | Sepet (items + total_price + item_count) |
| POST   | `/cart/items` | Ekle (201) — `{product_id, size, quantity?}`. Aynı (product_id+size) varsa miktar artar. |
| PUT    | `/cart/items/{id}` | Miktar güncelle — `{quantity}` |
| DELETE | `/cart/items/{id}` | Sil (204) |

## Orders

| Method | Path | Desc |
|---|---|---|
| GET  | `/orders` | Sipariş geçmişi |
| POST | `/orders` | Sipariş oluştur (201) — `{cart_item_ids?}` boş ise tüm sepet kullanılır |
| POST | `/orders/split` | Ortak ödemeli sipariş (201) — `{cart_item_ids?, friend_ids:[...]}` |
| GET  | `/orders/{id}` | Detay (items + split_participants) |
| GET  | `/orders/{id}/split-status` | Ortak ödeme durumu (paid_amount, remaining, all_paid) |

## Misc

| Method | Path | Desc |
|---|---|---|
| GET | `/health` | `{"status":"ok"}` (auth'suz) |

## Hata formatı

```json
{ "error": { "code": "NOT_FOUND", "message": "Ürün bulunamadı." } }
```

Code'lar: `UNAUTHORIZED` (401), `FORBIDDEN` (403), `NOT_FOUND` (404), `BAD_REQUEST` (400),
`EMAIL_EXISTS` (409), `USER_NOT_FOUND` (404), `PRODUCT_NOT_FOUND` (404), `SERVER_ERROR` (500).

## Hızlı dene

```bash
BASE=https://hackathon-dkpy.onrender.com
TOKEN=$(curl -s -X POST $BASE/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"smoke@test.com","password":"Sifre123!"}' | jq -r .token)

curl -H "Authorization: Bearer $TOKEN" $BASE/products
curl -H "Authorization: Bearer $TOKEN" $BASE/auth/profile
```

Smoke test suite:
```bash
BASE=$BASE ./tests/curl/run_all.sh
```
