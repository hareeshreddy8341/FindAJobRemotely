package com.findajobremotely.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** A showcase post (project, achievement, availability, article) a candidate publishes for employers. */
@Document("candidate_posts")
@CompoundIndex(name = "status_created", def = "{'status': 1, 'createdAt': -1}")
public class CandidatePost {

    @Id
    public String id;

    @Indexed
    public String authorId;

    public PostType type;
    public String title;
    public String content;
    public List<String> tags = new ArrayList<>();
    public PostStatus status = PostStatus.VISIBLE;

    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();
}
