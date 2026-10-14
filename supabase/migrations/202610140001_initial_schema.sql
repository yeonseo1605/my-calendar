-- My Calendar cloud data model
-- Run this file in the Supabase SQL editor or with `supabase db push`.

create extension if not exists pgcrypto;

create table if not exists public.user_settings (
  user_id uuid primary key references auth.users(id) on delete cascade,
  payload jsonb not null default '{}'::jsonb,
  schema_version integer not null default 1,
  updated_at timestamptz not null default now()
);

create table if not exists public.labels (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  client_id text not null,
  name text not null check (char_length(name) between 1 and 30),
  color text not null check (color ~ '^#[0-9A-Fa-f]{6}$'),
  sort_order integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (user_id, client_id)
);

create table if not exists public.tasks (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  client_id text not null,
  title text not null,
  due_date date,
  label_client_id text,
  completed boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (user_id, client_id)
);

create table if not exists public.event_metadata (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  google_calendar_id text not null default 'primary',
  google_event_id text not null,
  app_event_id text,
  label_client_id text,
  local_payload jsonb not null default '{}'::jsonb,
  google_updated_at timestamptz,
  deleted_at timestamptz,
  updated_at timestamptz not null default now(),
  unique (user_id, google_calendar_id, google_event_id)
);

create table if not exists public.calendar_sync_state (
  user_id uuid not null references auth.users(id) on delete cascade,
  google_calendar_id text not null default 'primary',
  sync_token text,
  last_full_sync_at timestamptz,
  last_incremental_sync_at timestamptz,
  updated_at timestamptz not null default now(),
  primary key (user_id, google_calendar_id)
);

alter table public.user_settings enable row level security;
alter table public.labels enable row level security;
alter table public.tasks enable row level security;
alter table public.event_metadata enable row level security;
alter table public.calendar_sync_state enable row level security;

create policy "users manage own settings" on public.user_settings
  for all using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);
create policy "users manage own labels" on public.labels
  for all using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);
create policy "users manage own tasks" on public.tasks
  for all using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);
create policy "users manage own event metadata" on public.event_metadata
  for all using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);
create policy "users manage own sync state" on public.calendar_sync_state
  for all using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

create function public.set_updated_at()
returns trigger language plpgsql set search_path = '' as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger user_settings_updated_at before update on public.user_settings
for each row execute function public.set_updated_at();
create trigger labels_updated_at before update on public.labels
for each row execute function public.set_updated_at();
create trigger tasks_updated_at before update on public.tasks
for each row execute function public.set_updated_at();
create trigger event_metadata_updated_at before update on public.event_metadata
for each row execute function public.set_updated_at();
create trigger calendar_sync_state_updated_at before update on public.calendar_sync_state
for each row execute function public.set_updated_at();
