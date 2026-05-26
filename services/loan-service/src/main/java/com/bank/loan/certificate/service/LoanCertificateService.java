package com.bank.loan.certificate.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.certificate.domain.LoanCertificate;
import com.bank.loan.certificate.dto.IssueCertificateRequest;
import com.bank.loan.certificate.dto.LoanCertificateListResponse;
import com.bank.loan.certificate.dto.LoanCertificateResponse;
import com.bank.loan.certificate.repository.LoanCertificateRepository;
import com.bank.loan.contract.domain.LoanContract;
import com.bank.loan.contract.repository.LoanContractRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 증명서 발급·조회 서비스.
 *
 * 본 단계는 즉시 발급(ISSUED). PDF 실제 생성·외부 송부는 stub —
 * cert_doc_url/cert_doc_hash 는 요청자가 사전 생성해 넘기거나 null.
 * 다발 발급(재발급)은 새 row 로 누적.
 */
@Service
@RequiredArgsConstructor
public class LoanCertificateService {

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_TABLE_CD = "LOAN_CERTIFICATE";
    private static final String REASON_ISSUED = "CERT_ISSUED";

    private final LoanCertificateRepository repository;
    private final LoanContractRepository contractRepository;
    private final CertificateNumberGenerator certNoGenerator;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

    @Transactional
    public LoanCertificateResponse issue(Long cntrId, IssueCertificateRequest req) {
        LoanContract contract = contractRepository.findByCntrIdAndDeletedAtIsNull(cntrId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_062));

        OffsetDateTime now = OffsetDateTime.now();
        LoanCertificate saved = repository.save(LoanCertificate.builder()
                .cntrId(contract.getCntrId())
                .customerId(contract.getCustomerId())
                .certTypeCd(req.certTypeCd())
                .certNo(certNoGenerator.generate(now))
                .certStatusCd(LoanCertificate.STATUS_ISSUED)
                .certPurposeCd(req.certPurposeCd())
                .certDocUrl(req.certDocUrl())
                .certDocHash(req.certDocHash())
                .issueChannelCd(req.issueChannelCd())
                .issuedAt(now)
                .retentionUntil(req.retentionUntil())
                .build());

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_TABLE_CD, saved.getCertId(),
                null, LoanCertificate.STATUS_ISSUED,
                REASON_ISSUED,
                "certNo=" + saved.getCertNo() + " / type=" + req.certTypeCd(),
                currentActor.currentActorId()
        ));

        return LoanCertificateResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public LoanCertificateListResponse list(Long cntrId) {
        contractRepository.findByCntrIdAndDeletedAtIsNull(cntrId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_062));
        List<LoanCertificateResponse> items = repository
                .findByCntrIdAndDeletedAtIsNullOrderByIssuedAtAsc(cntrId)
                .stream()
                .map(LoanCertificateResponse::of)
                .toList();
        return LoanCertificateListResponse.of(cntrId, items);
    }

    @Transactional(readOnly = true)
    public LoanCertificateResponse getById(Long certId) {
        return repository.findByCertIdAndDeletedAtIsNull(certId)
                .map(LoanCertificateResponse::of)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_140));
    }
}
