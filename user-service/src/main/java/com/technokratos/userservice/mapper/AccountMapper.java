package com.technokratos.userservice.mapper;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.userservice.dto.AccountResponse;
import com.technokratos.userservice.dto.UserDifficultyStatsResponse;
import com.technokratos.userservice.dto.UserLanguageStatsResponse;
import com.technokratos.userservice.model.AccountEntity;
import com.technokratos.userservice.model.UserDifficultyStatsEntity;
import com.technokratos.userservice.model.UserLanguageStatsEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AccountMapper {

    public AccountResponse toAccountResponse(
            AccountEntity account,
            List<UserDifficultyStatsEntity> difficultyStats,
            List<UserLanguageStatsEntity> languageStats
    ) {
        return AccountResponse.builder()
                .accountId(account.getAccountId())
                .username(account.getUsername())
                .email(account.getEmail())
                .fio(account.getFio())
                .totalSubmissions(account.getTotalSubmissions())
                .acceptedSubmissions(account.getCompletedSubmissions())
                .solvedTasksCount(account.getSolvedTasksCount())
                .totalPoints(account.getTotalPoints())
                .acceptance(account.getTotalSubmissions() == 0 ? 0.0 :
                        (double) account.getCompletedSubmissions() / account.getTotalSubmissions() * 100)
                .lastActive(account.getLastActive())
                .currentStreak(account.getCurrentStreak())
                .maxStreak(account.getMaxStreak())
                .userDifficultyStats(toDifficultyResponse(difficultyStats))
                .userLanguageStats(toLanguageResponse(languageStats))
                .build();
    }

    private UserDifficultyStatsResponse toDifficultyResponse(List<UserDifficultyStatsEntity> entities) {
        Map<TaskDifficulty, Integer> stats = entities.stream()
                .collect(Collectors.toMap(
                        UserDifficultyStatsEntity::getDifficulty,
                        UserDifficultyStatsEntity::getSolvedCount
                ));
        return UserDifficultyStatsResponse.builder().stats(stats).build();
    }

    private UserLanguageStatsResponse toLanguageResponse(List<UserLanguageStatsEntity> entities) {
        Map<ProgrammingLanguage, Integer> stats = entities.stream()
                .collect(Collectors.toMap(
                        UserLanguageStatsEntity::getLanguage,
                        UserLanguageStatsEntity::getSolvedCount
                ));
        return UserLanguageStatsResponse.builder().stats(stats).build();
    }
}
