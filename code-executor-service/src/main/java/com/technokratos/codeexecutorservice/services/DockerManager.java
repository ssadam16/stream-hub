package com.technokratos.codeexecutorservice.services;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.*;
import com.technokratos.codeexecutorservice.configuration.DockerProperties;
import com.technokratos.codeexecutorservice.configuration.LanguageConfig;
import com.technokratos.common.dto.ExecResult;
import com.technokratos.common.enums.ProgrammingLanguage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DockerManager {

    private final DockerProperties properties;
    private final DockerClient dockerClient;

    private void ensureImageExists(String imageName) {
        try {
            List<Image> images = dockerClient.listImagesCmd().exec();
            boolean imageExists = images.stream()
                    .anyMatch(img -> {
                        if (img.getRepoTags() != null) {
                            for (String tag : img.getRepoTags()) {
                                if (tag.equals(imageName)) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    });

            if (!imageExists) {
                boolean pulled = dockerClient.pullImageCmd(imageName)
                        .exec(new PullImageResultCallback())
                        .awaitCompletion(5, TimeUnit.MINUTES);
                if (pulled) {
                    log.info("Image successfully pulled");
                } else {
                    log.error("Failed to pull image within timeout");
                }
            } else {
                log.info("Image {} is already exists", imageName);
            }
        } catch (Exception e) {
            log.error("Cannot download image", e);
            //todo: че нибудь сделать тут
        }
    }


    public ExecResult execCommand(String containerId, String... command) {
        try {
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();

            var execCmd = dockerClient.execCreateCmd(containerId)
                    .withCmd(command)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();

            long startTime = System.currentTimeMillis();
            try (var callback = dockerClient.execStartCmd(execCmd.getId())
                    .exec(new ResultCallbackTemplate<>() {
                        @Override
                        public void onNext(Frame frame) {
                            try {
                                if (frame.getStreamType() == StreamType.STDOUT) {
                                    stdout.write(frame.getPayload());
                                } else if (frame.getStreamType() == StreamType.STDERR) {
                                    stderr.write(frame.getPayload());
                                }
                            } catch (IOException e) {
                                log.error("Error reading exec output", e);
                            }
                        }
                    })) {
                callback.awaitCompletion();
            }
            long elapsedMs = System.currentTimeMillis() - startTime;

            String output = stdout.toString(StandardCharsets.UTF_8);
            String error = stderr.toString(StandardCharsets.UTF_8);

            var inspectExec = dockerClient.inspectExecCmd(execCmd.getId()).exec();
            int exitCode = inspectExec.getExitCodeLong() != null
                    ? inspectExec.getExitCodeLong().intValue()
                    : 0;

            if (!error.isEmpty()) {
                log.debug("Exec stderr: {}", error);
            }

            return new ExecResult(
                    output,
                    error,
                    exitCode,
                    elapsedMs,
                    false
            );
        } catch (Exception e) {
            log.error("Failed to exec command with stdin", e);
            throw new RuntimeException("Exec failed", e);
        }
    }

    public String createContainer(ProgrammingLanguage language, String hostFilePath, UUID solutionId, LanguageConfig config) {
        String imageName = resolveImage(language);
        ensureImageExists(imageName);

        Path hostPath = Paths.get(hostFilePath);
        String hostDir = hostPath.getParent().toString();
        log.debug("HostDir: {}", hostDir);
        String containerWorkDir = config.getWorkingDir();

        log.info("Mounting {} -> {}", hostDir, containerWorkDir);

        Path bindPath = Paths.get(properties.getBindPath());

        if (!Files.exists(bindPath) && !Files.isDirectory(bindPath)) {
            try {
                Files.createDirectory(bindPath);
            } catch (IOException e) {
                log.error("Failed to create directory: {}", bindPath.toAbsolutePath());
                throw new RuntimeException("Failed to create bind host path");
            }
        }

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withMemory(config.getMemoryMB() * 1024L * 1024L)
                .withCpuCount(config.getCpuCount())
                .withNetworkMode("none")
                .withMemorySwap(0L)
                .withBinds(new Bind("%s/solution-%s".formatted(properties.getBindPath(), solutionId), new Volume(containerWorkDir)));

        CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                .withHostConfig(hostConfig)
                .withWorkingDir(containerWorkDir)
                .withCmd("tail", "-f", "/dev/null")
                .withAttachStdout(true)
                .withAttachStderr(true)
                .exec();

        log.info("Created container {} for language {} with command: {}",
                container.getId(), language, String.join(" ", config.getCommand()));

        return container.getId();
    }

    public ExecResult execWithStdin(String containerId, String input, int timeoutSeconds, String... command) {
        try {
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();

            var execCreateResp = dockerClient.execCreateCmd(containerId)
                    .withCmd(command)
                    .withWorkingDir("/workspace")
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();

            String execId = execCreateResp.getId();
            long startTime = System.currentTimeMillis();

            try (var stdinStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))) {

                var callback = dockerClient.execStartCmd(execId)
                        .withStdIn(stdinStream)
                        .exec(new ResultCallbackTemplate<>() {
                            @Override
                            public void onNext(Frame frame) {
                                try {
                                    if (frame.getStreamType() == StreamType.STDOUT) {
                                        stdout.write(frame.getPayload());
                                    } else if (frame.getStreamType() == StreamType.STDERR) {
                                        stderr.write(frame.getPayload());
                                    }
                                } catch (IOException e) {
                                    log.error("Error reading exec output", e);
                                }
                            }
                        });

                boolean finished = callback.awaitCompletion(timeoutSeconds, TimeUnit.SECONDS);
                long elapsedMs = System.currentTimeMillis() - startTime;

                if (!finished) {
                    log.warn("Exec command timed out after {} seconds", timeoutSeconds);
                    return new ExecResult("", "Timeout", -1, elapsedMs, true);
                }

                var inspectExec = dockerClient.inspectExecCmd(execId).exec();
                int exitCode = inspectExec.getExitCodeLong() != null
                        ? inspectExec.getExitCodeLong().intValue()
                        : 0;

                return new ExecResult(
                        stdout.toString(StandardCharsets.UTF_8),
                        stderr.toString(StandardCharsets.UTF_8),
                        exitCode,
                        elapsedMs,
                        false
                );

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Exec interrupted", e);
            }

        } catch (Exception e) {
            log.error("Failed to exec command with stdin", e);
            throw new RuntimeException("Exec failed", e);
        }
    }

    private String resolveImage(ProgrammingLanguage language) {
        if (language.equals(ProgrammingLanguage.JAVA)) {
            return properties.getImages().getJava();
        } else {
            log.warn("Language {} is not supported", language);
            throw new IllegalStateException("This language is not supported");
        }
    }

    public void startContainer(String containerId) {
        try {
            dockerClient.startContainerCmd(containerId).exec();
            log.debug("Started container: {}", containerId);
        } catch (Exception e) {
            log.error("Failer to start container {}: {}", containerId, e.getMessage());
        }
    }

    public void removeContainer(String containerId) {
        try {
            dockerClient.removeContainerCmd(containerId)
                    .withForce(true)
                    .exec();
            log.debug("Removed container: {}", containerId);
        } catch (Exception e) {
            log.warn("Failed to remove container {}: {}", containerId, e.getMessage());
        }
    }

}
