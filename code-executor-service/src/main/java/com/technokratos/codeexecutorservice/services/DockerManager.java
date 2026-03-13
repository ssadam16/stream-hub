package com.technokratos.codeexecutorservice.services;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Image;
import com.technokratos.codeexecutorservice.configuration.DockerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DockerManager {

    private final DockerProperties properties;
    private final DockerClient dockerClient;

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
            //todo: че нибудь сделать тут
        }
    }

    public String createContainer(String language, String workingDirName, Long memoryLimit, Long cpuCount) {
        String imageName = resolveImage(language);
        createImage(imageName);

        CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                .withWorkingDir("/%s".formatted(workingDirName))
                .withHostConfig(HostConfig.newHostConfig()
                        .withMemory(memoryLimit * 1024L * 1024L)
                        .withCpuCount(cpuCount)
                ).exec();

        String containerId = container.getId();
        log.info("Container with ID={} is created", containerId);

        return containerId;
    }

    private String resolveImage(String language) {
        if (language.equals("java")) {
            return properties.getJavaImage();
        } else {
            log.warn("Language {} is not supported", language);
            throw new IllegalStateException("This language is not supported");
        }
    }

}
