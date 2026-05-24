package com.technokratos.userservice.service;

import com.technokratos.common.events.TaskSubmissionEvent;
import com.technokratos.userservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserStatisticService {

    private final AccountRepository accountRepository;
    private final SolvedTaskRepository solvedTaskRepository;
    private final UserDifficultyStatsRepository difficultyStatsRepository;
    private final UserLanguageStatsRepository languageStatsRepository;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public void applySubmission(TaskSubmissionEvent event) {
        if (!processedEventRepository.tryMarkProcessed(event.solutionId())) {
            log.info("Event {} already processed, skipping", event.solutionId());
            return;
        }

        accountRepository.insert(event.userId());

        log.info("completed={} firstSolve will be checked", event.completed());

        boolean firstSolve = false;
        if (event.completed()) {
            firstSolve = solvedTaskRepository.insertIfAbsent(event.userId(), event.taskId());
        }

        log.info("firstSolve={}", firstSolve);

        accountRepository.updateStats(
                event.userId(),
                event.completed(),
                firstSolve,
                firstSolve ? event.points() : 0
        );

        if (firstSolve) {
            difficultyStatsRepository.increment(event.userId(), event.difficulty());
            log.info("difficulty incremented: {} {}", event.userId(), event.difficulty());
            languageStatsRepository.increment(event.userId(), event.language());
            log.info("language incremented: {} {}", event.userId(), event.language());
        }

        log.info("Applied submission: userId={} accepted={} firstSolve={}",
                event.userId(), event.completed(), firstSolve);
    }
}