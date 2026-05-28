-- Development-only demo data. Loaded only when the dev profile includes classpath:db/dev.
insert into hotel (id, code, name, active)
values ('10000000-0000-0000-0000-000000000001', 'DEMO-HOTEL', 'ResHub Demo Hotel', true)
on conflict (code) do update
set name = excluded.name,
    active = excluded.active,
    updated_at = now();

insert into agency (id, code, name, active)
values ('20000000-0000-0000-0000-000000000001', 'DEMO-AGENCY', 'ResHub Demo Agency', true)
on conflict (code) do update
set name = excluded.name,
    active = excluded.active,
    updated_at = now();

insert into room_type (
  id,
  hotel_id,
  code,
  name,
  class,
  base_occupancy,
  max_occupancy,
  attributes_raw,
  attributes_canonical,
  flexible_bedding
)
values (
  '30000000-0000-0000-0000-000000000001',
  '10000000-0000-0000-0000-000000000001',
  'DLX',
  'Deluxe Double',
  'DELUXE',
  2,
  3,
  '{"beds":"1 queen or 2 twin","view":"city"}'::jsonb,
  '{"beds":["queen","twin"],"view":"city"}'::jsonb,
  true
)
on conflict (hotel_id, code) do update
set name = excluded.name,
    class = excluded.class,
    base_occupancy = excluded.base_occupancy,
    max_occupancy = excluded.max_occupancy,
    attributes_raw = excluded.attributes_raw,
    attributes_canonical = excluded.attributes_canonical,
    flexible_bedding = excluded.flexible_bedding,
    updated_at = now();

insert into app_user (id, email, password_hash, role, hotel_id, agency_id)
values
  (
    '40000000-0000-0000-0000-000000000001',
    'admin@reshub.local',
    '$2a$10$U6O3LOtTgPbTRkWWZ70Kt.nn9sZ5QPmJcX.dVgzv1m8TwYz5aZ6ci',
    'ADMIN',
    '10000000-0000-0000-0000-000000000001',
    null
  ),
  (
    '40000000-0000-0000-0000-000000000002',
    'manager@reshub.local',
    '$2a$10$U6O3LOtTgPbTRkWWZ70Kt.nn9sZ5QPmJcX.dVgzv1m8TwYz5aZ6ci',
    'MANAGER',
    '10000000-0000-0000-0000-000000000001',
    null
  ),
  (
    '40000000-0000-0000-0000-000000000003',
    'reception@reshub.local',
    '$2a$10$U6O3LOtTgPbTRkWWZ70Kt.nn9sZ5QPmJcX.dVgzv1m8TwYz5aZ6ci',
    'RECEPTIONIST',
    '10000000-0000-0000-0000-000000000001',
    null
  ),
  (
    '40000000-0000-0000-0000-000000000004',
    'agency@reshub.local',
    '$2a$10$U6O3LOtTgPbTRkWWZ70Kt.nn9sZ5QPmJcX.dVgzv1m8TwYz5aZ6ci',
    'AGENCY',
    null,
    '20000000-0000-0000-0000-000000000001'
  )
on conflict (email) do update
set password_hash = excluded.password_hash,
    role = excluded.role,
    hotel_id = excluded.hotel_id,
    agency_id = excluded.agency_id,
    updated_at = now();

insert into agency_hotel_auth (id, agency_id, hotel_id, status, valid_from, valid_to, terms_json)
values (
  '50000000-0000-0000-0000-000000000001',
  '20000000-0000-0000-0000-000000000001',
  '10000000-0000-0000-0000-000000000001',
  'ACTIVE',
  current_date - 30,
  null,
  '{"source":"dev-seed"}'::jsonb
)
on conflict (agency_id, hotel_id) do update
set status = excluded.status,
    valid_from = excluded.valid_from,
    valid_to = excluded.valid_to,
    terms_json = excluded.terms_json,
    updated_at = now();

insert into reservation (
  id,
  hotel_id,
  agency_id,
  created_by_user_id,
  room_type_id,
  external_room_type_code,
  external_room_type_name,
  external_ref,
  status,
  arrival_date,
  departure_date,
  guest_name,
  guest_email,
  guest_phone,
  adults,
  children,
  notes
)
values (
  '60000000-0000-0000-0000-000000000001',
  '10000000-0000-0000-0000-000000000001',
  '20000000-0000-0000-0000-000000000001',
  '40000000-0000-0000-0000-000000000004',
  '30000000-0000-0000-0000-000000000001',
  'DLX',
  'Deluxe Double',
  'DEMO-RES-001',
  'NEW',
  current_date + 14,
  current_date + 17,
  'Alex Demo',
  'alex.demo@example.com',
  '+34928000000',
  2,
  0,
  'Seeded reservation for local API demos.'
)
on conflict (hotel_id, agency_id, external_ref) do nothing;
