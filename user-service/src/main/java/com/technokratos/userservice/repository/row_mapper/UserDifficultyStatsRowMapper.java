package com.technokratos.userservice.repository.row_mapper;

import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.userservice.model.UserDifficultyStatsEntity;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Component
public class UserDifficultyStatsRowMapper implements RowMapper<UserDifficultyStatsEntity> {


    @Override
    public @Nullable UserDifficultyStatsEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        return UserDifficultyStatsEntity.builder()
                .accountId(UUID.fromString(rs.getString("account_id")))
                .solvedCount(rs.getInt("solved_count"))
                .difficulty(TaskDifficulty.valueOf(rs.getString("difficulty")))
                .build();
    }
}
