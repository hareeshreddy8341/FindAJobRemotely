package com.findajobremotely.service;

import com.findajobremotely.core.Skills;
import com.findajobremotely.core.TextClean;
import com.findajobremotely.dto.CommonDtos.PageResponse;
import com.findajobremotely.dto.PostDtos.PostRequest;
import com.findajobremotely.dto.PostDtos.PostView;
import com.findajobremotely.exception.ApiException;
import com.findajobremotely.model.CandidatePost;
import com.findajobremotely.model.CandidateProfile;
import com.findajobremotely.model.PostStatus;
import com.findajobremotely.repository.CandidatePostRepository;
import com.findajobremotely.security.AuthPrincipal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class PostService {

    private static final int MAX_POSTS_PER_CANDIDATE = 50;

    private final CandidatePostRepository posts;
    private final MongoTemplate template;
    private final ViewMapper mapper;
    private final AuditService audit;

    public PostService(CandidatePostRepository posts, MongoTemplate template, ViewMapper mapper,
                       AuditService audit) {
        this.posts = posts;
        this.template = template;
        this.mapper = mapper;
        this.audit = audit;
    }

    // ---------------- candidate ----------------

    public List<PostView> mine(String authorId) {
        return mapper.posts(posts.findByAuthorIdOrderByCreatedAtDesc(authorId));
    }

    public PostView create(String authorId, PostRequest req) {
        if (posts.countByAuthorId(authorId) >= MAX_POSTS_PER_CANDIDATE) {
            throw ApiException.conflict("You have reached the limit of " + MAX_POSTS_PER_CANDIDATE + " posts");
        }
        CandidatePost p = new CandidatePost();
        p.authorId = authorId;
        fill(p, req);
        posts.save(p);
        return mapper.posts(List.of(p)).get(0);
    }

    public PostView update(String authorId, String postId, PostRequest req) {
        CandidatePost p = posts.findByIdAndAuthorId(postId, authorId)
                .orElseThrow(() -> ApiException.notFound("Post not found"));
        if (p.status == PostStatus.REMOVED) {
            throw ApiException.forbidden("This post was removed by an administrator");
        }
        fill(p, req);
        p.updatedAt = Instant.now();
        posts.save(p);
        return mapper.posts(List.of(p)).get(0);
    }

    public void delete(String authorId, String postId) {
        CandidatePost p = posts.findByIdAndAuthorId(postId, authorId)
                .orElseThrow(() -> ApiException.notFound("Post not found"));
        posts.deleteById(p.id);
    }

    // ---------------- employer / public feed ----------------

    /** Visible posts from active, public candidates only. */
    public PageResponse<PostView> feed(String q, int page, int size) {
        Pageable pageable = Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Criteria> all = new ArrayList<>();
        all.add(Criteria.where("status").is(PostStatus.VISIBLE));

        // Hide posts whose author turned their profile private or was suspended
        List<String> authorIds = template.find(
                        Query.query(Criteria.where("publicProfile").is(true).and("accountActive").is(true)),
                        CandidateProfile.class)
                .stream().map(cp -> cp.userId).toList();
        all.add(Criteria.where("authorId").in(authorIds));

        String cleanQ = TextClean.line(q);
        if (cleanQ != null && !cleanQ.isBlank()) {
            String rx = TextClean.escapeRegex(cleanQ.length() > 80 ? cleanQ.substring(0, 80) : cleanQ);
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(rx, java.util.regex.Pattern.CASE_INSENSITIVE);
            all.add(new Criteria().orOperator(
                    Criteria.where("title").regex(pattern),
                    Criteria.where("content").regex(pattern),
                    Criteria.where("tags").regex(pattern)));
        }
        Query query = new Query(new Criteria().andOperator(all.toArray(new Criteria[0])));
        long total = template.count(query, CandidatePost.class);
        List<CandidatePost> found = template.find(query.with(pageable), CandidatePost.class);
        Page<PostView> p = new PageImpl<>(mapper.posts(found), pageable, total);
        return PageResponse.of(p);
    }

    // ---------------- admin ----------------

    public PageResponse<PostView> adminList(int page, int size) {
        Pageable pageable = Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CandidatePost> p = posts.findAll(pageable);
        return new PageResponse<>(mapper.posts(p.getContent()), p.getNumber(), p.getSize(), p.getTotalElements(),
                p.getTotalPages());
    }

    public PostView adminSetStatus(AuthPrincipal admin, String postId, PostStatus status) {
        CandidatePost p = posts.findById(postId).orElseThrow(() -> ApiException.notFound("Post not found"));
        p.status = status;
        p.updatedAt = Instant.now();
        posts.save(p);
        audit.log(admin, "POST_STATUS_" + status, "POST", p.id, p.title);
        return mapper.posts(List.of(p)).get(0);
    }

    // ---------------- helpers ----------------

    private void fill(CandidatePost p, PostRequest req) {
        String title = TextClean.line(req.title());
        String content = TextClean.block(req.content());
        if (title == null || title.length() < 3 || content == null || content.length() < 10) {
            throw ApiException.badRequest("Please add a title and some content");
        }
        p.type = req.type();
        p.title = title;
        p.content = content;
        p.tags = Skills.clean(req.tags(), 8);
    }
}
