-- Room types (hotel-scoped)
create table if not exists room_type
(
  id uuid primary key,
  hotel_id uuid not null references hotel(id) on delete restrict,
  code varchar(32) not null,
  name varchar(120) not null,
  class varchar(24) null, -- STANDARD | SUPERIOR | DELUXE | SUITE | JR_SUITE
  base_occupancy smallint not null,
  max_occupancy smallint not null,
  attributes_raw jsonb not null,
  attributes_canonical jsonb not null,
  flexible_bedding boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint room_type_code_per_hotel unique (hotel_id, code)
  );

create index if not exists idx_room_type_attributes_canonical on room_type using gin (attributes_canonical);

-- Room type channel mapping
create table if not exists room_type_channel_map
(
  id uuid primary key,
  hotel_id uuid not null,
  room_type_id uuid not null references room_type(id) on delete restrict,
  channel varchar(32) not null,
  external_code varchar(64) not null,
  external_name varchar(160) null,
  meta_json jsonb null,
  created_at timestamptz not null default now(),
  constraint room_type_channel_map_unique unique (hotel_id, channel, external_code)
  );
