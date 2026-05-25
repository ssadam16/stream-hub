CREATE TABLE solved_tasks
(
    account_id UUID REFERENCES account (account_id) ON DELETE CASCADE,
    task_id    UUID NOT NULL,
    solved_at  TIMESTAMPTZ DEFAULT now(),
    PRIMARY KEY (account_id, task_id)
);