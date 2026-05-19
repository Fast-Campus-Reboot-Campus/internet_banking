package com.bank.loan.document.repository;

import com.bank.loan.document.domain.LoanDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanDocumentRepository extends JpaRepository<LoanDocument, Long> {

    Optional<LoanDocument> findByDocIdAndDeletedAtIsNull(Long docId);

    List<LoanDocument> findByApplIdAndDeletedAtIsNullOrderBySubmittedAtAsc(Long applId);
}
