package com.bank.docagent.submission.repository;

import com.bank.docagent.submission.domain.DocumentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentSubmissionRepository extends JpaRepository<DocumentSubmission, UUID> {

    List<DocumentSubmission> findByApplicationId(String applicationId);
}
