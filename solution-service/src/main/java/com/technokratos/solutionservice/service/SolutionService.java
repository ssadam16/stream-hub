package com.technokratos.solutionservice.service;

import com.technokratos.common.dto.TaskResponse;
import com.technokratos.common.enums.SolutionStatus;
import com.technokratos.common.events.CodeExecutionRequest;
import com.technokratos.common.events.CodeExecutionResult;
import com.technokratos.common.events.SolutionCompletedEvent;
import com.technokratos.solutionservice.client.TaskClient;
import com.technokratos.solutionservice.dto.SolutionRequest;
import com.technokratos.solutionservice.dto.SolutionResponse;
import com.technokratos.solutionservice.dto.SolutionResultResponse;
import com.technokratos.common.exceptions.SolutionNotFound;
import com.technokratos.solutionservice.mapper.SolutionMapper;
import com.technokratos.solutionservice.model.SolutionEntity;
import com.technokratos.solutionservice.model.SolutionSource;
import com.technokratos.solutionservice.repository.MongoSolutionRepository;
import com.technokratos.solutionservice.repository.SolutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class SolutionService {

    private final SolutionMapper mapper;
    private final SolutionRepository solutionRepository;
    private final KafkaTemplate<UUID, CodeExecutionRequest> kafkaTemplate;
    private final KafkaTemplate<UUID, SolutionCompletedEvent> kafkaPlagiarismTemplate;
    private final static String EXECUTION_CODE_KAFKA_TOPIC = "code-execution-topic";
    private final static String PLAGIARISM_KAFKA_TOPIC = "solution-completed-events";
    private final TaskClient taskClient;
    private final MongoSolutionRepository mongoSolutionRepository;

    @Transactional
    public UUID solveTask(SolutionRequest dto, UUID taskId, UUID userId) {
        log.info("User {} has been solved task {}", userId, taskId);
        SolutionEntity solution = mapper.toEntity(dto);
        solution.setUserId(userId);
        solution.setStatus(SolutionStatus.PENDING);
        solution.setTaskId(taskId);

        SolutionEntity saved = solutionRepository.save(solution);
        SolutionSource solutionToMongo = SolutionSource.builder()
                .code(dto.code())
                .solutionId(saved.getId())
                .taskId(taskId)
                .language(dto.language())
                .normalizedCode(normalize(dto.code()))
                .codeHash(sha256(dto.code()))
                .build();

        mongoSolutionRepository.save(solutionToMongo);

        log.debug("Solution saved: taskId={}, solutionId={}", saved.getTaskId(), saved.getId());

        UUID solutionId = saved.getId();

        TaskResponse task = taskClient.getTask(taskId);
        log.info("Task from task-service: {}", task.id());
        CodeExecutionRequest request = getCodeExecutionRequest(dto, userId , taskId, solutionId, task);
        kafkaTemplate.send(EXECUTION_CODE_KAFKA_TOPIC, saved.getId(), request);
        log.info("Send code-execution event to {} topic", EXECUTION_CODE_KAFKA_TOPIC);

        return solutionId;
    }

    private String sha256(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String normalize(String code) {
        return code
                .replaceAll("//[^\n]*", "")           // убрать // комменты
                .replaceAll("/\\*.*?\\*/", "")         // убрать /* */ комменты
                .replaceAll("#[^\n]*", "")             // убрать # комменты (Python/C++)
                .replaceAll("\\s+", " ")               // нормализовать пробелы
                .trim();
    }

    public SolutionResultResponse getResult(UUID solutionId) {
        SolutionEntity solution = solutionRepository.findById(solutionId)
                .orElseThrow(() -> new SolutionNotFound(solutionId));

        return mapper.toResponse(solution);
    }

    private CodeExecutionRequest getCodeExecutionRequest(SolutionRequest dto, UUID userId, UUID taskId, UUID solutionId, TaskResponse task) {
        return CodeExecutionRequest.builder()
                .taskId(taskId)
                .solutionId(solutionId)
                .language(dto.language())
                .code(dto.code())
                .userId(userId)
                .memoryLimit(task.memoryLimit())
                .testCases(task.testCases())
                .timeLimit(task.timeLimit())
                .build();
    }

    @KafkaListener(topics = "code-execution-results", groupId = "${spring.kafka.consumer.group-id}")
    public void onExecutionResult(CodeExecutionResult result) {
        UUID solutionId = result.solutionId();

        SolutionEntity solution = solutionRepository.findById(solutionId)
                .orElseThrow(() -> new SolutionNotFound(solutionId));

        solution.setStatus(result.status());
        solution.setTotalTests(result.totalTests());
        solution.setPassedTests(result.passedTests());
        solution.setTotalTimeMs(result.totalTimeMs());
        solution.setTestResults(result.testResults());

        solutionRepository.save(solution);
        log.info("Solution {} updated with status: {}", result.solutionId(), result.status());

        if (result.status() == SolutionStatus.COMPLETED) {
            kafkaPlagiarismTemplate.send(
                    PLAGIARISM_KAFKA_TOPIC,
                    result.solutionId(),
                    SolutionCompletedEvent.builder()
                            .solutionId(result.solutionId())
                            .taskId(solution.getTaskId())
                            .language(solution.getLanguage())
                            .build()
            );
            log.info("Sent solution {} to plagiarism check", result.solutionId());
        }
    }

    public Page<SolutionResponse> getAll(Pageable pageable) {
        return mapper.toResponsePage(solutionRepository.findAll(pageable));
    }

    public SolutionResponse getOne(UUID id) {
        SolutionEntity solution = solutionRepository.findById(id)
                .orElseThrow(() -> new SolutionNotFound(id));

        return mapper.toSolutionResponse(solution);
    }

    public void deleteOne(UUID id) {
        solutionRepository.deleteById(id);
    }
}
