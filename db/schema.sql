-- =====================================================================
-- Hackathon BFF schema
-- Supabase / Postgres 14+
-- Run order: extensions → users → friends → orders → splits → split_*
-- =====================================================================

create extension if not exists "pgcrypto";

-- ---------------------------------------------------------------------
-- 0) users
--    Hackathon basitliği: hem split katılımcısı hem friend referansı
--    burada tutulur. Production'da auth.users ile birleştirilir.
-- ---------------------------------------------------------------------
create table if not exists users (
    id          text primary key,                   -- "u_self", "u_42" gibi
    name        text not null,
    email       text unique,
    initials    text not null check (length(initials) <= 4),
    avatar_url  text,
    created_at  timestamptz not null default now()
);

-- ---------------------------------------------------------------------
-- 1) friends   (bidirectional pair: her yön ayrı satır)
-- ---------------------------------------------------------------------
create table if not exists friends (
    user_id     text not null references users(id) on delete cascade,
    friend_id   text not null references users(id) on delete cascade,
    created_at  timestamptz not null default now(),
    primary key (user_id, friend_id),
    check (user_id <> friend_id)
);

create index if not exists friends_user_idx on friends(user_id);

-- ---------------------------------------------------------------------
-- 2) orders   (mevcut order detail; splitId opsiyonel chip için)
--    payment_info JSONB içinde gönderilen sözleşme alanları:
--      cardImageUrl, paymentDescription, totalPrice, paymentItems,
--      paymentType, cobrandedRewardInfo, isCargoBundleUsed,
--      umicoInfoItems, splitId (opsiyonel, default null)
-- ---------------------------------------------------------------------
create table if not exists orders (
    id              text primary key,               -- "ORD-7700219"
    user_id         text not null references users(id) on delete cascade,
    payment_info    jsonb not null,
    split_id        text,                           -- denormalize: hızlı lookup
    created_at      timestamptz not null default now()
);

create index if not exists orders_user_idx     on orders(user_id);
create index if not exists orders_split_idx    on orders(split_id) where split_id is not null;

-- ---------------------------------------------------------------------
-- 3) splits   (ortak ödeme intent'i)
--    status:        WAITING | COMPLETED | EXPIRED | CANCELLED
--    split_method:  EQUAL   | CUSTOM
-- ---------------------------------------------------------------------
create table if not exists splits (
    id                  text primary key,           -- "sp_8f3c7ab2"
    initiator_id        text not null references users(id),
    initiator_order_id  text references orders(id), -- pay sonrası set
    total_kurus         bigint not null check (total_kurus > 0),
    split_method        text   not null check (split_method in ('EQUAL','CUSTOM')),
    status              text   not null default 'WAITING'
                        check (status in ('WAITING','COMPLETED','EXPIRED','CANCELLED')),
    products            jsonb  not null default '[]'::jsonb,
                        -- [{productId, name, imageUrl, priceKurus}]
    expires_at_millis   bigint not null,            -- absolute epoch ms
    created_at          timestamptz not null default now()
);

create index if not exists splits_initiator_idx on splits(initiator_id);
create index if not exists splits_status_idx    on splits(status);

-- ---------------------------------------------------------------------
-- 4) split_participants
--    status: PENDING | PAID | REJECTED | EXPIRED
--    is_initiator true olan satır initiator'ın kendi payı
-- ---------------------------------------------------------------------
create table if not exists split_participants (
    split_id        text not null references splits(id) on delete cascade,
    participant_id  text not null references users(id),
    share_kurus     bigint not null check (share_kurus > 0),
    is_initiator    boolean not null default false,
    status          text not null default 'PENDING'
                    check (status in ('PENDING','PAID','REJECTED','EXPIRED')),
    paid_order_id   text references orders(id),
    updated_at      timestamptz not null default now(),
    primary key (split_id, participant_id)
);

create index if not exists split_participants_part_idx on split_participants(participant_id);

-- =====================================================================
-- Seed data (opsiyonel - dev için)
-- =====================================================================

insert into users (id, name, email, initials, avatar_url) values
    ('u_self',  'Berna Tek',    'berna@example.com',   'BT', null),
    ('u_42',    'Ali Yılmaz',   'ali@example.com',     'AY', null),
    ('u_99',    'Zeynep Kaya',  'zeynep@example.com',  'ZK', null)
on conflict (id) do nothing;

insert into friends (user_id, friend_id) values
    ('u_self','u_42'),
    ('u_self','u_99')
on conflict do nothing;
