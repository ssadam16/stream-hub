package com.technokratos.userservice.model;

import com.technokratos.common.enums.ProgrammingLanguage;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Builder
@Data
public class UserLanguageStatsEntity {
    private UUID accountId;
    private ProgrammingLanguage language;
    private int solvedCount;
}
