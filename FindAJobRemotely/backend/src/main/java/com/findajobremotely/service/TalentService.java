package com.findajobremotely.service;

import com.findajobremotely.core.Skills;
import com.findajobremotely.core.TextClean;
import com.findajobremotely.dto.CommonDtos.PageResponse;
import com.findajobremotely.dto.PostDtos.TalentDetail;
import com.findajobremotely.dto.ProfileDtos.CandidatePublicView;
import com.findajobremotely.exception.ApiException;
import com.findajobremotely.model.CandidatePost;
import com.findajobremotely.model.CandidateProfile;
import com.findajobremotely.model.ExperienceLevel;
import com.findajobremotely.model.JobType;
import com.findajobremotely.model.PostStatus;
import com.findajobremotely.repository.ApplicationRepository;
import com.findajobremotely.repository.CandidatePostRepository;
import com.findajobremotely.repository.CandidateProfileRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

/** Talent search for employers. Only public data of candidates who opted in is ever returned. */
@Service
public class TalentService {

    private final MongoTemplate template;
    private final CandidateProfileRepository candidates;
    private final CandidatePostRepository posts;
    private final ApplicationRepository applications;
    private final ViewMapper mapper;

    public TalentService(MongoTemplate template, CandidateProfileRepository candidates,
                         CandidatePostRepository posts, ApplicationRepository applications, ViewMapper mapper) {
        this.template = template;
        this.candidates = candidates;
        this.posts = posts;
        this.applications = applications;
        this.mapper = mapper;
    }

    public PageResponse<CandidatePublicView> search(String q, String skill, ExperienceLevel level, JobType type,
                                                    int page, int size) {
        Pageable pageable = Paging.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        List<Criteria> all = new ArrayList<>();
        all.add(Criteria.where("publicProfile").is(true));
        all.add(Criteria.where("accountActive").is(true));
        // Hide empty profiles
        all.add(Criteria.where("headline").exists(true).ne(null));
        if (level != null) {
            all.add(Criteria.where("experienceLevel").is(level));
        }
        if (type != null) {
            all.add(Criteria.where("preferredJobTypes").is(type));
        }
        String cleanSkill = TextClean.line(skill);
        if (cleanSkill != null && !cleanSkill.isBlank()) {
            String rx = "^" + TextClean.escapeRegex(cleanSkill.length() > 40 ? cleanSkill.substring(0, 40) : cleanSkill) + "$";
            all.add(Criteria.where("skills").regex(Pattern.compile(rx, Pattern.CASE_INSENSITIVE)));
        }
        String cleanQ = TextClean.line(q);
        if (cleanQ != null && !cleanQ.isBlank()) {
            String rx = TextClean.escapeRegex(cleanQ.length() > 80 ? cleanQ.substring(0, 80) : cleanQ);
            Pattern pattern = Pattern.compile(rx, Pattern.CASE_INSENSITIVE);
            all.add(new Criteria().orOperator(
                    Criteria.where("headline").regex(pattern),
                    Criteria.where("about").regex(pattern),
                    Criteria.where("skills").regex(pattern),
                    Criteria.where("preferredRoles").regex(pattern)));
        }
        Query query = new Query(new Criteria().andOperator(all.toArray(new Criteria[0])));
        long total = template.count(query, CandidateProfile.class);
        List<CandidateProfile> found = template.find(query.with(pageable), CandidateProfile.class);
        return PageResponse.of(new PageImpl<>(mapper.candidatesPublic(found), pageable, total));
    }

    /** A candidate's public profile and visible posts. Visible if the profile is public, or they applied to this employer. */
    public TalentDetail detail(String employerId, String candidateId) {
        CandidateProfile p = candidates.findById(candidateId)
                .orElseThrow(() -> ApiException.notFound("Candidate not found"));
        boolean visible = p.accountActive
                && (p.publicProfile || applications.existsByEmployerIdAndCandidateId(employerId, candidateId));
        if (!visible) {
            throw ApiException.notFound("Candidate not found");
        }
        var user = mapper.usersById(List.of(candidateId)).get(candidateId);
        List<CandidatePost> visiblePosts = posts.findByAuthorIdAndStatus(candidateId, PostStatus.VISIBLE,
                Paging.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new TalentDetail(mapper.candidatePublic(user, p), mapper.posts(visiblePosts));
    }
}
