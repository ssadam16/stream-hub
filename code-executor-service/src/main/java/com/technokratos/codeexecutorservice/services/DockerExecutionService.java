package com.technokratos.codeexecutorservice.services;

import com.technokratos.codeexecutorservice.comparator.OutputComparator;
import com.technokratos.codeexecutorservice.configuration.ExecutorProperties;
import com.technokratos.codeexecutorservice.executors.LanguageExecutor;
import com.technokratos.codeexecutorservice.executors.LanguageExecutorFactory;
import com.technokratos.common.dto.ExecResult;
import com.technokratos.common.dto.tests.TestCasePayload;
import com.technokratos.common.dto.tests.TestCaseResult;
import com.technokratos.common.enums.ErrorType;
import com.technokratos.common.enums.SolutionStatus;
import com.technokratos.common.events.CodeExecutionRequest;
import com.technokratos.common.events.CodeExecutionResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Semaphore;

@Service
@Slf4j
@RequiredArgsConstructor
public class DockerExecutionService {

    private final DockerManager dockerManager;
    private final LanguageExecutorFactory factory;
    private final ExecutorProperties executorProperties;
    private  Semaphore containerSemaphore;

    @PostConstruct
    public void init() {
        containerSemaphore = new Semaphore(executorProperties.getMaxConcurrentContainers());
        log.info("Semaphore has been initialized with max concurrent containers = {}", executorProperties.getMaxConcurrentContainers());
    }

    public CodeExecutionResult executeCode(UUID solutionId, CodeExecutionRequest request) {
        log.info("Waiting for container slot, solutionId={}", solutionId);

        try {
            containerSemaphore.acquire();  // ждём свободный слот
            log.info("Got container slot, solutionId={}", solutionId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CodeExecutionResult.builder()
                    .solutionId(solutionId)
                    .errorMessage("Interrupted while waiting for container slot")
                    .status(SolutionStatus.INTERNAL_ERROR)
                    .build();
        }

        String containerId = null;

        try {
            LanguageExecutor executor = factory.get(request.language());

            String fileName = executor.getSourceFileName();

            containerId = dockerManager.createContainer(executor);
            log.info("Container created: {}", containerId);

            dockerManager.startContainer(containerId);

            dockerManager.copyFileToContainer(containerId, fileName, request.code(), "/workspace");
            log.debug("Copied {} to container", fileName);

            long startTime = System.currentTimeMillis();

            String[] compileCommand = executor.getCompileCommand();

            if (compileCommand != null) {

                ExecResult compileResult = dockerManager.execCommand(
                        containerId,
                        compileCommand
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


            List<TestCaseResult> testResults = new ArrayList<>();

            for (TestCasePayload test : request.testCases()) {
                log.debug("Running test {}, input='{}'", test.id(), test.input());

                String[] runCommand = executor.getRunCommand();

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
                boolean passed = OutputComparator.compare(actual, expected);

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
            containerSemaphore.release();
            log.info("Released container slot, solutionId={}", solutionId);
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
}