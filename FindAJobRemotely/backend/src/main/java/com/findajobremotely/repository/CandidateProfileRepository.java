package com.findajobremotely.repository;

import com.findajobremotely.model.CandidateProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CandidateProfileRepository extends MongoRepository<CandidateProfile, String> {

    Page<CandidateProfile> findByPublicProfileTrueAndAccountActiveTrue(Pageable pageable);
}
