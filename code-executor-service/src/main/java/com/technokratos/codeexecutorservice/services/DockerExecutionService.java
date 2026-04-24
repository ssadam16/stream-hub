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

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class DockerExecutionService {

    private final DockerManager dockerManager;

    public CodeExecutionResult executeCode(UUID solutionId, CodeExecutionRequest request) {
        log.info("Executing code for solution: {}, language: {}", solutionId, request.language());

        String containerId = null;

        try {
            LanguageConfig config = getLanguageConfig(request.language());
            String fileName = getFileName(request.language());

            containerId = dockerManager.createContainer(request.language(), config);
            log.info("Container created: {}", containerId);

            dockerManager.startContainer(containerId);

            dockerManager.copyFileToContainer(containerId, fileName, request.code(), "/workspace");
            log.debug("Copied {} to container", fileName);

            long startTime = System.currentTimeMillis();

            // Компилируем (только для Java)
            if (request.language() == ProgrammingLanguage.JAVA) {
                ExecResult compileResult = dockerManager.execCommand(
                        containerId,
                        "javac", "/workspace/" + fileName
                );

                log.info("Compilation finished: exitCode={}, stderr='{}'",
                        compileResult.exitCode(), compileResult.stderr());

                if (compileResult.exitCode() != 0) {
                    return CodeExecutionResult.builder()
                            .solutionId(solutionId)
                            .totalTests(request.testCases().size())
                            .passedTests(0)
                            .totalTimeMs(compileResult.elapsedMs())
                            .status(SolutionStatus.COMPILATION_ERROR)
                            .errorMessage(compileResult.stderr())
                            .build();
                }
            }

            // Прогоняем тесты
            List<TestCaseResult> testResults = new ArrayList<>();

            for (TestCasePayload test : request.testCases()) {
                log.debug("Running test {}, input='{}'", test.id(), test.input());

                String[] runCommand = getRunCommand(request.language());

                ExecResult runResult = dockerManager.execWithStdin(
                        containerId,
                        test.input() + "\n",
                        request.timeLimit(),
                        runCommand
                );

                log.debug("Test {} finished: timedOut={}, exitCode={}, stdout='{}'",
                        test.id(), runResult.timedOut(), runResult.exitCode(), runResult.stdout());

                if (runResult.timedOut()) {
                    testResults.add(new TestCaseResult(
                            test.id(), false, "", runResult.elapsedMs(), ErrorType.TIMEOUT
                    ));
                    continue;
                }

                if (runResult.exitCode() != 0) {
                    testResults.add(new TestCaseResult(
                            test.id(), false, runResult.stderr(), runResult.elapsedMs(), ErrorType.RUNTIME_ERROR
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

            log.info("Execution completed for solution: {}", solutionId);

            return CodeExecutionResult.builder()
                    .testResults(testResults)
                    .totalTests(request.testCases().size())
                    .status(calculateStatus(testResults))
                    .solutionId(solutionId)
                    .totalTimeMs(totalTimeMs)
                    .passedTests(testResults.stream().filter(t -> t.errorType() == ErrorType.OK).count())
                    .build();

        } catch (Exception e) {
            log.error("Failed to execute code for solution: {}", solutionId, e);

            return CodeExecutionResult.builder()
                    .solutionId(solutionId)
                    .status(SolutionStatus.INTERNAL_ERROR)
                    .passedTests(0)
                    .totalTests(request.testCases() != null ? request.testCases().size() : 0)
                    .testResults(new ArrayList<>())
                    .errorMessage("Execution failed: " + e.getMessage())
                    .build();

        } finally {
            if (containerId != null) {
                dockerManager.removeContainer(containerId);
            }
        }
    }

    private SolutionStatus calculateStatus(List<TestCaseResult> testResults) {
        if (testResults.isEmpty()) {
            return SolutionStatus.INTERNAL_ERROR;
        }

        if (testResults.stream().anyMatch(r -> r.errorType() == ErrorType.TIMEOUT)) {
            return SolutionStatus.TIME_LIMIT_EXCEEDED;
        }

        if (testResults.stream().anyMatch(r -> r.errorType() == ErrorType.RUNTIME_ERROR)) {
            return SolutionStatus.RUNTIME_ERROR;
        }

        if (testResults.stream().anyMatch(r -> r.errorType() == ErrorType.WRONG_ANSWER)) {
            return SolutionStatus.PARTIALLY_COMPLETED;
        }

        boolean allPassed = testResults.stream().allMatch(TestCaseResult::passed);
        return allPassed ? SolutionStatus.COMPLETED : SolutionStatus.PARTIALLY_COMPLETED;
    }

    private String getFileName(ProgrammingLanguage language) {
        return switch (language) {
            case JAVA -> "Main.java";
            case PYTHON -> "solution.py";
        };
    }

    private String[] getRunCommand(ProgrammingLanguage language) {
        return switch (language) {
            case JAVA -> new String[]{"java", "-cp", "/workspace", "Main"};
            case PYTHON -> new String[]{"python", "-u", "/workspace/solution.py"};
        };
    }

    private LanguageConfig getLanguageConfig(ProgrammingLanguage language) {
        return switch (language) {
            case JAVA -> LanguageConfig.builder()
                    .memoryMB(512L)
                    .cpuCount(2L)
                    .timeout(30)
                    .workingDir("/workspace")
                    .build();

            case PYTHON -> LanguageConfig.builder()
                    .memoryMB(256L)
                    .cpuCount(1L)
                    .timeout(10)
                    .workingDir("/workspace")
                    .build();
        };
    }
}