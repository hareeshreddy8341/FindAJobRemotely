package com.findajobremotely.controller;

import com.findajobremotely.dto.CommonDtos.PageResponse;
import com.findajobremotely.dto.JobDtos.JobView;
import com.findajobremotely.model.ExperienceLevel;
import com.findajobremotely.model.JobType;
import com.findajobremotely.security.AuthPrincipal;
import com.findajobremotely.service.JobService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public job browsing - no login required. */
@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobs;

    public JobController(JobService jobs) {
        this.jobs = jobs;
    }

    @GetMapping
    public PageResponse<JobView> search(@RequestParam(required = false) String q,
                                        @RequestParam(required = false) JobType type,
                                        @RequestParam(required = false) ExperienceLevel level,
                                        @RequestParam(required = false) String skill,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return jobs.search(q, type, level, skill, page, size);
    }

    @GetMapping("/{id}")
    public JobView get(@PathVariable String id, Authentication auth) {
        AuthPrincipal viewer = auth != null && auth.getPrincipal() instanceof AuthPrincipal p ? p : null;
        return jobs.get(id, viewer);
    }
}
