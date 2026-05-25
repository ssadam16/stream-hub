package com.technokratos.userservice.repository.row_mapper;

import com.technokratos.userservice.model.AccountEntity;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Component
public class AccountRowMapper implements RowMapper<AccountEntity> {
    @Override
    public @Nullable AccountEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        return AccountEntity.builder()
                .email(rs.getString("email"))
                .fio(rs.getString("fio"))
                .username(rs.getString("username"))
                .accountId(UUID.fromString(rs.getString("account_id")))
                .totalSubmissions(rs.getLong("total_submissions"))
                .completedSubmissions(rs.getLong("completed_submissions"))
                .solvedTasksCount(rs.getLong("solved_tasks_count"))
                .totalPoints(rs.getLong("total_points"))
                .lastActive(rs.getTimestamp("last_active") != null ? rs.getTimestamp("last_active").toInstant() : null)
                .currentStreak(rs.getLong("current_streak"))
                .maxStreak(rs.getLong("max_streak"))
                .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null)
                .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : null)
                .build();
    }
}
