package com.technokratos.codeexecutorservice.services;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.*;
import com.technokratos.codeexecutorservice.configuration.DockerProperties;
import com.technokratos.codeexecutorservice.configuration.LanguageConfig;
import com.technokratos.common.enums.ProgrammingLanguage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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


    public String execCommand(String containerId, String... command) {
        try {
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();

            var execCmd = dockerClient.execCreateCmd(containerId)
                    .withCmd(command)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();

            try (var callback = dockerClient.execStartCmd(execCmd.getId())
                    .exec(new ResultCallbackTemplate<>() {
                        @Override
                        public void onNext(Frame frame) {
                            try {
                                if (frame.getStreamType() == StreamType.STDOUT) {
                                    stdout.write(frame.getPayload());
                                } else {
                                    stderr.write(frame.getPayload());
                                }
                            } catch (IOException e) {
                                log.error("Error reading exec output", e);
                            }
                        }
                    })) {
                callback.awaitCompletion();
            }

            String output = stdout.toString();
            String error = stderr.toString();

            if (!error.isEmpty()) {
                log.debug("Exec stderr: {}", error);
            }

            return output;

        } catch (Exception e) {
            log.error("Failed to exec command in container", e);
            return "Error: " + e.getMessage();
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
                .withCmd(config.getCommand())
                .withAttachStdout(true)
                .withAttachStderr(true)
                .exec();

        log.info("Created container {} for language {} with command: {}",
                container.getId(), language, String.join(" ", config.getCommand()));

        return container.getId();
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

    public String waitForContainer(String containerId, int timeoutSeconds) {
        try {
            log.debug(" Debug: checking files in container...");

            String lsOutput = execCommand(containerId, "ls", "-la", "/workspace");
            log.debug("Files in /workspace:\n{}", lsOutput);

            String pwdOutput = execCommand(containerId, "pwd");
            log.debug("Current working directory: {}", pwdOutput);

            boolean finished = dockerClient.waitContainerCmd(containerId)
                    .exec(new WaitContainerResultCallback())
                    .awaitCompletion(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                log.warn("Container {} did not finish within {} seconds, killing...", containerId, timeoutSeconds);
                dockerClient.killContainerCmd(containerId).exec();
                return "Execution timeout after " + timeoutSeconds + " seconds";
            }

            return getContainerLogs(containerId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for container", e);
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


    private String getContainerLogs(String containerId) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .exec(new ResultCallbackTemplate<>() {
                        @Override
                        public void onNext(Frame object) {
                            try {
                                byteArrayOutputStream.write(object.getPayload());
                            } catch (IOException e) {
                                log.error("Error writing log", e);
                            }
                        }
                    })
                    .awaitCompletion();
            return byteArrayOutputStream.toString();
        } catch (IOException | InterruptedException e) {
            log.error("Failed to get container logs", e);
            return "Failed to get execution logs";
        }
    }
}