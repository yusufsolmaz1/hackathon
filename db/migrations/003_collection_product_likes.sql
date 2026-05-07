-- Migration: per-collection product likes
-- Run once in Supabase SQL Editor (idempotent).

create table if not exists collection_product_likes (
    collection_id text not null references collections(id) on delete cascade,
    product_id    text not null references products(id) on delete cascade,
    user_id       text not null references users(id) on delete cascade,
    status        text not null check (status in ('liked', 'disliked')),
    created_at    timestamptz not null default now(),
    primary key (collection_id, product_id, user_id)
);

create index if not exists collection_product_likes_idx
    on collection_product_likes(collection_id, product_id);

alter table collection_product_likes disable row level security;
