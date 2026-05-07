-- Migration: notifications table (TrendSocial #notifications)
-- Run once in Supabase SQL Editor.

create table if not exists notifications (
    id text primary key,
    user_id text not null references users(id) on delete cascade,
    type text not null check (type in (
        'order_confirmed','order_shipped','order_delivered',
        'friend_request',
        'split_payment_received','split_payment_reminder',
        'collection_shared'
    )),
    title text not null,
    body text not null,
    is_read boolean not null default false,
    created_at timestamptz not null default now()
);

create index if not exists notifications_user_idx on notifications(user_id, created_at desc);

alter table notifications disable row level security;
