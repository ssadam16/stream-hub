package com.technokratos.taskservice.repositories;

import com.technokratos.taskservice.entities.tests.ConsoleTestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConsoleTestCaseRepository extends JpaRepository<ConsoleTestCase, UUID> {
}
