package com.findajobremotely.repository;

import com.findajobremotely.model.ResumeFile;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ResumeFileRepository extends MongoRepository<ResumeFile, String> {
}
