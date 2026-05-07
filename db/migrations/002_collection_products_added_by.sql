-- Migration: collection_products → added_by + added_at
-- Run once in Supabase SQL Editor (idempotent).

alter table collection_products
    add column if not exists added_by text references users(id) on delete set null;

alter table collection_products
    add column if not exists added_at timestamptz default now();

-- Backfill: existing rows attributed to collection owner
update collection_products cp
   set added_by = c.owner_id
  from collections c
 where cp.collection_id = c.id
   and cp.added_by is null;
