-- Основная статистика пользователя
CREATE TABLE account
(
    account_id              UUID PRIMARY KEY,
    email VARCHAR(100) UNIQUE,
    username VARCHAR(100),
    fio VARCHAR(100),
    total_submissions    BIGINT NOT NULL  DEFAULT 0,
    completed_submissions BIGINT NOT NULL  DEFAULT 0,
    solved_tasks_count   BIGINT NOT NULL  DEFAULT 0,
    total_points         BIGINT NOT NULL  DEFAULT 0,
    rating               BIGINT           DEFAULT 0,
    last_active          TIMESTAMPTZ,
    current_streak       INT           DEFAULT 0,
    max_streak           INT           DEFAULT 0,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);

CREATE TABLE user_language_stats
(
    account_id      UUID REFERENCES account (account_id) ON DELETE CASCADE,
    language     VARCHAR(20),
    solved_count INT NOT NULL DEFAULT 0,
    PRIMARY KEY (account_id, language)
);

CREATE TABLE user_difficulty_stats
(
    account_id      UUID REFERENCES account (account_id) ON DELETE CASCADE,
    difficulty   VARCHAR(20),
    solved_count INT NOT NULL DEFAULT 0,
    PRIMARY KEY (account_id, difficulty)
);