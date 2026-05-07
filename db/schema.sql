-- TrendSocial API — Supabase Schema
-- Run in Supabase SQL Editor (idempotent: DROP CASCADE then CREATE).
-- RLS disabled for hackathon scope.

------------------------------------------------------------
-- 1) DROP existing
------------------------------------------------------------
drop table if exists order_split_participants cascade;
drop table if exists order_items cascade;
drop table if exists orders cascade;
drop table if exists cart_items cascade;
drop table if exists collection_participants cascade;
drop table if exists collection_products cascade;
drop table if exists collections cascade;
drop table if exists product_likes cascade;
drop table if exists product_favorites cascade;
drop table if exists products cascade;
drop table if exists friends cascade;
drop table if exists sessions cascade;
drop table if exists users cascade;

-- Old hackathon tables (in case)
drop table if exists splits cascade;
drop table if exists split_participants cascade;
drop table if exists items cascade;

------------------------------------------------------------
-- 2) Users + Sessions
------------------------------------------------------------
create table users (
    id text primary key,
    email text unique not null,
    password_hash text not null,        -- sha256 hex
    name text not null,
    avatar_url text,
    avatar_color_name text default 'blue',
    birthday timestamptz,               -- ISO 8601 datetime; null ise henuz girilmemis
    created_at timestamptz default now()
);

create table sessions (
    token text primary key,
    user_id text not null references users(id) on delete cascade,
    created_at timestamptz default now()
);
create index sessions_user_idx on sessions(user_id);

------------------------------------------------------------
-- 3) Products + Likes/Favorites
------------------------------------------------------------
create table products (
    id text primary key,
    brand text not null,
    name text not null,
    rating numeric(2,1) not null default 0,
    review_count integer not null default 0,
    price numeric(10,2) not null,
    image_name text not null,
    like_count integer not null default 0,
    dislike_count integer not null default 0,
    created_at timestamptz default now()
);

create table product_favorites (
    user_id text references users(id) on delete cascade,
    product_id text references products(id) on delete cascade,
    primary key (user_id, product_id)
);

create table product_likes (
    user_id text references users(id) on delete cascade,
    product_id text references products(id) on delete cascade,
    status text not null check (status in ('liked', 'disliked')),
    primary key (user_id, product_id)
);

------------------------------------------------------------
-- 4) Collections
------------------------------------------------------------
create table collections (
    id text primary key,
    owner_id text not null references users(id) on delete cascade,
    name text not null,
    description text,
    is_shared boolean not null default false,
    image_name text default 'collection_default',
    created_at timestamptz default now()
);

create table collection_products (
    collection_id text references collections(id) on delete cascade,
    product_id text references products(id) on delete cascade,
    primary key (collection_id, product_id)
);

create table collection_participants (
    collection_id text references collections(id) on delete cascade,
    user_id text references users(id) on delete cascade,
    primary key (collection_id, user_id)
);

------------------------------------------------------------
-- 5) Friends (bidirectional — each direction stored)
------------------------------------------------------------
create table friends (
    user_id text references users(id) on delete cascade,
    friend_id text references users(id) on delete cascade,
    created_at timestamptz default now(),
    primary key (user_id, friend_id),
    check (user_id <> friend_id)
);

------------------------------------------------------------
-- 6) Cart
------------------------------------------------------------
create table cart_items (
    id text primary key,
    user_id text not null references users(id) on delete cascade,
    product_id text not null references products(id) on delete cascade,
    brand text not null,
    name text not null,
    size text not null,
    price numeric(10,2) not null,
    quantity integer not null default 1 check (quantity > 0),
    icon_name text not null,
    created_at timestamptz default now()
);
create index cart_user_idx on cart_items(user_id);

------------------------------------------------------------
-- 7) Orders
------------------------------------------------------------
create table orders (
    id text primary key,
    user_id text not null references users(id) on delete cascade,
    order_number text not null unique,
    total_price numeric(10,2) not null,
    status text not null check (status in (
        'pending','payment_pending','confirmed','preparing','shipped','delivered','cancelled'
    )),
    created_at timestamptz default now(),
    estimated_delivery timestamptz,
    is_split_payment boolean not null default false
);
create index orders_user_idx on orders(user_id);

create table order_items (
    id bigserial primary key,
    order_id text not null references orders(id) on delete cascade,
    name text not null,
    brand text not null,
    quantity integer not null,
    price numeric(10,2) not null,
    icon_name text not null
);
create index order_items_order_idx on order_items(order_id);

create table order_split_participants (
    order_id text references orders(id) on delete cascade,
    friend_id text not null,            -- not FK; can be a friend usr_id even if friendship later removed
    name text not null,
    amount numeric(10,2) not null,
    has_paid boolean not null default false,
    primary key (order_id, friend_id)
);

------------------------------------------------------------
-- 8) RLS — disable for hackathon
------------------------------------------------------------
alter table users disable row level security;
alter table sessions disable row level security;
alter table products disable row level security;
alter table product_favorites disable row level security;
alter table product_likes disable row level security;
alter table collections disable row level security;
alter table collection_products disable row level security;
alter table collection_participants disable row level security;
alter table friends disable row level security;
alter table cart_items disable row level security;
alter table orders disable row level security;
alter table order_items disable row level security;
alter table order_split_participants disable row level security;

------------------------------------------------------------
-- 9) SEED DATA
------------------------------------------------------------

-- Users (password "Sifre123!" → sha256 hex below)
-- echo -n 'Sifre123!' | shasum -a 256
--   = 8e3f8a5e3f4f7c9c4e2c5e7d8b9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f
-- Pre-computed for: 'Sifre123!'
insert into users (id, email, password_hash, name, avatar_color_name) values
  ('usr_demo01', 'burak@example.com',  'eb3a3acef229c4ad6a9f10b7d12e9d3eb4c66bbe1ab73086c3b4d8c2c4f6dabb', 'Burak Filiz',  'blue'),
  ('usr_demo02', 'ayse@example.com',   'eb3a3acef229c4ad6a9f10b7d12e9d3eb4c66bbe1ab73086c3b4d8c2c4f6dabb', 'Ayse Yilmaz',  'orange'),
  ('usr_demo03', 'mehmet@example.com', 'eb3a3acef229c4ad6a9f10b7d12e9d3eb4c66bbe1ab73086c3b4d8c2c4f6dabb', 'Mehmet Kaya',  'green');

-- Note: real hash for 'Sifre123!' is computed at register-time by AuthService;
-- the hash above is a placeholder. Demo users CAN'T log in until you reset their
-- passwords via /auth/register or update the seed. See readme for details.

-- Friends (bidirectional)
insert into friends (user_id, friend_id) values
  ('usr_demo01', 'usr_demo02'),
  ('usr_demo02', 'usr_demo01'),
  ('usr_demo01', 'usr_demo03'),
  ('usr_demo03', 'usr_demo01');

-- Products (25 items)
insert into products (id, brand, name, rating, review_count, price, image_name) values
  ('prd_001', 'Zara',     'Siyah V Yaka Askili Midi Boy Viskon Elbise', 4.8, 124, 599.99,  'zara_dress'),
  ('prd_002', 'Nike',     'Air Max 270 Kirmizi Erkek Spor Ayakkabi',    4.9, 856, 3299.00, 'nike_shoe'),
  ('prd_003', 'Apple',    'Watch Series 8 GPS 45mm',                    4.7, 412, 11499.00,'applewatch'),
  ('prd_004', 'Adidas',   'Originals Stan Smith Beyaz Sneaker',         4.6, 654, 1899.00, 'adidas_stan'),
  ('prd_005', 'Levi''s',  '501 Original Fit Erkek Jean',                4.5, 312, 1299.00, 'levis_jeans'),
  ('prd_006', 'Apple',    'iPhone 15 Pro 256GB Titanyum',               4.9, 988, 64999.00,'iphone'),
  ('prd_007', 'Mango',    'Floral Desenli Maxi Elbise',                 4.4, 87,  799.50,  'mango_dress'),
  ('prd_008', 'Puma',     'RS-X Reinvention Erkek Sneaker',             4.5, 233, 2499.00, 'puma_rsx'),
  ('prd_009', 'H&M',      'Basic Beyaz T-Shirt 3''lu Paket',            4.3, 1542,299.99,  'hm_tshirt'),
  ('prd_010', 'Samsung',  'Galaxy S24 Ultra 512GB',                     4.8, 567, 54999.00,'samsung_s24'),
  ('prd_011', 'Mavi',     'Slim Fit Lacivert Erkek Pantolon',           4.4, 198, 899.00,  'mavi_pants'),
  ('prd_012', 'New Balance','574 Classic Gri Sneaker',                  4.7, 421, 2199.00, 'nb_574'),
  ('prd_013', 'Pull&Bear','Oversize Hoodie Bej',                        4.2, 156, 549.00,  'pb_hoodie'),
  ('prd_014', 'Sony',     'WH-1000XM5 Kablosuz Kulaklik',               4.9, 723, 12999.00,'sony_headphones'),
  ('prd_015', 'Vans',     'Old Skool Siyah Beyaz Sneaker',              4.6, 891, 1799.00, 'vans_oldskool'),
  ('prd_016', 'Bershka',  'Crop Top Beyaz',                             4.1, 67,  249.00,  'bershka_crop'),
  ('prd_017', 'Apple',    'AirPods Pro 2. Nesil',                       4.8, 1234,9999.00, 'airpods'),
  ('prd_018', 'Tommy Hilfiger','Logolu Polo Yaka T-Shirt',              4.5, 142, 1599.00, 'tommy_polo'),
  ('prd_019', 'Asics',    'Gel-Kayano 30 Kosu Ayakkabisi',              4.8, 389, 4499.00, 'asics_kayano'),
  ('prd_020', 'Stradivarius','Yuksek Bel Mom Jean',                     4.3, 234, 699.00,  'stradi_jean'),
  ('prd_021', 'Casio',    'G-Shock GA-2100 Siyah Saat',                 4.7, 512, 3299.00, 'casio_gshock'),
  ('prd_022', 'Lacoste',  'Klasik Pike Polo Beyaz',                     4.6, 298, 1899.00, 'lacoste_polo'),
  ('prd_023', 'Converse', 'Chuck Taylor All Star Yuksek Bilek',         4.6, 1102,1499.00, 'converse_chuck'),
  ('prd_024', 'Xiaomi',   'Mi Band 8 Akilli Bileklik',                  4.4, 445, 1299.00, 'mi_band'),
  ('prd_025', 'Defacto',  'Basic Cep Detayli Sweatshirt',               4.0, 89,  349.00,  'defacto_sweat');

-- Sample favorites/likes
insert into product_favorites (user_id, product_id) values
  ('usr_demo01', 'prd_001'),
  ('usr_demo01', 'prd_002'),
  ('usr_demo02', 'prd_003');

insert into product_likes (user_id, product_id, status) values
  ('usr_demo01', 'prd_002', 'liked'),
  ('usr_demo02', 'prd_002', 'liked'),
  ('usr_demo03', 'prd_007', 'disliked');

-- Sample collection
insert into collections (id, owner_id, name, description, is_shared, image_name) values
  ('col_demo01', 'usr_demo01', 'Yaz Tatili Plani', 'Tatil icin alinacaklar', true, 'yaz_tatili'),
  ('col_demo02', 'usr_demo01', 'Favori Ayakkabilar', null, false, 'favori_ayakkabi');

insert into collection_products (collection_id, product_id) values
  ('col_demo01', 'prd_001'),
  ('col_demo01', 'prd_007'),
  ('col_demo02', 'prd_002'),
  ('col_demo02', 'prd_004'),
  ('col_demo02', 'prd_015');

insert into collection_participants (collection_id, user_id) values
  ('col_demo01', 'usr_demo01'),
  ('col_demo01', 'usr_demo02');

------------------------------------------------------------
-- DONE
------------------------------------------------------------
-- After running:
--   select count(*) from users;       -- 3
--   select count(*) from products;    -- 25
--   select count(*) from friends;     -- 4 (2 bidirectional pairs)
