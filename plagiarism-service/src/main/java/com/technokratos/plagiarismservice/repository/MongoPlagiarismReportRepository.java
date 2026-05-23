package com.technokratos.plagiarismservice.repository;

import com.technokratos.plagiarismservice.model.PlagiarismReport;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

public interface MongoPlagiarismReportRepository extends MongoRepository<PlagiarismReport, String> {
    List<PlagiarismReport> findAllBySolutionId(UUID solutionId);

    List<PlagiarismReport> findAllByTaskId(UUID taskId);
}
