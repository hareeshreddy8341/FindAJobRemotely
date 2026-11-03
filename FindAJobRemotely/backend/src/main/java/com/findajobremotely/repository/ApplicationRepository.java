package com.findajobremotely.repository;

import com.findajobremotely.model.Application;
import com.findajobremotely.model.ApplicationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ApplicationRepository extends MongoRepository<Application, String> {

    Page<Application> findByCandidateId(String candidateId, Pageable pageable);

    List<Application> findByCandidateId(String candidateId);

    Page<Application> findByJobId(String jobId, Pageable pageable);

    Page<Application> findByJobIdAndStatus(String jobId, ApplicationStatus status, Pageable pageable);

    Optional<Application> findByJobIdAndCandidateId(String jobId, String candidateId);

    boolean existsByEmployerIdAndCandidateId(String employerId, String candidateId);

    long countByJobId(String jobId);

    long countByCandidateId(String candidateId);

    long countByStatus(ApplicationStatus status);

    void deleteByCandidateId(String candidateId);

    void deleteByEmployerId(String employerId);
}
