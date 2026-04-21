package com.technokratos.solutionservice.service;

import com.technokratos.common.dto.CurrentUserInfo;
import com.technokratos.common.enums.SolutionStatus;
import com.technokratos.common.events.SolutionCreationEvent;
import com.technokratos.solutionservice.dto.SolutionRequest;
import com.technokratos.solutionservice.dto.SolutionResponse;
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

    private final KafkaTemplate<UUID, SolutionCreationEvent> kafkaTemplate;

    private final static String EXECUTION_CODE_KAFKA_TOPIC = "code-execution-topic";

    @Transactional
    public UUID solveTask(SolutionRequest dto, CurrentUserInfo user, UUID taskId) {
        SolutionEntity solution = mapper.toEntity(dto);
        solution.setUserId(user.id());
        solution.setStatus(SolutionStatus.PENDING);
        solution.setTaskId(taskId);

        SolutionEntity savedSolution = solutionRepository.save(solution);

        log.debug("Solution saved entity: taskId={}, solutionId={}", savedSolution.getTaskId(), savedSolution.getId());

        UUID solutionId = savedSolution.getId();
        kafkaTemplate.send(EXECUTION_CODE_KAFKA_TOPIC, solutionId, mapper.toSolutionCreationEvent(savedSolution));
        return solutionId;
    }

    public Page<SolutionResponse> getAll(Pageable pageable) {
        return mapper.toResponsePage(solutionRepository.findAll(pageable));
    }

    public SolutionResponse getOne(UUID id) {
        SolutionEntity solution = solutionRepository.findById(id).orElseThrow(() -> new SolutionNotFound(id));

        return mapper.toSolutionResponse(solution);
    }

    public void deleteOne(UUID id) {
        solutionRepository.deleteById(id);
    }
}
