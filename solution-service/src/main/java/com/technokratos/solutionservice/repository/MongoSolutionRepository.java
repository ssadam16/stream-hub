package com.technokratos.solutionservice.repository;

import com.technokratos.solutionservice.model.SolutionSource;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoSolutionRepository extends MongoRepository<SolutionSource, String> {

}
