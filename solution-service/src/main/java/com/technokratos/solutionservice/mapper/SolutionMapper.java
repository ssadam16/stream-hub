package com.technokratos.solutionservice.mapper;

import com.technokratos.common.dto.SolutionShortView;
import com.technokratos.common.dto.SolutionView;
import com.technokratos.common.dto.tests.TestCaseFull;
import com.technokratos.common.dto.tests.TestCasePublic;
import com.technokratos.solutionservice.model.SolutionEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SolutionMapper {

    public SolutionView toView(SolutionEntity s, String code) {
        return SolutionView.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .taskId(s.getTaskId())
                .language(s.getLanguage())
                .status(s.getStatus())
                .passedTests(s.getPassedTests())
                .totalTests(s.getTotalTests())
                .totalTimeMs(s.getTotalTimeMs())
                .testResults(s.getTestResults())
                .code(code)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    public SolutionShortView toShortView(SolutionEntity s) {
        return SolutionShortView.builder()
                .id(s.getId())
                .taskId(s.getTaskId())
                .language(s.getLanguage())
                .status(s.getStatus())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    public List<TestCasePublic> toExecutorTests(List<TestCaseFull> cases) {
        if (cases == null || cases.isEmpty()) {
            return List.of();
        }
        return cases.stream()
                .map(c -> TestCasePublic.builder()
                        .id(c.id())
                        .input(c.input())
                        .expectedOutput(c.expectedOutput())
                        .build())
                .toList();
    }
}
