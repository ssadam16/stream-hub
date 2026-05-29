package com.technokratos.plagiarismservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "plagiarism_reports")
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlagiarismReport {
    @Id
    private String id;

    private UUID solutionId;
    private UUID matchedSolutionId;
    private UUID taskId;
    private double similarity; // 0.0 - 1.0
    private Instant detectedAt;
}