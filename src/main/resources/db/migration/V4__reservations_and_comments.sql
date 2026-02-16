-- Reservations
create table if not exists reservation
(
  id uuid primary key,
  hotel_id uuid not null references hotel(id) on delete restrict,
  agency_id uuid not null references agency(id) on delete restrict,
  created_by_user_id uuid not null references app_user(id) on delete restrict,
  room_type_id uuid null,
  external_room_type_code varchar(64) null,
  external_room_type_name varchar(160) null,
  external_ref varchar(64) not null,
  status varchar(16) not null,
  arrival_date date not null,
  departure_date date not null,
  guest_name varchar(160) not null,
  guest_email varchar(254) null,
  guest_phone varchar(32) null,
  adults smallint not null default 1,
  children smallint not null default 0,
  notes text null,
  cancelled_at timestamptz null,
  cancel_reason varchar(160) null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint reservation_status_check check (status in ('NEW', 'CONFIRMED', 'CANCELLED', 'NOSHOW')),
  constraint reservation_date_check check (arrival_date < departure_date),
  constraint reservation_party_check check (adults >= 1 and children >= 0),
  constraint reservation_external_ref_unique unique (hotel_id, agency_id, external_ref),
  constraint reservation_room_type_hotel_fk
    foreign key (hotel_id, room_type_id) references room_type(hotel_id, id) on delete restrict
  );

create index if not exists idx_reservation_hotel_arrival on reservation(hotel_id, arrival_date);
create index if not exists idx_reservation_hotel_status on reservation(hotel_id, status);

-- Reservation comments (staff-internal)
create table if not exists reservation_comment
(
  id uuid primary key,
  reservation_id uuid not null references reservation(id) on delete cascade,
  author_user_id uuid not null references app_user(id) on delete restrict,
  body text not null,
  created_at timestamptz not null default now()
  );

-- Reservation lifecycle transitions
create or replace function reservation_status_transition_guard()
returns trigger as $$
begin
  if (tg_op = 'UPDATE') then
    if (old.status = new.status) then
      return new;
    end if;
    if (old.status = 'NEW' and new.status in ('CONFIRMED', 'CANCELLED', 'NOSHOW')) then
      return new;
    elsif (old.status = 'CONFIRMED' and new.status in ('CANCELLED', 'NOSHOW')) then
      return new;
    else
      raise exception 'invalid reservation status transition: % -> %', old.status, new.status;
    end if;
  end if;
  return new;
end;
$$ language plpgsql;

drop trigger if exists reservation_status_transition_check on reservation;
create trigger reservation_status_transition_check
  before update on reservation
  for each row
  execute function reservation_status_transition_guard();
