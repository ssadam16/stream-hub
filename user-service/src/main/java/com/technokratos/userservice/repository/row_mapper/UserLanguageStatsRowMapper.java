package com.technokratos.userservice.repository.row_mapper;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.userservice.model.UserLanguageStatsEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Component
public class UserLanguageStatsRowMapper implements RowMapper<UserLanguageStatsEntity> {


    @Override
    public @Nullable UserLanguageStatsEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        return UserLanguageStatsEntity.builder()
                .accountId(UUID.fromString(rs.getString("account_id")))
                .solvedCount(rs.getInt("solved_count"))
                .language(ProgrammingLanguage.valueOf(rs.getString("language")))
                .build();
    }
}