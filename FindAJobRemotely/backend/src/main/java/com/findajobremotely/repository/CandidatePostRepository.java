package com.findajobremotely.repository;

import com.findajobremotely.model.CandidatePost;
import com.findajobremotely.model.PostStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CandidatePostRepository extends MongoRepository<CandidatePost, String> {

    List<CandidatePost> findByAuthorIdOrderByCreatedAtDesc(String authorId);

    List<CandidatePost> findByAuthorIdAndStatus(String authorId, PostStatus status, Pageable pageable);

    Optional<CandidatePost> findByIdAndAuthorId(String id, String authorId);

    long countByAuthorId(String authorId);

    void deleteByAuthorId(String authorId);
}
