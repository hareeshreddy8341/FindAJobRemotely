package com.findajobremotely.repository;

import com.findajobremotely.model.Commission;
import com.findajobremotely.model.CommissionStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommissionRepository extends MongoRepository<Commission, String> {

    Optional<Commission> findByApplicationId(String applicationId);

    List<Commission> findByApplicationIdIn(Collection<String> applicationIds);

    List<Commission> findByCandidateIdOrderByCreatedAtDesc(String candidateId);

    Page<Commission> findByStatus(CommissionStatus status, Pageable pageable);

    List<Commission> findByStatus(CommissionStatus status);
}
