package com.technokratos.codeexecutorservice.services;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.ConflictException;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import com.technokratos.codeexecutorservice.api.dto.ExecutionResult;
import com.technokratos.codeexecutorservice.configuration.DockerProperties;
import com.technokratos.common.enums.ProgrammingLanguage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DockerManager {

    private final DockerProperties properties;
    private final DockerClient dockerClient;

    private final String JAVA_COMPILE_CMD = "javac";

    private void createImage(String imageName) {
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
                dockerClient.pullImageCmd(imageName)
                        .exec(new PullImageResultCallback())
                        .awaitCompletion(5, TimeUnit.MINUTES);
                log.info("Image {} is downloaded", imageName);
            } else {
                log.info("Image {} is already exists", imageName);
            }
        } catch (Exception e) {
            log.error("Cannot download image", e);
            throw new DockerException("Image creating failure %s".formatted(e.getMessage()), 500);
        }
    }

    public String createContainer(ProgrammingLanguage language, Long memoryLimit, Long cpuCount, String code) {
        String hostDir = properties.getHostDir();

        try {
            Files.createDirectories(Paths.get(hostDir));

            Path codeFile = Paths.get(hostDir, "Main.%s".formatted(language.toString().toLowerCase()));
            Files.write(codeFile, code.getBytes());

            String imageName = resolveImage(language);
            createImage(imageName);

            CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                    .withWorkingDir(properties.getWorkingDir())
                    .withHostConfig(HostConfig.newHostConfig()
                            .withMemory(memoryLimit * 1024L * 1024L)
                            .withCpuCount(cpuCount)
                            .withBinds((
                                    new Bind(
                                            hostDir,
                                            new Volume(properties.getWorkingDir())
                                    ))
                            )
                            .withAutoRemove(true)
                    )
                    .withCmd("sleep", "infinity")
                    .exec();

            String containerId = container.getId();
            log.info("Container with ID={} is created", containerId);

            return containerId;
        } catch (NotFoundException | ConflictException e) {
            log.error("Cannot create container", e);
            throw new DockerException("Container creating failure: %s".formatted(e.getMessage()), 500);
        } catch (IOException e) {
            log.error("Cannot create directory", e);
            throw new DockerException("Directory creating failure: %s".formatted(e.getMessage()), 500);
        }
    }

    public ExecutionResult processConsoleJavaCode(String containerId, ProgrammingLanguage language) {
        try {
            dockerClient.startContainerCmd(containerId).exec();
            log.info("Container ID={} has started", containerId);

            ExecCreateCmdResponse compileCmd = dockerClient.execCreateCmd(containerId)
                    .withCmd("javac", "Main.java")
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();

            log.info("Code has compiled: containerId = {}", containerId);

            String compileResult = execCommand(containerId, compileCmd);
            if (compileResult.toLowerCase().contains("error") ||
                    compileResult.toLowerCase().contains("exception") ||
                    compileResult.toLowerCase().contains("fail")) {
                log.warn("Compilation error: {}", compileResult);

                return ExecutionResult.builder()
                        .containerId(containerId)
                        .isSuccess(false)
                        .output(compileResult)
                        .build();
            }

            ExecCreateCmdResponse runCmd = dockerClient.execCreateCmd(containerId)
                    .withCmd("java", "Main")
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();

            log.info("Program has started: containerId = {}", containerId);

            String output = execCommand(containerId, runCmd);

            return ExecutionResult.builder()
                    .containerId(containerId)
                    .isSuccess(!output.contains("Exception"))
                    .output(output)
                    .build();

        } catch (Exception e) {
            throw new DockerException("Cannot complete command execution", 500);
        } finally {
            cleanUp(containerId);
        }
    }

    private void cleanUp(String containerId) {
        if (containerId != null) {
            dockerClient.killContainerCmd(containerId).exec();
            dockerClient.removeContainerCmd(containerId).exec();

            try {
                Files.walk(Paths.get(properties.getHostDir()))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                log.warn("Cannot walk to directory");
            }
        }
    }

    private String execCommand(String containerId, ExecCreateCmdResponse cmd) throws InterruptedException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        dockerClient.execStartCmd(cmd.getId())
                .exec(new ResultCallback.Adapter<Frame>() {
                    @Override
                    public void onNext(Frame frame) {
                        try {
                            output.write(frame.getPayload());
                        } catch (IOException e) {
                            log.error("Cannot write data", e);
                        }
                    }
                }).awaitCompletion(10, TimeUnit.SECONDS);

        return output.toString().trim();
    }

    private String resolveImage(ProgrammingLanguage language) {
        if (language == ProgrammingLanguage.JAVA) {
            return properties.getJavaImage();
        } else {
            log.warn("Language {} is not supported", language);
            throw new IllegalStateException("Language %s is not supported".formatted(language));
        }
    }

}
