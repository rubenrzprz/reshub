-- Hotels
create table if not exists hotel
(
  id
  uuid
  primary
  key,
  code
  varchar
(
  24
) not null unique,
  name varchar
(
  120
) not null,
  active boolean not null default true,
  created_at timestamptz not null default now
(
),
  updated_at timestamptz not null default now
(
)
  );

-- Agencies
create table if not exists agency
(
  id
  uuid
  primary
  key,
  code
  varchar
(
  24
) not null unique,
  name varchar
(
  120
) not null,
  active boolean not null default true,
  created_at timestamptz not null default now
(
),
  updated_at timestamptz not null default now
(
)
  );

-- Users (staff or agency users; exactly one affiliation)
create table if not exists app_user
(
  id
  uuid
  primary
  key,
  email
  varchar
(
  254
) not null unique,
  password_hash varchar
(
  72
) not null,
  role varchar
(
  24
) not null, -- MANAGER | RECEPTIONIST | AGENCY
  hotel_id uuid null references hotel
(
  id
) on delete restrict,
  agency_id uuid null references agency
(
  id
)
  on delete restrict,
  created_at timestamptz not null default now
(
),
  updated_at timestamptz not null default now
(
),
  constraint app_user_affiliation_xor check
(
(
  hotel_id
  is
  null
) <>
(
  agency_id
  is
  null
))
  );

create index if not exists idx_app_user_hotel on app_user(hotel_id);
create index if not exists idx_app_user_agency on app_user(agency_id);
