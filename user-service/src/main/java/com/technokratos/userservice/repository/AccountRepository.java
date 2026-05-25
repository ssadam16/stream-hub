package com.technokratos.userservice.repository;

import com.technokratos.common.dto.PageResponse;
import com.technokratos.userservice.dto.AccountInfoResponse;
import com.technokratos.userservice.model.AccountEntity;
import com.technokratos.userservice.repository.row_mapper.AccountRowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AccountRepository {

    private final JdbcTemplate jdbcTemplate;
    private final AccountRowMapper rowMapper;

    private static final String SELECT_BY_ID = """
            SELECT * FROM account WHERE account_id = ?
            """;

    private static final String INSERT = """
            INSERT INTO account (account_id, created_at, updated_at)
            VALUES (?, now(), now())
            ON CONFLICT DO NOTHING
            """;

    private static final String UPDATE_STATS = """
            UPDATE account SET
                total_submissions     = total_submissions + 1,
                last_active           = now(),
                updated_at            = now(),
                completed_submissions = completed_submissions + ?,
                solved_tasks_count    = solved_tasks_count + ?,
                total_points          = total_points + ?,
                current_streak = CASE
                    WHEN ? = 0                                 THEN current_streak
                    WHEN DATE(last_active) = CURRENT_DATE      THEN current_streak
                    WHEN DATE(last_active) = CURRENT_DATE - 1  THEN current_streak + 1
                    ELSE 1
                END,
                max_streak = CASE
                    WHEN ? = 0                                 THEN max_streak
                    WHEN DATE(last_active) = CURRENT_DATE      THEN max_streak
                    WHEN DATE(last_active) = CURRENT_DATE - 1  THEN GREATEST(max_streak, current_streak + 1)
                    ELSE GREATEST(max_streak, 1)
                END
            WHERE account_id = ?
            """;

    private static final String INSERT_WITH_ACCOUNT_INFO = """
            insert into account (account_id, email, username, fio, created_at, updated_at)
            values (?, ?, ?, ?, now(), now())
            on conflict do nothing
            """;

    private static final String SELECT_TOP_RATING_USERS = """
            SELECT account_id, username, email, fio
            FROM account
            ORDER BY total_points DESC
            LIMIT ? OFFSET ?
            """;

    private static final String COUNT_ALL = """
            SELECT COUNT(*) FROM account
            """;

    public Optional<AccountEntity> findByAccountId(UUID accountId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(SELECT_BY_ID, rowMapper, accountId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void insert(UUID accountId) {
        jdbcTemplate.update(INSERT, accountId);
    }

    public void insert(AccountInfoResponse accountInfoResponse) {
        jdbcTemplate.update(
                INSERT_WITH_ACCOUNT_INFO,
                accountInfoResponse.accountId(),
                accountInfoResponse.email(),
                accountInfoResponse.username(),
                accountInfoResponse.fio()
        );
    }

    public void updateStats(UUID accountId, boolean completed, boolean firstSolve, int points) {
        jdbcTemplate.update(UPDATE_STATS,
                completed ? 1 : 0,
                firstSolve ? 1 : 0,
                points,
                firstSolve ? 1 : 0,
                firstSolve ? 1 : 0,
                accountId
        );
    }

    public PageResponse<AccountInfoResponse> getTopByRating(int page, int size) {
        int offset = page * size;

        List<AccountInfoResponse> content = jdbcTemplate.query(
                SELECT_TOP_RATING_USERS,
                (rs, rowNum) -> new AccountInfoResponse(
                        UUID.fromString(rs.getString("account_id")),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("fio")
                ),
                size,
                offset
        );
        Long total = jdbcTemplate.queryForObject(COUNT_ALL, Long.class);

        long totalElements = total != null ? total : 0L;

        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}