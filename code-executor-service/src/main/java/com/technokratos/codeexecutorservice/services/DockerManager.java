package com.technokratos.codeexecutorservice.services;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.*;
import com.technokratos.codeexecutorservice.configuration.LanguageConfig;
import com.technokratos.codeexecutorservice.executors.LanguageExecutor;
import com.technokratos.common.dto.ExecResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DockerManager {

    private final DockerClient dockerClient;

    @PostConstruct
    public void cleanupStaleContainers() {
        log.info("Cleaning up stale containers...");

        dockerClient.listContainersCmd()
                .withLabelFilter(Map.of("stream-hub.executor", "true"))
                .exec()
                .forEach(c -> {
                    dockerClient.removeContainerCmd(c.getId()).withForce(true).exec();
                    log.debug("Removed stale container: {}", c.getId().substring(0, 12));
                });
    }

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

    public String createContainer(LanguageExecutor executor, long memoryMb) {
        ensureImageExists(executor.getImage());

        LanguageConfig config = executor.getConfig();
        String containerWorkDir = config.getWorkingDir();

        long memoryBytes = memoryMb  * 1024L * 1024L;

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withMemory(memoryBytes)
                .withCpuCount(config.getCpuCount())
                .withPidsLimit(config.getPidsLimit() != null ? config.getPidsLimit() : 64L)
                .withMemorySwap(memoryBytes)
                .withCapDrop(Capability.ALL)
                .withReadonlyRootfs(true)
                .withTmpFs(Map.of(
                        "/tmp",           "rw,nosuid,size=64m,exec",
                        "/workspace",     "rw,nosuid,size=32m,exec",
                        "/root/.cache",   "rw,nosuid,size=128m,exec"
                ))
                .withSecurityOpts(List.of("no-new-privileges:true"))
                .withNetworkMode("none");

        CreateContainerResponse container = dockerClient.createContainerCmd(executor.getImage())
                .withHostConfig(hostConfig)
                .withWorkingDir(containerWorkDir)
                .withCmd("tail", "-f", "/dev/null")
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withLabels(Map.of(
                        "stream-hub.executor", "true",
                        "created_by", "code-executor-service"
                ))
                .exec();

        log.info("Created container {} for language {}", container.getId(), executor.getLanguage());

        return container.getId();
    }

    public void writeFileViaExec(String containerId, String fileName, String code) {
        String encoded = Base64.getEncoder().encodeToString(code.getBytes(StandardCharsets.UTF_8));

        ExecResult result = execCommand(
                containerId,
                "sh", "-c",
                "echo '%s' | base64 -d > /workspace/%s".formatted(encoded, fileName)
        );

        if (result.exitCode() != 0) {
            throw new RuntimeException("Failed to write source file: " + result.stderr());
        }

        log.debug("Wrote file {} to container {} via exec", fileName, containerId);
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

    public ExecResult execWithStdin(String containerId, String input, int timeoutMs, String... command) {
        byte[] inputBytes = input != null
                ? input.getBytes(StandardCharsets.UTF_8)
                : new byte[0];

        String stdinPath = "/tmp/stdin-" + java.util.UUID.randomUUID();

        String encoded = Base64.getEncoder().encodeToString(inputBytes);
        ExecResult writeResult = execCommand(
                containerId,
                "sh", "-c",
                "echo %s | base64 -d > %s".formatted(encoded, stdinPath)
        );
        if (writeResult.exitCode() != 0) {
            throw new RuntimeException("Failed to write stdin file: " + writeResult.stderr());
        }

        String joinedCommand = buildShellCommand(command);
        String fullCommand = "%s < %s".formatted(joinedCommand, stdinPath);

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        var execCmd = dockerClient.execCreateCmd(containerId)
                .withCmd("sh", "-c", fullCommand)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .exec();

        long startTime = System.currentTimeMillis();

        try (ResultCallback.Adapter<Frame> callback = dockerClient.execStartCmd(execCmd.getId())
                .exec(new ResultCallback.Adapter<Frame>() {
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

            boolean finished = callback.awaitCompletion(timeoutMs, TimeUnit.MILLISECONDS);
            long elapsedMs = System.currentTimeMillis() - startTime;

            if (!finished) {
                killExecProcess(containerId, execCmd.getId());
                cleanupStdinFile(containerId, stdinPath);
                return new ExecResult(
                        stdout.toString(StandardCharsets.UTF_8),
                        "Timeout",
                        -1,
                        elapsedMs,
                        true
                );
            }

            var inspect = dockerClient.inspectExecCmd(execCmd.getId()).exec();
            int exitCode = inspect.getExitCodeLong() != null
                    ? inspect.getExitCodeLong().intValue()
                    : 0;

            cleanupStdinFile(containerId, stdinPath);

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
        } catch (Exception e) {
            log.error("Failed to exec command with stdin", e);
            throw new RuntimeException("Exec failed", e);
        }
    }

    private String buildShellCommand(String... command) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < command.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append('\'')
                    .append(command[i].replace("'", "'\\''"))
                    .append('\'');
        }
        return sb.toString();
    }

    private void cleanupStdinFile(String containerId, String stdinPath) {
        try {
            execCommand(containerId, "sh", "-c", "rm -f " + stdinPath);
        } catch (Exception e) {
            log.warn("Failed to clean up stdin file {} in container {}", stdinPath, containerId, e);
        }
    }

    private void killExecProcess(String containerId, String execId) {
        try {
            var inspect = dockerClient.inspectExecCmd(execId).exec();
            Long pid = inspect.getPidLong();
            if (pid != null && pid > 0) {
                execCommand(containerId, "sh", "-c", "kill -9 " + pid + " 2>/dev/null || true");
            }
        } catch (Exception e) {
            log.warn("Failed to kill exec process {} in container {}", execId, containerId, e);
        }
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