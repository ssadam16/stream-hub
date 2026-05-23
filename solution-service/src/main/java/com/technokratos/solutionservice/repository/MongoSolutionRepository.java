package com.technokratos.solutionservice.repository;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.solutionservice.model.SolutionSource;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MongoSolutionRepository extends MongoRepository<SolutionSource, String> {
    Optional<SolutionSource> findBySolutionIdAndUserId(UUID solutionId, UUID userId);

    boolean existsByUserIdAndNormalizedCodeAndTaskIdAndLanguage(UUID userId, String normalizedCode, UUID taskId, ProgrammingLanguage language);
}
