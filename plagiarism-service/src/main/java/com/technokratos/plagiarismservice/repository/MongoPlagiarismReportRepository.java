package com.technokratos.plagiarismservice.repository;

import com.technokratos.plagiarismservice.model.PlagiarismReport;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MongoPlagiarismReportRepository extends MongoRepository<PlagiarismReport, String> {
    List<PlagiarismReport> findAllBySolutionId(UUID solutionId);

    List<PlagiarismReport> findAllByTaskId(UUID taskId);
}
