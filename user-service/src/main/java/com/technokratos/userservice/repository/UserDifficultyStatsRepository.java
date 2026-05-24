package com.technokratos.userservice.repository;

import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.userservice.model.UserDifficultyStatsEntity;
import com.technokratos.userservice.repository.row_mapper.UserDifficultyStatsRowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserDifficultyStatsRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserDifficultyStatsRowMapper rowMapper;

    private static final String SELECT_BY_ACCOUNT = """
            SELECT * FROM user_difficulty_stats WHERE account_id = ?
            """;

    private static final String UPSERT = """
            INSERT INTO user_difficulty_stats (account_id, difficulty, solved_count)
            VALUES (?, ?, 1)
            ON CONFLICT (account_id, difficulty)
            DO UPDATE SET solved_count = user_difficulty_stats.solved_count + 1
            """;

    public List<UserDifficultyStatsEntity> findByAccountId(UUID accountId) {
        return jdbcTemplate.query(SELECT_BY_ACCOUNT, rowMapper, accountId);
    }

    public void increment(UUID accountId, TaskDifficulty difficulty) {
        jdbcTemplate.update(UPSERT, accountId, difficulty.name());
    }
}