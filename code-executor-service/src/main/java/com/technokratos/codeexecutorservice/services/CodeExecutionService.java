package com.technokratos.codeexecutorservice.services;

import com.technokratos.codeexecutorservice.api.dto.SolutionRequest;
import com.technokratos.codeexecutorservice.api.dto.kafka.KafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeExecutionService {

    public void executeCode(SolutionRequest request) {

    }
}
