# Hackathon BFF — API Reference

Split Payment + Friends + Order detail BFF.

**Production base URL ayrımı (MultiDC remote-config anahtarları):**
- `AndroidSplitPaymentBaseUrl` → `/split-payment/*`
- `AndroidFriendsBaseUrl` → `/friends/*`

Hackathon implementasyonunda her iki BFF aynı Ktor sunucusunda farklı prefix'ler altında host ediliyor.

**Base URL (local dev):** `http://localhost:8080`

**Auth shim:** `X-User-Id` header — gerçek production'da JWT'den çıkarılır.

---

## İçindekiler

- [Health](#health)
- [Split Payment](#split-payment)
  - [POST /split-payment/initiate](#11-post-split-paymentinitiate)
  - [POST /split-payment/initiator-pay/{splitId}](#12-post-split-paymentinitiator-paysplitid)
  - [GET /split-payment/request/{splitId}/{participantId}](#13-get-split-paymentrequestsplitidparticipantid)
  - [POST /split-payment/reject/{splitId}](#14-post-split-paymentrejectsplitid)
  - [GET /split-payment/{splitId}](#15-get-split-paymentsplitid)
  - [POST /split-payment/{splitId}/remind/{participantId}](#16-post-split-paymentsplitidremindparticipantid)
  - [POST /split-payment/{splitId}/cancel](#17-post-split-paymentsplitidcancel)
- [Friends](#friends)
  - [GET /friends](#21-get-friends)
  - [POST /friends/add](#22-post-friendsadd)
  - [DELETE /friends/{friendId}](#23-delete-friendsfriendid)
- [Orders](#orders)
  - [GET /orders/{id}](#3-get-ordersid)
- [Items](#items)
- [Ortak Tipler](#ortak-tipler)
- [Hata Modeli](#hata-modeli)

---

## Health

### `GET /health`

Liveness probe.

**Response 200**
```json
{ "status": "ok" }
```

---

## Split Payment

### 1.1 `POST /split-payment/initiate`

Initiator checkout'ta "Ortak Öde"'yi seçip katılımcıları belirledikten sonra çağırır. Server `splitId` döner.

**Validation**
- Tam olarak 1 katılımcı `isInitiator=true` olmalı
- `participants[].shareKurus` toplamı `totalKurus`'a eşit olmalı (hem EQUAL hem CUSTOM modunda doğrulanır)
- `participantId` listesinde duplicate olmamalı

**Request body** — `InitiateSplitRequestDto`
```json
{
  "totalKurus": 12000,
  "splitMethod": "EQUAL",
  "participants": [
    { "participantId": "u_self", "shareKurus": 4000, "isInitiator": true },
    { "participantId": "u_42",   "shareKurus": 4000, "isInitiator": false },
    { "participantId": "u_99",   "shareKurus": 4000, "isInitiator": false }
  ]
}
```

**Response 201**
```json
{ "splitId": "sp_8f3c7ab2" }
```

**Errors:** `400 VALIDATION`

---

### 1.2 `POST /split-payment/initiator-pay/{splitId}`

Initiator checkout'u tamamlayıp gerçek `orderId` aldıktan sonra çağırır. Server payı kilitler (PAID), katılımcılara push/sms gönderir ve tracking deeplink'i döner. `orders.split_id` da denormalize olarak set edilir (sipariş detay chip'i için).

**Path params**
| name | type | example |
|---|---|---|
| `splitId` | string | `sp_8f3c7ab2` |

**Request body** — `InitiatorPayRequestDto`
```json
{ "orderId": "ORD-7700219" }
```

**Response 200**
```json
{ "trackingDeeplink": "ty://?Page=OrtakOdemeTakip&SplitId=sp_8f3c7ab2" }
```

**Errors:** `400 VALIDATION`, `404 NOT_FOUND`

---

### 1.3 `GET /split-payment/request/{splitId}/{participantId}`

Katılımcı request ekranı için detay.

**Path params**
| name | type | example |
|---|---|---|
| `splitId` | string | `sp_8f3c7ab2` |
| `participantId` | string | `u_42` |

**Response 200** — `SplitPaymentRequestResponseDto`
```json
{
  "splitId": "sp_8f3c7ab2",
  "participantId": "u_42",
  "initiatorName": "Berna T.",
  "shareKurus": 4000,
  "totalKurus": 12000,
  "splitMethod": "EQUAL",
  "status": "PENDING",
  "products": [
    { "productId": "p_1", "name": "Saat", "imageUrl": null, "priceKurus": 12000 }
  ]
}
```

**Errors:** `404 NOT_FOUND`

---

### 1.4 `POST /split-payment/reject/{splitId}`

Katılımcı payment-request ekranından veya tracking ekranından kendi payını reddeder.

> PAID statüsündeki katılımcı reject yapamaz (400).

**Path params:** `splitId`
**Headers:** `X-User-Id: u_42`

**Response 204** — No content
**Errors:** `400 VALIDATION`, `404 NOT_FOUND`

---

### 1.5 `GET /split-payment/{splitId}`

Tracking — Bekleme Odası (20s polling).
`role` viewer'a göre `INITIATOR` / `PARTICIPANT` döner.
`expiresAtMillis` mutlak epoch ms (process death dirençli).

**Path params:** `splitId`
**Headers:** `X-User-Id: u_self`

**Response 200** — `SplitPaymentTrackingDto`
```json
{
  "splitId": "sp_8f3c7ab2",
  "role": "INITIATOR",
  "overallStatus": "WAITING",
  "expiresAtMillis": 1731000000000,
  "totalKurus": 12000,
  "yourShareKurus": 4000,
  "initiator": { "name": "Berna T.", "initials": "BT" },
  "participants": [
    { "id": "u_42", "name": "Ali Y.", "initials": "AY", "status": "PENDING", "amountKurus": 4000 },
    { "id": "u_99", "name": "Mert K.", "initials": "MK", "status": "PAID",    "amountKurus": 4000 }
  ],
  "products": [
    { "productId": "p_1", "name": "Saat", "imageUrl": null, "priceKurus": 12000 }
  ]
}
```

**Errors:** `404 NOT_FOUND`

---

### 1.6 `POST /split-payment/{splitId}/remind/{participantId}`

Initiator katılımcıya hatırlatma push'ı yollar.

> Sadece `PENDING` statüdeki katılımcılar hatırlatılabilir.

**Path params:** `splitId`, `participantId`

**Response 204** — No content
**Errors:** `400 VALIDATION`, `404 NOT_FOUND`

---

### 1.7 `POST /split-payment/{splitId}/cancel`

Initiator split'i iptal eder. Polling iptal edilmeli, ekran kapatılmalı.

> Zaten `CANCELLED` / `COMPLETED` / `EXPIRED` olan split tekrar iptal edilemez (400).

**Path params:** `splitId`

**Response 204** — No content
**Errors:** `400 VALIDATION`, `404 NOT_FOUND`

---

## Friends

### 2.1 `GET /friends`

Arkadaş listesi.

**Headers:** `X-User-Id: u_self`

**Response 200** — `FriendsListResponseDto`
```json
{
  "friends": [
    {
      "id": "u_42",
      "name": "Ali Yılmaz",
      "email": "ali@example.com",
      "initials": "AY",
      "avatarUrl": null
    }
  ]
}
```

---

### 2.2 `POST /friends/add`

E-posta ile arkadaş ekle.

> Hata durumunda response body **`FriendsErrorResponseDto`** formatındadır (genel `ErrorResponse` değil). Client `errorCode` set'ini sealed `FriendsError` tipine maple eder.

**Headers:** `X-User-Id: u_self`

**Request body** — `AddFriendRequestDto`
```json
{ "email": "yeni@example.com" }
```

**Response 200** — `AddFriendResponseDto`
```json
{
  "friend": {
    "id": "u_123",
    "name": "Yeni Kullanıcı",
    "email": "yeni@example.com",
    "initials": "YK",
    "avatarUrl": null
  }
}
```

**Hata kodları**

| `errorCode`      | HTTP | Anlamı                              |
|------------------|------|-------------------------------------|
| `INVALID_EMAIL`  | 400  | Geçersiz e-posta formatı            |
| `SELF_ADD`       | 400  | Kendini eklemeye çalıştı            |
| `USER_NOT_FOUND` | 404  | E-postaya karşılık kullanıcı yok    |
| `ALREADY_FRIEND` | 409  | Zaten arkadaş listesinde            |

**Hata response (örnek)**
```json
{ "errorCode": "USER_NOT_FOUND", "message": "Kullanıcı bulunamadı." }
```

---

### 2.3 `DELETE /friends/{friendId}`

Arkadaşı sil (bidirectional — her iki yönlü edge silinir).

**Path params:** `friendId` (örn. `u_42`)
**Headers:** `X-User-Id: u_self`

**Response 204** — No content
**Errors:** `404 NOT_FOUND`

---

## Orders

### 3 `GET /orders/{id}`

Sipariş detayı. **Yeni alan: `paymentInfo.splitId`** (opsiyonel, nullable, default null). Split kapsamında oluşturulmuş siparişlerde dolu döner; client `ty://?Page=OrtakOdemeTakip&SplitId=<id>&OrderId=<orderId>` deeplink'i ile chip render eder. Geriye dönük uyumlu.

**Path params:** `id` (örn. `ORD-7700219`)

**Response 200** — `OrderDetailDto`
```json
{
  "id": "ORD-7700219",
  "paymentInfo": {
    "cardImageUrl": null,
    "paymentDescription": null,
    "totalPrice": "120,00 TL",
    "paymentItems": [],
    "paymentType": "CREDIT_CARD",
    "cobrandedRewardInfo": null,
    "isCargoBundleUsed": false,
    "umicoInfoItems": [],
    "splitId": "sp_8f3c7ab2"
  }
}
```

**Errors:** `404 NOT_FOUND`

---

## Items

Mevcut item CRUD (orijinal scope).

| Method | Path | Açıklama |
|---|---|---|
| `GET` | `/items` | Liste |
| `POST` | `/items` | Oluştur |
| `GET` | `/items/{id}` | Tekil |
| `PUT` | `/items/{id}` | Güncelle |
| `DELETE` | `/items/{id}` | Sil |

**Item şeması**
```json
{ "id": "string?", "name": "string", "description": "string?" }
```

---

## Ortak Tipler

### Enums

| Enum | Değerler |
|---|---|
| `SplitMethod` | `EQUAL`, `CUSTOM` |
| `SplitParticipantStatus` | `PENDING`, `PAID`, `REJECTED`, `EXPIRED` |
| `SplitOverallStatus` | `WAITING`, `COMPLETED`, `EXPIRED`, `CANCELLED` |
| `SplitRole` | `INITIATOR`, `PARTICIPANT` |

### Header

| Header | Required | Açıklama |
|---|---|---|
| `X-User-Id` | viewer/caller olan endpointlerde | Hackathon shim — production'da JWT'den çıkarılır |

---

## Hata Modeli

### Genel `ErrorResponse`

Friends dışındaki tüm endpoint'ler bu format ile döner:

```json
{ "error": "split sp_x not found", "code": "NOT_FOUND" }
```

| `code` | Anlamı |
|---|---|
| `VALIDATION` | İstek doğrulama hatası (400) |
| `NOT_FOUND` | Kayıt bulunamadı (404) |
| `INTERNAL` | Sunucu hatası (500) |

### `FriendsErrorResponseDto`

Sadece `/friends/*` endpoint'lerinde — bkz. [Friends 2.2 hata kodları tablosu](#22-post-friendsadd).

```json
{ "errorCode": "ALREADY_FRIEND", "message": "Zaten arkadaş listenizde." }
```
