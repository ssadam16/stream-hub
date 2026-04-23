package com.technokratos.codeexecutorservice.services;

import com.technokratos.codeexecutorservice.configuration.LanguageConfig;
import com.technokratos.common.dto.ExecResult;
import com.technokratos.common.dto.tests.TestCasePayload;
import com.technokratos.common.dto.tests.TestCaseResult;
import com.technokratos.common.enums.ErrorType;
import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.common.enums.SolutionStatus;
import com.technokratos.common.events.CodeExecutionRequest;
import com.technokratos.common.events.CodeExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class DockerExecutionService {

    private final DockerManager dockerManager;

    public CodeExecutionResult executeCode(UUID solutionId, CodeExecutionRequest request) {
        log.info("Executing code for solution: {}, language: {}", solutionId, request.language());
        log.info("Request to execute code, Code execution request: {}", request);

        Path tempFile = null;
        String containerId = null;

        try {
            LanguageConfig config = getLanguageConfig(request.language());
            tempFile = createTempFile(solutionId, request.code(), request.language());

            log.debug("Created temp file: {}", tempFile.toAbsolutePath());

            containerId = dockerManager.createContainer(
                    request.language(),
                    tempFile.toString(),
                    solutionId,
                    config
            );

            log.info("Container created: {}, starting execution...", containerId);

            dockerManager.startContainer(containerId);
            long startTime = System.currentTimeMillis();

            ExecResult compileResult = dockerManager.execCommand(
                    containerId,
                    "javac", "/workspace/Main.java"
            );

            log.info("Compilation finished: exitCode={}, stdout='{}', stderr='{}'",
                    compileResult.exitCode(),
                    compileResult.stdout(),
                    compileResult.stderr());


            if (compileResult.exitCode() != 0) {
                dockerManager.removeContainer(containerId);
                return CodeExecutionResult.builder()
                        .solutionId(solutionId)
                        .totalTests(request.testCases().size())
                        .passedTests(0)
                        .totalTimeMs(compileResult.elapsedMs())
                        .status(SolutionStatus.COMPILATION_ERROR)
                        .errorMessage(compileResult.stderr())
                        .build();
            }

            List<TestCaseResult> testResults = new ArrayList<>();

            for (TestCasePayload test : request.testCases()) {
                log.info("Running test {}, input='{}', expectedOutput='{}', timeLimit={}",
                        test.id(), test.input(), test.expectedOutput(), request.timeLimit());

                ExecResult runResult = dockerManager.execWithStdin(
                        containerId,
                        test.input() + "\n",
                        request.timeLimit(),
                        "java", "-cp", "/workspace", "Main"
                );

                log.info("Test {} finished: timedOut={}, exitCode={}, stdout='{}', stderr='{}'",
                        test.id(), runResult.timedOut(), runResult.exitCode(),
                        runResult.stdout(), runResult.stderr());

                if (runResult.timedOut()) {
                    testResults.add(new TestCaseResult(
                            test.id(),
                            false,
                            "",
                            runResult.elapsedMs(),
                            ErrorType.TIMEOUT
                    ));
                    continue;
                }

                String actual = runResult.stdout().trim();
                String expected = test.expectedOutput().trim();
                boolean passed = actual.equals(expected);

                testResults.add(new TestCaseResult(
                        test.id(),
                        passed,
                        actual,
                        runResult.elapsedMs(),
                        passed ? ErrorType.OK : ErrorType.WRONG_ANSWER
                ));
            }

            long totalTimeMs = System.currentTimeMillis() - startTime;
            dockerManager.removeContainer(containerId);

            log.info("Execution completed for solution: {}", solutionId);

            return CodeExecutionResult.builder()
                    .testResults(testResults)
                    .totalTests(request.testCases().size())
                    .status(calculateStatus(testResults))
                    .solutionId(solutionId)
                    .totalTimeMs(totalTimeMs)
                    .passedTests(testResults.stream().filter(t -> t.errorType().equals(ErrorType.OK)).count())
                    .build();


        } catch (Exception e) {
            log.error("Failed to execute code for solution: {}", solutionId, e);

            return CodeExecutionResult.builder()
                    .solutionId(solutionId)
                    .status(SolutionStatus.INTERNAL_ERROR)
                    .passedTests(0)
                    .totalTests(request.testCases().size())
                    .testResults(new ArrayList<>())
                    .errorMessage("Execution failed: %s".formatted(e.getMessage()))
                    .build();


        } finally {
            if (containerId != null) {
                dockerManager.removeContainer(containerId);
            }
            cleanTempFile(tempFile);
        }
    }

    private SolutionStatus calculateStatus(List<TestCaseResult> testResults) {
        if (testResults.isEmpty()) {
            return SolutionStatus.INTERNAL_ERROR;
        }

        boolean hasTimeout = testResults.stream()
                .anyMatch(r -> r.errorType() == ErrorType.TIMEOUT);
        if (hasTimeout) {
            return SolutionStatus.TIME_LIMIT_EXCEEDED;
        }

        // Проверяем есть ли хотя бы один RUNTIME_ERROR
        boolean hasRuntimeError = testResults.stream()
                .anyMatch(r -> r.errorType() == ErrorType.RUNTIME_ERROR);
        if (hasRuntimeError) {
            return SolutionStatus.RUNTIME_ERROR;
        }

        // Проверяем есть ли хотя бы один WRONG_ANSWER
        boolean hasWrongAnswer = testResults.stream()
                .anyMatch(r -> r.errorType() == ErrorType.WRONG_ANSWER);
        if (hasWrongAnswer) {
            return SolutionStatus.PARTIALLY_COMPLETED;
        }

        // Если все прошли
        boolean allPassed = testResults.stream()
                .allMatch(TestCaseResult::passed);

        return allPassed ? SolutionStatus.COMPLETED : SolutionStatus.PARTIALLY_COMPLETED;

    }

    private void cleanTempFile(Path tempFile) {
        if (tempFile != null) {
            try {
                Path parentDir = tempFile.getParent();

                if (parentDir != null && !parentDir.equals(Paths.get("/code-execution-temp"))) {
                    try (Stream<Path> paths = Files.walk(parentDir)) {
                        paths.sorted(Comparator.reverseOrder())
                                .forEach(path -> {
                                    try {
                                        Files.deleteIfExists(path);
                                        log.debug("Deleted: {}", path);
                                    } catch (IOException e) {
                                        log.warn("Failed to delete: {}", path, e);
                                    }
                                });
                        log.debug("Cleaned up directory: {}", parentDir);
                    }
                } else {
                    Files.deleteIfExists(tempFile);
                    log.debug("File deleted: {}", tempFile);
                }
            } catch (IOException e) {
                log.warn("Failed to clean temp file: {}", tempFile, e);
            }
        }
    }

    private LanguageConfig getLanguageConfig(ProgrammingLanguage language) {
        return switch (language) {
            case JAVA -> LanguageConfig.builder()
                    .memoryMB(512L)
                    .cpuCount(2L)
                    .timeout(30)
                    .workingDir("/workspace")
                    .command(new String[]{"/bin/sh", "-c", "javac Main.java && java Main"})
                    .build();

            case PYTHON -> LanguageConfig.builder()
                    .memoryMB(256L)
                    .cpuCount(1L)
                    .timeout(10)
                    .workingDir("/workspace")
                    .command(new String[]{"python", "/workspace/solution.py"})
                    .build();

        };
    }

    private Path createTempFile(UUID solutionId, String code, ProgrammingLanguage language) throws IOException {
        String extension = switch (language) {
            case JAVA -> ".java";
            case PYTHON -> ".py";
        };

        String fileName = language.equals(ProgrammingLanguage.JAVA) ? "Main" : "solution";

        Path solutionDir = Paths.get("/code-execution-temp", "solution-" + solutionId);
        Files.createDirectories(solutionDir);
        Path tempFile = solutionDir.resolve(fileName + extension);
        Files.writeString(tempFile, code);

        log.debug("Created file in volume: {}", tempFile);

        return tempFile;
    }

}
