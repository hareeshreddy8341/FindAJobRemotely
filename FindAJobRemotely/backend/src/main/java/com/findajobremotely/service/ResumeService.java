package com.findajobremotely.service;

import com.findajobremotely.core.FieldCrypto;
import com.findajobremotely.core.TextClean;
import com.findajobremotely.exception.ApiException;
import com.findajobremotely.model.CandidateProfile;
import com.findajobremotely.model.ResumeFile;
import com.findajobremotely.repository.ApplicationRepository;
import com.findajobremotely.repository.CandidateProfileRepository;
import com.findajobremotely.repository.ResumeFileRepository;
import com.findajobremotely.security.AuthPrincipal;
import java.io.IOException;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Resumes are PDF-only, size-limited, encrypted at rest, and downloadable only by the owner or an employer they applied to. */
@Service
public class ResumeService {

    private static final long MAX_BYTES = 2L * 1024 * 1024;

    public record Download(String fileName, byte[] data) {
    }

    private final ResumeFileRepository files;
    private final CandidateProfileRepository profiles;
    private final ApplicationRepository applications;
    private final FieldCrypto crypto;
    private final AuditService audit;

    public ResumeService(ResumeFileRepository files, CandidateProfileRepository profiles,
                         ApplicationRepository applications, FieldCrypto crypto, AuditService audit) {
        this.files = files;
        this.profiles = profiles;
        this.applications = applications;
        this.crypto = crypto;
        this.audit = audit;
    }

    public void upload(String candidateId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Choose a PDF file to upload");
        }
        if (file.getSize() > MAX_BYTES) {
            throw ApiException.badRequest("The file is too large (max 2 MB)");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("The file could not be read");
        }
        // Trust the content, not the client-supplied name or content type
        if (!TextClean.isPdf(data)) {
            throw ApiException.badRequest("Only PDF files are accepted");
        }
        CandidateProfile p = profiles.findById(candidateId)
                .orElseThrow(() -> ApiException.notFound("Profile not found"));

        ResumeFile r = new ResumeFile();
        r.candidateId = candidateId;
        r.fileName = TextClean.safeFileName(file.getOriginalFilename());
        r.sizeBytes = data.length;
        r.dataEnc = crypto.encryptBytes(data, candidateId);
        r.uploadedAt = Instant.now();
        files.save(r);

        p.resumeFileName = r.fileName;
        p.resumeSizeBytes = r.sizeBytes;
        p.resumeUploadedAt = r.uploadedAt;
        p.updatedAt = Instant.now();
        profiles.save(p);
    }

    public void delete(String candidateId) {
        files.deleteById(candidateId);
        profiles.findById(candidateId).ifPresent(p -> {
            p.resumeFileName = null;
            p.resumeSizeBytes = null;
            p.resumeUploadedAt = null;
            profiles.save(p);
        });
    }

    public Download downloadOwn(String candidateId) {
        return load(candidateId);
    }

    /** Employers get a 404 unless this candidate has applied to one of their jobs. */
    public Download downloadForEmployer(AuthPrincipal employer, String candidateId) {
        if (!applications.existsByEmployerIdAndCandidateId(employer.id(), candidateId)) {
            throw ApiException.notFound("Resume not found");
        }
        Download d = load(candidateId);
        audit.log(employer, "RESUME_DOWNLOADED", "CANDIDATE", candidateId, null);
        return d;
    }

    private Download load(String candidateId) {
        ResumeFile r = files.findById(candidateId).orElseThrow(() -> ApiException.notFound("Resume not found"));
        return new Download(r.fileName, crypto.decryptBytes(r.dataEnc, candidateId));
    }
}
