package com.technokratos.taskservice.repositories;

import com.technokratos.taskservice.entities.tests.ConsoleTestCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConsoleTestCaseRepository extends JpaRepository<ConsoleTestCase, UUID> {
}
