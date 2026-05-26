package com.technokratos.solutionservice.scheduling;

import com.technokratos.common.enums.SolutionStatus;
import com.technokratos.solutionservice.model.SolutionEntity;
import com.technokratos.solutionservice.repository.SolutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StuckSolutionReaper {

    private final SolutionRepository solutionRepository;

    @Value("${reaper.processing-timeout-minutes:10}")
    private int timeoutMinutes;

    @Scheduled(fixedDelayString = "${reaper.interval-ms:60000}")
    @Transactional
    public void reapStuckSolutions() {
        Instant threshold = Instant.now().minus(timeoutMinutes, ChronoUnit.MINUTES);

        List<SolutionEntity> stuck = solutionRepository
                .findByStatusAndCreatedAtBefore(SolutionStatus.PROCESSING, threshold);

        if (stuck.isEmpty()) return;

        stuck.forEach(s -> {
            s.setStatus(SolutionStatus.INTERNAL_ERROR);
            log.warn("Reaping stuck solution {} created at {}", s.getId(), s.getCreatedAt());
        });

        solutionRepository.saveAll(stuck);
        log.info("Reaped {} stuck solutions older than {} minutes", stuck.size(), timeoutMinutes);
    }
}