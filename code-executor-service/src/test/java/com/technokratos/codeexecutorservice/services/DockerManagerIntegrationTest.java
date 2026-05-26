package com.technokratos.codeexecutorservice.services;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.*;
import org.junit.jupiter.api.*;
import org.testcontainers.DockerClientFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DockerManagerIntegrationTest {

    private DockerClient dockerClient;
    private DockerManager dockerManager;
    private String containerId;

    private static final String IMAGE = "alpine:3.20";

    @BeforeAll
    void setUp() throws InterruptedException {
        dockerClient = DockerClientFactory.instance().client();
        dockerManager = new DockerManager(dockerClient);

        dockerClient.pullImageCmd("alpine")
                .withTag("3.20")
                .exec(new com.github.dockerjava.api.command.PullImageResultCallback())
                .awaitCompletion();

        var container = dockerClient.createContainerCmd(IMAGE)
                .withHostConfig(HostConfig.newHostConfig().withNetworkMode("none"))
                .withCmd("tail", "-f", "/dev/null")
                .withLabels(Map.of("stream-hub.executor", "true"))
                .exec();
        containerId = container.getId();
        dockerClient.startContainerCmd(containerId).exec();
    }

    @AfterAll
    void tearDown() {
        if (containerId != null) {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        }
    }

    @Test
    @Timeout(30)
    void multilineInput_passesThroughVerbatim() {
        String input = "line one\nline two\nline three\n";

        var result = dockerManager.execWithStdin(containerId, input, 10_000, "wc", "-l");

        assertThat(result.timedOut()).isFalse();
        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout().trim()).isEqualTo("3");
    }

    @Test
    @Timeout(30)
    void specialCharsInput_roundTripsExactly() {
        String input = "He said \"hi\" $HOME `whoami` 100% \\n \\\\ end\n";

        var result = dockerManager.execWithStdin(containerId, input, 10_000, "base64");

        assertThat(result.timedOut()).isFalse();
        assertThat(result.exitCode()).isZero();

        String decoded = new String(
                java.util.Base64.getMimeDecoder().decode(result.stdout().trim()),
                java.nio.charset.StandardCharsets.UTF_8
        );
        assertThat(decoded).isEqualTo(input);
    }

    @Test
    @Timeout(30)
    void cyrillicAndUnicode_roundTripsExactly() {
        String input = "Привет, мир! \nстрока два\n";

        var result = dockerManager.execWithStdin(containerId, input, 10_000, "base64");

        String decoded = new String(
                java.util.Base64.getMimeDecoder().decode(result.stdout().trim()),
                java.nio.charset.StandardCharsets.UTF_8
        );
        assertThat(decoded).isEqualTo(input);
    }

    @Test
    @Timeout(30)
    void emptyInput_doesNotHang() {
        var result = dockerManager.execWithStdin(containerId, "", 10_000, "wc", "-c");

        assertThat(result.timedOut()).isFalse();
        assertThat(result.stdout().trim()).isEqualTo("0");
    }

    @Test
    @Timeout(30)
    void timeout_isReportedForLongRunningCommand() {
        var result = dockerManager.execWithStdin(containerId, "", 500, "sleep", "10");

        assertThat(result.timedOut()).isTrue();
        assertThat(result.exitCode()).isEqualTo(-1);
    }
}