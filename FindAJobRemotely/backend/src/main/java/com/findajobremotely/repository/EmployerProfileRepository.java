package com.findajobremotely.repository;

import com.findajobremotely.model.EmployerProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EmployerProfileRepository extends MongoRepository<EmployerProfile, String> {
}
