package com.technokratos.solutionservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.technokratos.common.dto.TaskResponse;
import com.technokratos.common.enums.SolutionStatus;
import com.technokratos.common.events.CodeExecutionRequest;
import com.technokratos.common.events.CodeExecutionResult;
import com.technokratos.solutionservice.client.TaskClient;
import com.technokratos.solutionservice.dto.SolutionRequest;
import com.technokratos.solutionservice.dto.SolutionResponse;
import com.technokratos.solutionservice.dto.SolutionResultResponse;
import com.technokratos.solutionservice.handler.exceptions.SolutionNotFound;
import com.technokratos.solutionservice.mapper.SolutionMapper;
import com.technokratos.solutionservice.model.SolutionEntity;
import com.technokratos.solutionservice.repository.SolutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class SolutionService {

    private final SolutionMapper mapper;

    private final SolutionRepository solutionRepository;

    private final KafkaTemplate<UUID, CodeExecutionRequest> kafkaTemplate;

    private final static String EXECUTION_CODE_KAFKA_TOPIC = "code-execution-topic";
    private final TaskClient taskClient;

    @Transactional
    public UUID solveTask(SolutionRequest dto, UUID taskId) {
        UUID userId = UUID.randomUUID();
        SolutionEntity solution = mapper.toEntity(dto);
        solution.setUserId(userId);//добавить userId
        solution.setStatus(SolutionStatus.PENDING);
        solution.setTaskId(taskId);

        SolutionEntity savedSolution = solutionRepository.save(solution);

        log.debug("Solution saved entity: taskId={}, solutionId={}",
                savedSolution.getTaskId(),
                savedSolution.getId()
        );

        UUID solutionId = savedSolution.getId();

        TaskResponse task = taskClient.getTask(taskId);

        CodeExecutionRequest codeExecutionRequest = getCodeExecutionRequest(dto,userId , taskId, savedSolution, solutionId, task);

        kafkaTemplate.send(EXECUTION_CODE_KAFKA_TOPIC, solutionId, codeExecutionRequest);

        return solutionId;
    }

    public SolutionResultResponse getResult(UUID solutionId) {
        SolutionEntity solution = solutionRepository.findById(solutionId)
                .orElseThrow(() -> new SolutionNotFound(solutionId));

        return mapper.toResponse(solution);
    }

    private CodeExecutionRequest getCodeExecutionRequest(SolutionRequest dto, UUID userId, UUID taskId, SolutionEntity savedSolution, UUID solutionId, TaskResponse task) {
        return CodeExecutionRequest.builder()
                .code(savedSolution.getCode())
                .taskId(taskId)
                .solutionId(solutionId)
                .language(dto.language())
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
