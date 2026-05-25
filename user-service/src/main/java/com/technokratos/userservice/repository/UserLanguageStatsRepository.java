package com.technokratos.userservice.repository;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.userservice.model.UserLanguageStatsEntity;
import com.technokratos.userservice.repository.row_mapper.UserLanguageStatsRowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserLanguageStatsRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserLanguageStatsRowMapper rowMapper;

    private static final String SELECT_BY_ACCOUNT = """
            SELECT * FROM user_language_stats WHERE account_id = ?
            """;

    private static final String UPSERT = """
            INSERT INTO user_language_stats (account_id, language, solved_count)
            VALUES (?, ?, 1)
            ON CONFLICT (account_id, language)
            DO UPDATE SET solved_count = user_language_stats.solved_count + 1
            """;

    public List<UserLanguageStatsEntity> findByAccountId(UUID accountId) {
        return jdbcTemplate.query(SELECT_BY_ACCOUNT, rowMapper, accountId);
    }

    public void increment(UUID accountId, ProgrammingLanguage language) {
        jdbcTemplate.update(UPSERT, accountId, language.name());
    }
}