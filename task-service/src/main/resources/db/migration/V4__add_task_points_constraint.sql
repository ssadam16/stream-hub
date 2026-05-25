ALTER TABLE tasks
    DROP CONSTRAINT tasks_points_check,
    ADD CONSTRAINT tasks_points_check CHECK (points >= 0 AND points <= 1000);