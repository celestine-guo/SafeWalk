-- SafeWalk security baseline for Supabase PostgreSQL
-- 说明：
-- 1) SafeWalk 当前通过 FastAPI + Clerk 访问数据库，不使用 Supabase Data API 直接读写业务表。
-- 2) 以下策略默认收紧 anon/authenticated/service_role 对业务表的直接访问。
-- 3) 如果你后续要开放 Data API，请显式 GRANT + RLS policy，不要删除整段安全基线。

create schema if not exists app_private;

create or replace function app_private.current_user_id()
returns text
language sql
stable
set search_path = ''
as $$
  select nullif(current_setting('app.current_user_id', true), '');
$$;

-- 默认不暴露给 Supabase Data API 常见角色
revoke all on all tables in schema public from anon, authenticated;

-- 防御性 RLS：即使误授予表权限，也按 user_id 限制行访问
alter table public.trips enable row level security;
alter table public.locations enable row level security;
alter table public.emergencies enable row level security;
alter table public.chat_messages enable row level security;
alter table public.notification_events enable row level security;
alter table public.guardians enable row level security;
alter table public.trip_events enable row level security;

drop policy if exists trips_user_isolation on public.trips;
create policy trips_user_isolation on public.trips
for all to authenticated
using ((select app_private.current_user_id()) is not null and user_id = (select app_private.current_user_id()))
with check ((select app_private.current_user_id()) is not null and user_id = (select app_private.current_user_id()));

drop policy if exists emergencies_user_isolation on public.emergencies;
create policy emergencies_user_isolation on public.emergencies
for all to authenticated
using ((select app_private.current_user_id()) is not null and user_id = (select app_private.current_user_id()))
with check ((select app_private.current_user_id()) is not null and user_id = (select app_private.current_user_id()));

drop policy if exists guardians_user_isolation on public.guardians;
create policy guardians_user_isolation on public.guardians
for all to authenticated
using ((select app_private.current_user_id()) is not null and user_id = (select app_private.current_user_id()))
with check ((select app_private.current_user_id()) is not null and user_id = (select app_private.current_user_id()));

drop policy if exists chat_messages_user_isolation on public.chat_messages;
create policy chat_messages_user_isolation on public.chat_messages
for all to authenticated
using ((select app_private.current_user_id()) is not null and user_id = (select app_private.current_user_id()))
with check ((select app_private.current_user_id()) is not null and user_id = (select app_private.current_user_id()));

drop policy if exists notification_events_user_isolation on public.notification_events;
create policy notification_events_user_isolation on public.notification_events
for all to authenticated
using ((select app_private.current_user_id()) is not null and user_id = (select app_private.current_user_id()))
with check ((select app_private.current_user_id()) is not null and user_id = (select app_private.current_user_id()));

drop policy if exists trip_events_user_isolation on public.trip_events;
create policy trip_events_user_isolation on public.trip_events
for all to authenticated
using ((select app_private.current_user_id()) is not null and user_id = (select app_private.current_user_id()))
with check ((select app_private.current_user_id()) is not null and user_id = (select app_private.current_user_id()));

drop policy if exists locations_user_isolation on public.locations;
create policy locations_user_isolation on public.locations
for all to authenticated
using (
  exists (
    select 1
    from public.trips t
    where t.id = locations.trip_id
      and t.user_id = (select app_private.current_user_id())
  )
)
with check (
  exists (
    select 1
    from public.trips t
    where t.id = locations.trip_id
      and t.user_id = (select app_private.current_user_id())
  )
);
