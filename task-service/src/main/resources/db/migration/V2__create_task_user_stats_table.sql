create table task_user_stats(
    task_id uuid not null,
    user_id uuid not null,
    accepted boolean not null,
    primary key (task_id, user_id)
)
