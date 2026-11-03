package com.findajobremotely.repository;

import com.findajobremotely.model.Job;
import com.findajobremotely.model.JobStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface JobRepository extends MongoRepository<Job, String> {

    List<Job> findByEmployerIdOrderByCreatedAtDesc(String employerId);

    List<Job> findByStatus(JobStatus status, Pageable pageable);

    long countByStatus(JobStatus status);

    void deleteByEmployerId(String employerId);
}
