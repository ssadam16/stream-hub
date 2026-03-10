create table if not exists tasks (
    id uuid primary key default gen_random_uuid(),
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,
    title varchar(255) not null,
    condition text not null,
    author_id uuid not null,
    difficulty varchar(50) not null check (difficulty in('EASY', 'MEDIUM', 'HARD', 'IMPOSSIBLE')),
    type varchar(50) not null check (type in('CONSOLE', 'UNIT')),
    time_limit int,
    memory_limit int
);

create table if not exists console_test_cases (
    id uuid primary key default gen_random_uuid(),
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,
    task_id uuid not null references tasks(id) on delete cascade,
    is_example boolean not null default false,
    is_passed boolean not null default false,
    order_num smallint not null,
    input varchar(1023) not null,
    expected_output varchar(1023) not null
);