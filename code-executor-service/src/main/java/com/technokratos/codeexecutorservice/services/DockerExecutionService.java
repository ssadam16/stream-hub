package com.technokratos.codeexecutorservice.services;

import com.technokratos.codeexecutorservice.configuration.LanguageConfig;
import com.technokratos.common.enums.ProgrammingLanguage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
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
        Path tempFile = null;
        String containerId = null;

        try {
            LanguageConfig config = getLanguageConfig(language, solutionId);
            tempFile = createTempFile(solutionId, code, language);

            log.info("Created temp file: {}", tempFile.toAbsolutePath());

            containerId = dockerManager.createContainer(
                    language,
                    tempFile.toString(),
                    solutionId,
                    config
            );

            log.info("Container created: {}, starting execution...", containerId);

            // ЗАПУСКАЕМ КОНТЕЙНЕР
            dockerManager.startContainer(containerId);

            // ЖДЕМ ЗАВЕРШЕНИЯ И ПОЛУЧАЕМ ЛОГИ
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
                // Получаем родительскую директорию (solution-xxx)
                Path parentDir = tempFile.getParent();

                // Проверяем, что это не корневая директория /code-execution-temp
                if (parentDir != null && !parentDir.equals(Paths.get("/code-execution-temp"))) {
                    // Рекурсивно удаляем всю директорию solution-xxx
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

    private LanguageConfig getLanguageConfig(ProgrammingLanguage language, UUID solutionId) {
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

        log.info("🔥🔥🔥 TEMP FILE CREATED AT: {}", tempFile.toAbsolutePath());
        log.debug("Created file in volume: {}", tempFile);

        return tempFile;
    }

}
