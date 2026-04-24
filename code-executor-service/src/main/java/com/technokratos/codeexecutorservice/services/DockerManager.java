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
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
                    throw new RuntimeException("Failed to pull image");
                }
            } else {
                log.info("Image {} already exists", imageName);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while pulling image", e);
        } catch (Exception e) {
            log.error("Cannot download image", e);
            throw new RuntimeException("Failed to ensure image exists", e);
        }
    }

    public String createContainer(ProgrammingLanguage language, LanguageConfig config) {
        String imageName = resolveImage(language);
        ensureImageExists(imageName);

        String containerWorkDir = config.getWorkingDir();

        long memorySwap = config.getMemoryMB() * 1024L * 1024L;

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withMemory(memorySwap)
                .withCpuCount(config.getCpuCount())
                .withPidsLimit(64L)
                .withMemorySwap(memorySwap)
                .withNetworkMode("none")
                .withMemorySwap(0L);

        CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                .withHostConfig(hostConfig)
                .withWorkingDir(containerWorkDir)
                .withCmd("tail", "-f", "/dev/null")
                .withAttachStdout(true)
                .withAttachStderr(true)
                .exec();

        log.info("Created container {} for language {}", container.getId(), language);

        return container.getId();
    }

    public void copyFileToContainer(String containerId, String fileName, String code, String remotePath) {
        try {
            byte[] fileBytes = code.getBytes(StandardCharsets.UTF_8);

            ByteArrayOutputStream tarStream = new ByteArrayOutputStream();
            try (TarArchiveOutputStream tar = new TarArchiveOutputStream(tarStream)) {
                tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

                TarArchiveEntry entry = new TarArchiveEntry(fileName);
                entry.setSize(fileBytes.length);
                entry.setMode(420); // rw-r--r--

                tar.putArchiveEntry(entry);
                tar.write(fileBytes);
                tar.closeArchiveEntry();
            }

            dockerClient.copyArchiveToContainerCmd(containerId)
                    .withTarInputStream(new ByteArrayInputStream(tarStream.toByteArray()))
                    .withRemotePath(remotePath)
                    .exec();

            log.debug("Copied file {} to container {} at {}", fileName, containerId, remotePath);

        } catch (IOException e) {
            log.error("Failed to copy file to container", e);
            throw new RuntimeException("Failed to copy file to container", e);
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

            return new ExecResult(output, error, exitCode, elapsedMs, false);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Exec interrupted", e);
        } catch (Exception e) {
            log.error("Failed to exec command", e);
            throw new RuntimeException("Exec failed", e);
        }
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
        return switch (language) {
            case JAVA -> properties.getImages().getJava();
            case PYTHON -> properties.getImages().getPython();
        };
    }

    public void startContainer(String containerId) {
        try {
            dockerClient.startContainerCmd(containerId).exec();
            log.debug("Started container: {}", containerId);
        } catch (Exception e) {
            log.error("Failed to start container {}: {}", containerId, e.getMessage());
            throw new RuntimeException("Failed to start container", e);
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