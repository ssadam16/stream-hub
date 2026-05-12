package com.technokratos.taskservice.repository;

import com.technokratos.taskservice.model.tests.ConsoleTestCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsoleTestCaseRepository extends JpaRepository<ConsoleTestCase, UUID> {
    List<ConsoleTestCase> getAllByTask_Id(UUID taskId);
}
