create table if not exists tasks (
    id uuid primary key default gen_random_uuid(),
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,
    external_id varchar(255) unique,
    title varchar(255) not null,
    condition text not null,
    author_id uuid not null,
    difficulty varchar(50) not null check (difficulty in('EASY', 'MEDIUM', 'HARD', 'IMPOSSIBLE')),
    type varchar(50) not null check (type in('CONSOLE_IO', 'FUNCTION_UNIT, STRESS_TEST')),
    time_limit int,
    memory_limit int
);

create table if not exists test_cases (
    id uuid primary key default gen_random_uuid(),
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,
    task_id uuid not null references tasks(id) on delete cascade,
    input varchar(1023) not null,
    expected_output varchar(1023) not null,
    is_hidden boolean not null
);