package com.technokratos.plagiarismservice.service;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.common.events.SolutionCompletedEvent;
import com.technokratos.common.handler.exceptions.SolutionNotFound;
import com.technokratos.plagiarismservice.model.PlagiarismReport;
import com.technokratos.plagiarismservice.model.SolutionSource;
import com.technokratos.plagiarismservice.repository.MongoPlagiarismReportRepository;
import com.technokratos.plagiarismservice.repository.MongoSolutionRepository;
import de.jplag.*;
import de.jplag.cpp.CPPLanguage;
import de.jplag.golang.GoLanguage;
import de.jplag.java.JavaLanguage;
import de.jplag.options.JPlagOptions;
import de.jplag.python3.PythonLanguage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlagiarismService {

    private static final double SIMILARITY_THRESHOLD = 0.8;

    private final MongoSolutionRepository mongoSolutionRepository;
    private final MongoPlagiarismReportRepository plagiarismReportRepository;

    @KafkaListener(groupId = "plagiarism-service-group", topics = "solution-completed-events")
    public void onSolutionCompeted(SolutionCompletedEvent event) {
        log.info("Checking plagiarism for solution: {}", event.solutionId());
        try {
            check(event);
        } catch (Exception e) {
            log.error("Failed to check plagiarism for solution: {}", event.solutionId(), e);
        }
    }

    private void check(SolutionCompletedEvent event) throws IOException {

        SolutionSource current = mongoSolutionRepository.findBySolutionId(event.solutionId())
                .orElseThrow(() -> new SolutionNotFound(event.solutionId()));

        List<SolutionSource> exactDuplicates = mongoSolutionRepository
                .findAllByTaskIdAndLanguageAndCodeHash(
                        event.taskId(),
                        event.language(),
                        current.getCodeHash()
                )
                .stream()
                .filter(s -> !s.getSolutionId().equals(event.solutionId()))
                .toList();

        for (SolutionSource duplicate : exactDuplicates) {
            log.warn("Exact copy detected! solution={} == solution={}",
                    event.solutionId(), duplicate.getSolutionId());
            savePlagiarismReport(current, duplicate, 1.0);
        }

        List<SolutionSource> others = mongoSolutionRepository
                .findAllByTaskIdAndLanguage(event.taskId(), event.language())
                .stream()
                .filter(s -> !s.getCodeHash().equals(current.getCodeHash()))
                .filter(s -> !s.getSolutionId().equals(event.solutionId()))
                .toList();

        if (others.isEmpty()) {
            log.info("No other solutions to compare for task={}, language={}",
                    event.taskId(), event.language());
            return;
        }

        runJPlagBatch(current, others, event.language());
    }

    private void runJPlagBatch(SolutionSource current, List<SolutionSource> others, ProgrammingLanguage language) throws IOException {
        Path tempRoot = Files.createTempDirectory("jplag_");

        try {
            String extension = getExtension(language);

            createSubmissionDir(tempRoot, current.getSolutionId().toString(),
                    current.getCode(), extension);

            for (SolutionSource other : others) {
                createSubmissionDir(tempRoot, other.getSolutionId().toString(),
                        other.getCode(), extension);
            }

            JPlagOptions options = new JPlagOptions(
                    getJPlagLanguage(language),
                    Set.of(tempRoot.toFile()),
                    Set.of()
            ).withMinimumTokenMatch(9)
                    .withSimilarityThreshold(SIMILARITY_THRESHOLD);

            JPlagResult result = JPlag.run(options);

            Map<UUID, SolutionSource> othersById = new HashMap<>();

            for (SolutionSource other : others) {
                othersById.put(other.getSolutionId(), other);
            }

            for (JPlagComparison comparison : result.getAllComparisons()) {
                Submission a = comparison.firstSubmission();
                Submission b = comparison.secondSubmission();

                // Имя submission = ID директории = solutionId
                UUID idA = UUID.fromString(a.getName());
                UUID idB = UUID.fromString(b.getName());

                SolutionSource matched;

                if (idA.equals(current.getSolutionId())) {
                    matched = othersById.get(idB);
                } else if (idB.equals(current.getSolutionId())) {
                    matched = othersById.get(idA);
                } else {
                    continue;
                }

                if (matched == null) continue;

                double similarity = comparison.similarity();

                if (similarity >= SIMILARITY_THRESHOLD) {
                    log.warn("Similar code detected! solution={} ~ solution={}, similarity={}",
                            current.getSolutionId(), matched.getSolutionId(), similarity);
                    savePlagiarismReport(current, matched, similarity);
                }

            }
        } catch (Exception e) {
            log.error("JPlag failed", e);
        } finally {
            deleteRecursively(tempRoot);
        }

    }

    private void deleteRecursively(Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            log.warn("Failed to clean up temp dir: {}", root, e);
        }
    }

    private void createSubmissionDir(Path tempRoot, String solutionId, String code, String extension) throws IOException {
        Path dir = tempRoot.resolve(solutionId);
        Files.createDirectories(dir);
        Path file = dir.resolve("solution" + extension);
        Files.writeString(file, code);
    }


    private Language getJPlagLanguage(ProgrammingLanguage language) {
        return switch (language) {
            case JAVA -> new JavaLanguage();
            case PYTHON -> new PythonLanguage();
            case GO -> new GoLanguage();
            case CPP -> new CPPLanguage();
        };
    }

    private String getExtension(ProgrammingLanguage language) {
        return switch (language) {
            case JAVA ->  ".java";
            case PYTHON -> ".py";
            case CPP -> ".cpp";
            case GO -> ".go";
        };
    }

    private void savePlagiarismReport(SolutionSource current, SolutionSource other, double similarity) {
        PlagiarismReport plagiarismReport = PlagiarismReport.builder()
                .similarity(similarity)
                .detectedAt(Instant.now())
                .solutionId(current.getSolutionId())
                .matchedSolutionId(other.getSolutionId())
                .taskId(current.getTaskId())
                .build();

        plagiarismReportRepository.save(plagiarismReport);
        log.info("Plagiarism report saved: {} ~ {} (similarity={})",
                current.getSolutionId(), other.getSolutionId(), similarity);
    }
}
