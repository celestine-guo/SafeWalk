-- SafeWalk schema bootstrap for Supabase PostgreSQL
-- 仅在 Supabase 项目数据库中执行，不要在本地 SQLite 执行。

create table if not exists public.trips (
  id bigserial primary key,
  user_id text not null,
  mode text not null check (mode in ('walking', 'taxi')),
  status text not null default 'active' check (status in ('active', 'arrived', 'sos', 'cancelled', 'timeout')),
  start_latitude double precision not null,
  start_longitude double precision not null,
  destination_latitude double precision not null,
  destination_longitude double precision not null,
  planned_arrival_at timestamptz not null,
  deviation_threshold_meters double precision not null,
  timeout_grace_minutes integer not null,
  planned_path_json jsonb not null,
  started_at timestamptz not null default now(),
  arrived_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.locations (
  id bigserial primary key,
  trip_id bigint not null references public.trips(id) on delete cascade,
  latitude double precision not null,
  longitude double precision not null,
  accuracy double precision,
  recorded_at timestamptz not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.emergencies (
  id bigserial primary key,
  user_id text not null,
  type text not null check (type in ('sos', 'manual_alert')),
  status text not null check (status in ('active', 'resolved')),
  latitude double precision not null,
  longitude double precision not null,
  "timestamp" timestamptz not null default now(),
  resolved_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.chat_messages (
  id bigserial primary key,
  trip_id bigint not null references public.trips(id) on delete cascade,
  user_id text not null,
  role text not null check (role in ('user', 'assistant')),
  content text not null,
  provider text not null default 'clerk',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.notification_events (
  id bigserial primary key,
  trip_id bigint not null references public.trips(id) on delete cascade,
  user_id text not null,
  type text not null,
  status text not null check (status in ('pending', 'generated')),
  title text not null,
  body text not null,
  payload_json jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.guardians (
  id bigserial primary key,
  user_id text not null,
  guardian_name text not null,
  guardian_phone text not null,
  relationship text not null,
  is_primary boolean not null default false,
  note text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(user_id, guardian_phone)
);

create table if not exists public.trip_events (
  id bigserial primary key,
  trip_id bigint not null references public.trips(id) on delete cascade,
  user_id text not null,
  event_type text not null,
  title text not null,
  content text not null,
  payload_json jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_trips_user_id on public.trips(user_id);
create index if not exists idx_trips_status on public.trips(status);
create index if not exists idx_locations_trip_recorded on public.locations(trip_id, recorded_at desc, id desc);
create index if not exists idx_emergencies_user_status on public.emergencies(user_id, status);
create index if not exists idx_chat_messages_trip_created on public.chat_messages(trip_id, created_at desc, id desc);
create index if not exists idx_notification_events_trip_created on public.notification_events(trip_id, created_at desc, id desc);
create index if not exists idx_guardians_user_id on public.guardians(user_id);
create index if not exists idx_trip_events_trip_created on public.trip_events(trip_id, created_at desc, id desc);

create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists set_trips_updated_at on public.trips;
create trigger set_trips_updated_at before update on public.trips
for each row execute function public.set_updated_at();

drop trigger if exists set_locations_updated_at on public.locations;
create trigger set_locations_updated_at before update on public.locations
for each row execute function public.set_updated_at();

drop trigger if exists set_emergencies_updated_at on public.emergencies;
create trigger set_emergencies_updated_at before update on public.emergencies
for each row execute function public.set_updated_at();

drop trigger if exists set_chat_messages_updated_at on public.chat_messages;
create trigger set_chat_messages_updated_at before update on public.chat_messages
for each row execute function public.set_updated_at();

drop trigger if exists set_notification_events_updated_at on public.notification_events;
create trigger set_notification_events_updated_at before update on public.notification_events
for each row execute function public.set_updated_at();

drop trigger if exists set_guardians_updated_at on public.guardians;
create trigger set_guardians_updated_at before update on public.guardians
for each row execute function public.set_updated_at();

drop trigger if exists set_trip_events_updated_at on public.trip_events;
create trigger set_trip_events_updated_at before update on public.trip_events
for each row execute function public.set_updated_at();
