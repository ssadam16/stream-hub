package com.technokratos.userservice.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SolvedTaskRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String INSERT_IF_ABSENT = """
            INSERT INTO solved_tasks (account_id, task_id)
            VALUES (?, ?)
            ON CONFLICT DO NOTHING
            """;

    public boolean insertIfAbsent(UUID accountId, UUID taskId) {
        int rows = jdbcTemplate.update(INSERT_IF_ABSENT, accountId, taskId);
        return rows == 1;
    }
}