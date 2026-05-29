package com.technokratos.codeexecutorservice.services;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DockerManagerUnitTest {

    @Mock(answer = org.mockito.Answers.RETURNS_SELF)
    private ExecCreateCmd execCreateCmd;

    @Mock(answer = org.mockito.Answers.RETURNS_SELF)
    private ExecStartCmd execStartCmd;

    @Mock
    private DockerClient dockerClient;

    @Mock
    private ExecCreateCmdResponse execCreateResponse;

    @Mock
    private InspectExecCmd inspectExecCmd;

    @Mock
    private InspectExecResponse inspectExecResponse;

    private DockerManager dockerManager;

    private static final String CONTAINER_ID = "test-container-id";
    private static final String EXEC_ID = "test-exec-id";

    @BeforeEach
    void setUp() {
        dockerManager = new DockerManager(dockerClient);

        when(dockerClient.execCreateCmd(CONTAINER_ID)).thenReturn(execCreateCmd);
        when(execCreateCmd.exec()).thenReturn(execCreateResponse);
        when(execCreateResponse.getId()).thenReturn(EXEC_ID);

        when(dockerClient.execStartCmd(EXEC_ID)).thenReturn(execStartCmd);
        when(execStartCmd.withStdIn(any(InputStream.class))).thenReturn(execStartCmd);
    }

    @Test
    void execWithStdin_attachesStdinStream() throws Exception {
        stubSuccessfulExec(0, "out", "");

        dockerManager.execWithStdin(CONTAINER_ID, "hello", 5000, "cat");

        verify(execCreateCmd).withAttachStdin(true);
        verify(execStartCmd).withStdIn(any(InputStream.class));
    }

    @Test
    void execWithStdin_passesCommandToExecCreate() throws Exception {
        stubSuccessfulExec(0, "", "");

        dockerManager.execWithStdin(CONTAINER_ID, "x", 5000, "python3", "main.py");

        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(execCreateCmd).withCmd(captor.capture());
        assertThat(captor.getValue()).containsExactly("python3", "main.py");
    }

    @Test
    void execWithStdin_returnsStdoutStderrAndExitCode() throws Exception {
        stubSuccessfulExec(42, "the-output", "the-error");

        var result = dockerManager.execWithStdin(CONTAINER_ID, "in", 5000, "cat");

        assertThat(result.stdout()).isEqualTo("the-output");
        assertThat(result.stderr()).isEqualTo("the-error");
        assertThat(result.exitCode()).isEqualTo(42);
        assertThat(result.timedOut()).isFalse();
    }

    @Test
    void execWithStdin_nullInput_doesNotThrow() throws Exception {
        stubSuccessfulExec(0, "", "");

        var result = dockerManager.execWithStdin(CONTAINER_ID, null, 5000, "cat");

        assertThat(result.exitCode()).isZero();
        verify(execStartCmd).withStdIn(any(InputStream.class));
    }

    @Test
    void execWithStdin_timeout_returnsTimedOutResult() {
        when(execStartCmd.exec(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(dockerClient.inspectExecCmd(EXEC_ID)).thenReturn(inspectExecCmd);
        when(inspectExecCmd.exec()).thenReturn(inspectExecResponse);
        when(inspectExecResponse.getPidLong()).thenReturn(null);

        var result = dockerManager.execWithStdin(CONTAINER_ID, "in", 100, "sleep", "10");

        assertThat(result.timedOut()).isTrue();
        assertThat(result.exitCode()).isEqualTo(-1);
        assertThat(result.stderr()).isEqualTo("Timeout");
    }

    @Test
    @SuppressWarnings("unchecked")
    void execWithStdin_wrapsExceptionInRuntime() {
        when(execStartCmd.exec(any())).thenThrow(new RuntimeException("boom"));

        assertThrows(RuntimeException.class,
                () -> dockerManager.execWithStdin(CONTAINER_ID, "in", 5000, "cat"));
    }


    @SuppressWarnings("unchecked")
    private void stubSuccessfulExec(int exitCode, String stdout, String stderr) {
        when(execStartCmd.exec(any())).thenAnswer(invocation -> {
            ResultCallback.Adapter<Frame> cb = invocation.getArgument(0);
            if (!stdout.isEmpty()) {
                cb.onNext(new Frame(StreamType.STDOUT, stdout.getBytes(StandardCharsets.UTF_8)));
            }
            if (!stderr.isEmpty()) {
                cb.onNext(new Frame(StreamType.STDERR, stderr.getBytes(StandardCharsets.UTF_8)));
            }
            cb.onComplete();
            return cb;
        });

        when(dockerClient.inspectExecCmd(EXEC_ID)).thenReturn(inspectExecCmd);
        when(inspectExecCmd.exec()).thenReturn(inspectExecResponse);
        when(inspectExecResponse.getExitCodeLong()).thenReturn((long) exitCode);
    }
}