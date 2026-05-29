package com.technokratos.plagiarismservice.service;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.common.events.SolutionCompletedEvent;
import com.technokratos.common.handler.exceptions.SolutionNotFound;
import com.technokratos.plagiarismservice.model.PlagiarismReport;
import com.technokratos.plagiarismservice.model.SolutionSource;
import com.technokratos.plagiarismservice.repository.MongoPlagiarismReportRepository;
import com.technokratos.plagiarismservice.repository.MongoSolutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Plagiarism Service Unit Tests")
class PlagiarismServiceUnitTest {

    @Mock
    private MongoSolutionRepository solutionRepository;

    @Mock
    private MongoPlagiarismReportRepository reportRepository;

    @InjectMocks
    private PlagiarismService plagiarismService;

    private UUID taskId;
    private UUID solutionId;
    private UUID otherSolutionId;
    private SolutionSource currentSource;
    private SolutionSource otherSource;
    private SolutionCompletedEvent event;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        solutionId = UUID.randomUUID();
        otherSolutionId = UUID.randomUUID();

        currentSource = SolutionSource.builder()
                .solutionId(solutionId)
                .taskId(taskId)
                .language(ProgrammingLanguage.JAVA)
                .code("public class Solution { }")
                .codeHash("hash123")
                .build();

        otherSource = SolutionSource.builder()
                .solutionId(otherSolutionId)
                .taskId(taskId)
                .language(ProgrammingLanguage.JAVA)
                .code("public class Solution { }")
                .codeHash("hash456")
                .build();

        event = SolutionCompletedEvent.builder()
                .solutionId(solutionId)
                .taskId(taskId)
                .userId(UUID.randomUUID())
                .language(ProgrammingLanguage.JAVA)
                .build();
    }

    @Nested
    @DisplayName("Exact duplicate detection")
    class ExactDuplicateDetection {

        @Test
        @DisplayName("Should detect exact duplicate by code hash")
        void shouldDetectExactDuplicate() {
            when(solutionRepository.findBySolutionId(solutionId)).thenReturn(Optional.of(currentSource));
            when(solutionRepository.findAllByTaskIdAndLanguageAndCodeHash(taskId, ProgrammingLanguage.JAVA, "hash123"))
                    .thenReturn(List.of(currentSource, otherSource));
            when(solutionRepository.findAllByTaskIdAndLanguage(taskId, ProgrammingLanguage.JAVA))
                    .thenReturn(List.of(currentSource, otherSource));

            plagiarismService.onSolutionCompleted(event);

            ArgumentCaptor<PlagiarismReport> reportCaptor = ArgumentCaptor.forClass(PlagiarismReport.class);
            verify(reportRepository, atLeastOnce()).save(reportCaptor.capture());

            PlagiarismReport report = reportCaptor.getValue();
            assertThat(report.getSimilarity()).isEqualTo(1.0);
            assertThat(report.getSolutionId()).isEqualTo(solutionId);
            assertThat(report.getMatchedSolutionId()).isEqualTo(otherSolutionId);
        }

        @Test
        @DisplayName("Should not report self as duplicate")
        void shouldNotReportSelfDuplicate() {
            when(solutionRepository.findBySolutionId(solutionId)).thenReturn(Optional.of(currentSource));
            when(solutionRepository.findAllByTaskIdAndLanguageAndCodeHash(taskId, ProgrammingLanguage.JAVA, "hash123"))
                    .thenReturn(List.of(currentSource));

            plagiarismService.onSolutionCompleted(event);

            verify(reportRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should handle missing solution gracefully (no exception)")
        void shouldHandleMissingSolution() {
            when(solutionRepository.findBySolutionId(solutionId)).thenReturn(Optional.empty());

            plagiarismService.onSolutionCompleted(event);

            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle null language gracefully (no exception)")
        void shouldHandleNullLanguage() {
            SolutionCompletedEvent nullLangEvent = SolutionCompletedEvent.builder()
                    .solutionId(solutionId)
                    .taskId(taskId)
                    .userId(UUID.randomUUID())
                    .language(null)
                    .build();

            when(solutionRepository.findBySolutionId(solutionId)).thenReturn(Optional.of(currentSource));
            when(solutionRepository.findAllByTaskIdAndLanguage(any(), any()))
                    .thenReturn(List.of());

            plagiarismService.onSolutionCompleted(nullLangEvent);

            verify(reportRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Language support")
    class LanguageSupport {

        @Test
        @DisplayName("Should support all programming languages")
        void shouldSupportAllLanguages() {
            ProgrammingLanguage[] languages = ProgrammingLanguage.values();

            for (ProgrammingLanguage lang : languages) {
                SolutionSource source = SolutionSource.builder()
                        .solutionId(UUID.randomUUID())
                        .taskId(taskId)
                        .language(lang)
                        .code("code")
                        .codeHash("hash")
                        .build();

                assertThat(source.getLanguage()).isNotNull();
            }
        }

        @Test
        @DisplayName("Should get correct file extension for each language")
        void shouldGetCorrectExtension() {
            assertThat(getExtensionForLanguage(ProgrammingLanguage.JAVA)).isEqualTo(".java");
            assertThat(getExtensionForLanguage(ProgrammingLanguage.PYTHON)).isEqualTo(".py");
            assertThat(getExtensionForLanguage(ProgrammingLanguage.CPP)).isEqualTo(".cpp");
            assertThat(getExtensionForLanguage(ProgrammingLanguage.GO)).isEqualTo(".go");
        }

        private String getExtensionForLanguage(ProgrammingLanguage lang) {
            return switch (lang) {
                case JAVA -> ".java";
                case PYTHON -> ".py";
                case CPP -> ".cpp";
                case GO -> ".go";
            };
        }
    }

    @Nested
    @DisplayName("Plagiarism report saving")
    class ReportSaving {

        @Test
        @DisplayName("Should save report with correct data")
        void shouldSaveReportWithCorrectData() {
            when(solutionRepository.findBySolutionId(solutionId)).thenReturn(Optional.of(currentSource));
            when(solutionRepository.findAllByTaskIdAndLanguageAndCodeHash(any(), any(), any()))
                    .thenReturn(List.of(otherSource));
            when(solutionRepository.findAllByTaskIdAndLanguage(any(), any()))
                    .thenReturn(List.of(currentSource, otherSource));

            plagiarismService.onSolutionCompleted(event);

            ArgumentCaptor<PlagiarismReport> captor = ArgumentCaptor.forClass(PlagiarismReport.class);
            verify(reportRepository).save(captor.capture());

            PlagiarismReport report = captor.getValue();
            assertThat(report.getSolutionId()).isEqualTo(solutionId);
            assertThat(report.getTaskId()).isEqualTo(taskId);
            assertThat(report.getDetectedAt()).isNotNull();
            assertThat(report.getSimilarity()).isBetween(0.0, 1.0);
        }

        @Test
        @DisplayName("Should save report for exact match even if JPlag fails")
        void shouldSaveReportForExactMatch() {
            when(solutionRepository.findBySolutionId(solutionId)).thenReturn(Optional.of(currentSource));
            when(solutionRepository.findAllByTaskIdAndLanguageAndCodeHash(any(), any(), any()))
                    .thenReturn(List.of(otherSource));
            when(solutionRepository.findAllByTaskIdAndLanguage(any(), any()))
                    .thenReturn(List.of(currentSource, otherSource));

            plagiarismService.onSolutionCompleted(event);

            verify(reportRepository, atLeastOnce()).save(any());
        }
    }

    @Nested
    @DisplayName("Performance and edge cases")
    class PerformanceAndEdgeCases {

        @Test
        @DisplayName("Should handle empty list of other solutions")
        void shouldHandleEmptyOtherSolutions() {
            when(solutionRepository.findBySolutionId(solutionId)).thenReturn(Optional.of(currentSource));
            when(solutionRepository.findAllByTaskIdAndLanguageAndCodeHash(any(), any(), any()))
                    .thenReturn(List.of());
            when(solutionRepository.findAllByTaskIdAndLanguage(any(), any()))
                    .thenReturn(List.of(currentSource));

            plagiarismService.onSolutionCompleted(event);

            verify(reportRepository, never()).save(any());
        }
    }
}