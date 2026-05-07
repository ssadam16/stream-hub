package com.technokratos.plagiarismservice.repository;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.plagiarismservice.model.SolutionSource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MongoSolutionRepository extends MongoRepository<SolutionSource, String> {

    Optional<SolutionSource> findBySolutionId(UUID solutionId);

    List<SolutionSource> findAllByTaskIdAndLanguageAndCodeHash(
            UUID taskId, ProgrammingLanguage language, String codeHash);

    List<SolutionSource> findAllByTaskIdAndLanguage(UUID taskId, ProgrammingLanguage language);
}
