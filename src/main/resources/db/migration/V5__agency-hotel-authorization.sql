-- Agency-hotel coarse authorization (feature-flagged enforcement)
create table if not exists agency_hotel_auth
(
  id uuid primary key,
  agency_id uuid not null references agency(id) on delete restrict,
  hotel_id uuid not null references hotel(id) on delete restrict,
  status varchar(12) not null,
  valid_from date null,
  valid_to date null,
  terms_json jsonb null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint agency_hotel_auth_status_check check (status in ('ACTIVE', 'SUSPENDED', 'ENDED')),
  constraint agency_hotel_auth_validity_check check (valid_from is null or valid_to is null or valid_from <= valid_to),
  constraint agency_hotel_auth_agency_hotel_unique unique (agency_id, hotel_id)
  );

create index if not exists idx_agency_hotel_auth_agency on agency_hotel_auth(agency_id);
create index if not exists idx_agency_hotel_auth_hotel on agency_hotel_auth(hotel_id);

-- Optional granular room-type allowlist per authorization
create table if not exists agency_hotel_room_type_allow
(
  auth_id uuid not null references agency_hotel_auth(id) on delete cascade,
  room_type_id uuid not null references room_type(id) on delete restrict,
  primary key (auth_id, room_type_id)
  );
