package com.bank.loan.guarantor.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.security.mask.Masking;
import com.bank.common.web.BusinessException;
import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.guarantor.domain.GuarantorAgreement;
import com.bank.loan.guarantor.domain.GuarantorMaster;
import com.bank.loan.guarantor.dto.CancelGuarantorAgreementRequest;
import com.bank.loan.guarantor.dto.GuarantorAgreementListResponse;
import com.bank.loan.guarantor.dto.GuarantorAgreementResponse;
import com.bank.loan.guarantor.dto.RegisterGuarantorAgreementRequest;
import com.bank.loan.guarantor.dto.SignGuarantorAgreementRequest;
import com.bank.loan.guarantor.repository.GuarantorAgreementRepository;
import com.bank.loan.guarantor.repository.GuarantorMasterRepository;
import com.bank.loan.notification.event.GuarantorCanceledEvent;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/**
 * 보증 약정 서비스 (flows §1.1 — APPROVED → CONTRACTED 전제조건 중 하나).
 *
 * 등록 가능한 신청 상태: SUBMITTED / PRESCREENED / REVIEWING / APPROVED.
 *   - CONTRACTED 이후엔 약정 변경 차단 — 약정 후 보증 변경은 별도 절차로 분리.
 *   - 종료 상태(REJECTED/EXPIRED/CANCELED/WITHDRAWN/CONTRACTED) 는 등록 불가.
 *
 * PII 처리 (idv 와 동일 패턴):
 *   - guarantor_name_masked / mobile_no_masked 는 Masking 유틸로 채움
 *   - guarantor_name_enc / mobile_no_enc 는 평문 UTF-8 bytes 임시 stub
 *   - guarantor_ci_hash 는 SHA-256(mobileNo) 임시 — 인증기관 발급 CI 도입 시 교체
 *
 * 같은 신청에 같은 보증인의 REGISTERED/SIGNED 약정은 중복 등록 차단 (LOAN_174).
 * 다른 신청에서 이미 GuarantorMaster row 가 있으면 (ci_hash 매칭) 재사용한다.
 */
@Service
@RequiredArgsConstructor
public class GuarantorAgreementService {

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_TABLE_CD = "GUARANTOR_AGREEMENT";
    private static final String REASON_REGISTERED = "GUARANTOR_REGISTERED";
    private static final String REASON_SIGNED     = "GUARANTOR_SIGNED";
    private static final String REASON_CANCELED   = "GUARANTOR_CANCELED";

    private static final Set<String> REGISTERABLE_APPL_STATUSES = Set.of(
            LoanApplication.STATUS_SUBMITTED,
            LoanApplication.STATUS_PRESCREENED,
            LoanApplication.STATUS_REVIEWING,
            LoanApplication.STATUS_APPROVED
    );

    private static final List<String> ACTIVE_GAGR_STATUSES = List.of(
            GuarantorAgreement.STATUS_REGISTERED,
            GuarantorAgreement.STATUS_SIGNED
    );

    private final LoanApplicationRepository applicationRepository;
    private final GuarantorMasterRepository masterRepository;
    private final GuarantorAgreementRepository agreementRepository;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public GuarantorAgreementResponse register(Long applId, RegisterGuarantorAgreementRequest req) {
        LoanApplication application = applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));
        if (!REGISTERABLE_APPL_STATUSES.contains(application.currentStatus())) {
            throw new BusinessException(LoanErrorCode.LOAN_173,
                    "current=" + application.currentStatus());
        }

        String ciHash = sha256Hex(req.guarantorMobileNo());
        GuarantorMaster master = masterRepository.findByGuarantorCiHashAndDeletedAtIsNull(ciHash)
                .orElseGet(() -> masterRepository.save(GuarantorMaster.builder()
                        .guarantorNameEnc(req.guarantorName().getBytes(StandardCharsets.UTF_8))
                        .guarantorNameMasked(Masking.name(req.guarantorName()))
                        .guarantorCiHash(ciHash)
                        .relationTypeCd(req.relationTypeCd())
                        .mobileNoEnc(req.guarantorMobileNo().getBytes(StandardCharsets.UTF_8))
                        .mobileNoMasked(Masking.mobile(req.guarantorMobileNo()))
                        .build()));

        if (agreementRepository.existsByApplIdAndGmstIdAndGagrStatusCdInAndDeletedAtIsNull(
                applId, master.getGmstId(), ACTIVE_GAGR_STATUSES)) {
            throw new BusinessException(LoanErrorCode.LOAN_174,
                    "applId=" + applId + ", gmstId=" + master.getGmstId());
        }

        OffsetDateTime now = OffsetDateTime.now();
        GuarantorAgreement saved = agreementRepository.save(GuarantorAgreement.builder()
                .applId(applId)
                .gmstId(master.getGmstId())
                .gagrTypeCd(req.gagrTypeCd())
                .guaranteeAmount(req.guaranteeAmount())
                .guaranteeRatioBps(req.guaranteeRatioBps())
                .gagrStatusCd(GuarantorAgreement.STATUS_REGISTERED)
                .consentedAt(now)
                .build());

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_TABLE_CD, saved.getGagrId(),
                null, GuarantorAgreement.STATUS_REGISTERED,
                REASON_REGISTERED,
                "applId=" + applId + ", gmstId=" + master.getGmstId() + ", type=" + req.gagrTypeCd(),
                currentActor.currentActorId()
        ));

        return GuarantorAgreementResponse.of(saved, master);
    }

    @Transactional
    public GuarantorAgreementResponse sign(Long applId, Long gagrId,
                                           SignGuarantorAgreementRequest req,
                                           String clientIp, String device) {
        GuarantorAgreement agreement = requireAgreement(applId, gagrId);
        if (!agreement.isSignable()) {
            throw new BusinessException(LoanErrorCode.LOAN_171,
                    "current=" + agreement.currentStatus());
        }

        String before = agreement.currentStatus();
        agreement.markSigned(req.signedDocUrl(), req.signedDocHash(), clientIp, device);

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_TABLE_CD, agreement.getGagrId(),
                before, GuarantorAgreement.STATUS_SIGNED,
                REASON_SIGNED, "docHash=" + req.signedDocHash(),
                currentActor.currentActorId()
        ));

        GuarantorMaster master = masterRepository.findByGmstIdAndDeletedAtIsNull(agreement.getGmstId())
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_170));
        return GuarantorAgreementResponse.of(agreement, master);
    }

    @Transactional
    public GuarantorAgreementResponse cancel(Long applId, Long gagrId,
                                             CancelGuarantorAgreementRequest req) {
        GuarantorAgreement agreement = requireAgreement(applId, gagrId);
        if (!agreement.isCancellable()) {
            throw new BusinessException(LoanErrorCode.LOAN_172,
                    "current=" + agreement.currentStatus());
        }

        String before = agreement.currentStatus();
        agreement.markCanceled();

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_TABLE_CD, agreement.getGagrId(),
                before, GuarantorAgreement.STATUS_CANCELED,
                req == null || req.cancelReasonCd() == null ? REASON_CANCELED : req.cancelReasonCd(),
                req == null ? null : req.cancelRemark(),
                currentActor.currentActorId()
        ));

        // 취소 이벤트 발행 — SIGNED 보증인이 빠질 경우 GuarantorNotificationListener 가 운영자 알람 적재
        eventPublisher.publishEvent(new GuarantorCanceledEvent(
                agreement.getApplId(), agreement.getGagrId(), before));

        GuarantorMaster master = masterRepository.findByGmstIdAndDeletedAtIsNull(agreement.getGmstId())
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_170));
        return GuarantorAgreementResponse.of(agreement, master);
    }

    @Transactional(readOnly = true)
    public GuarantorAgreementListResponse list(Long applId) {
        applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));

        List<GuarantorAgreement> agreements = agreementRepository
                .findByApplIdAndDeletedAtIsNullOrderByGagrIdAsc(applId);

        List<GuarantorAgreementResponse> items = agreements.stream()
                .map(a -> GuarantorAgreementResponse.of(a,
                        masterRepository.findByGmstIdAndDeletedAtIsNull(a.getGmstId())
                                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_170))))
                .toList();

        return GuarantorAgreementListResponse.of(applId, items);
    }

    private GuarantorAgreement requireAgreement(Long applId, Long gagrId) {
        GuarantorAgreement agreement = agreementRepository.findByGagrIdAndDeletedAtIsNull(gagrId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_170));
        if (!agreement.getApplId().equals(applId)) {
            throw new BusinessException(LoanErrorCode.LOAN_170,
                    "applId mismatch: agreement.applId=" + agreement.getApplId() + ", path=" + applId);
        }
        return agreement;
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(LoanErrorCode.LOAN_020, e.getMessage());
        }
    }
}
