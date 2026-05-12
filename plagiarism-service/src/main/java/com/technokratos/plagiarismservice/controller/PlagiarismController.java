package com.technokratos.plagiarismservice.controller;

import com.technokratos.plagiarismservice.model.PlagiarismReport;
import com.technokratos.plagiarismservice.repository.MongoPlagiarismReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/plagiarism")
@RequiredArgsConstructor
public class PlagiarismController {

    private final MongoPlagiarismReportRepository reportRepository;

    @GetMapping("/solution/{solutionId}")
    public List<PlagiarismReport> getBySolution(@PathVariable UUID solutionId) {
        return reportRepository.findAllBySolutionId(solutionId);
    }

    @GetMapping("/task/{taskId}/matrix")
    public List<PlagiarismReport> getMatrix(@PathVariable UUID taskId) {
        return reportRepository.findAllByTaskId(taskId);
    }
}
