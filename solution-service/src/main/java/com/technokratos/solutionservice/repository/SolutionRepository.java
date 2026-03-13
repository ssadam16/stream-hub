package com.technokratos.solutionservice.repository;

import com.technokratos.solutionservice.model.SolutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SolutionRepository extends JpaRepository<SolutionEntity, UUID> {
}