package com.bank.loan.document.repository;

import com.bank.loan.document.domain.LoanDocumentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanDocumentSubmissionRepository extends JpaRepository<LoanDocumentSubmission, String> {
}
