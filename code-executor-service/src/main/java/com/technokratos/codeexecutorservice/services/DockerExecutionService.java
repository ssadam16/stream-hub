package com.technokratos.codeexecutorservice.services;

import com.technokratos.codeexecutorservice.configuration.LanguageConfig;
import com.technokratos.common.enums.ProgrammingLanguage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class DockerExecutionService {

    private final DockerManager dockerManager;

    public String executeCode(UUID solutionId, String code, ProgrammingLanguage language) {
        log.info("Received request to execute code: {}. Language: {}", code, language.name());

        Path tempFile = null;
        String containerId = null;

        try {
            LanguageConfig config = getLanguageConfig(language);
            tempFile = createTempFile(solutionId, code, language);

            log.debug("Created temp file: {}", tempFile.toAbsolutePath());

            containerId = dockerManager.createContainer(
                    language,
                    tempFile.toString(),
                    solutionId,
                    config
            );

            log.info("Container created: {}, starting execution...", containerId);

            dockerManager.startContainer(containerId);

            String logs = dockerManager.waitForContainer(containerId, config.getTimeout());

            log.info("Execution completed for solution: {}", solutionId);

            return logs;

        } catch (Exception e) {
            log.error("Failed to execute code for solution: {}", solutionId, e);
            throw new RuntimeException("Execution failed: " + e.getMessage(), e);
        } finally {
            if (containerId != null) {
                dockerManager.removeContainer(containerId);
            }
            cleanTempFile(tempFile);
        }
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
