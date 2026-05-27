package com.bank.loan.idv.service;

import com.bank.common.security.mask.Masking;
import com.bank.common.web.BusinessException;
import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.idv.domain.LoanIdentityVerification;
import com.bank.loan.idv.dto.IdentityVerificationResponse;
import com.bank.loan.idv.dto.VerifyIdentityRequest;
import com.bank.loan.idv.repository.LoanIdentityVerificationRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 본인확인 서비스. 외부 인증기관(PASS/카카오/공동인증) 연계는 추후 도입되며,
 * 본 단계에서는 항상 PASS 반환하는 stub 으로 동작한다.
 *
 * mobile_no 평문은 저장하지 않는다:
 *   - mobile_no_masked 만 채움
 *   - mobile_no_enc 는 Phase 3 암호화 구현체 도입 후 채움
 *   - ci_hash 는 SHA-256(mobileNo) 로 임시 산출 (실제 운영에선 인증기관이 발급한 CI 사용)
 */
@Service
@RequiredArgsConstructor
public class LoanIdentityVerificationService {

    private final LoanIdentityVerificationRepository repository;
    private final LoanApplicationRepository applicationRepository;

    @Transactional
    public IdentityVerificationResponse verify(Long applId, VerifyIdentityRequest req,
                                               String clientIp, String device) {
        LoanApplication application = applicationRepository.findByApplIdAndDeletedAtIsNull(applId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_012));

        // 외부 인증 stub — 항상 PASS
        OffsetDateTime now = OffsetDateTime.now();
        String externalTxNo = UUID.randomUUID().toString().replace("-", "");
        String ciHash = sha256Hex(req.mobileNo());

        LoanIdentityVerification saved = repository.save(LoanIdentityVerification.builder()
                .applId(application.getApplId())
                .customerId(application.getCustomerId())
                .idvMethodCd(req.idvMethodCd())
                .idvTargetCd(req.idvTargetCd())
                .idvStatusCd(LoanIdentityVerification.STATUS_DONE)
                .idvResultCd(LoanIdentityVerification.RESULT_PASS)
                .ciHash(ciHash)
                .mobileNoMasked(Masking.mobile(req.mobileNo()))
                .verifiedAt(now)
                .clientIp(clientIp)
                .device(device)
                .externalTxNo(externalTxNo)
                .build());

        return IdentityVerificationResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public IdentityVerificationResponse get(Long idvId) {
        return repository.findByIdvIdAndDeletedAtIsNull(idvId)
                .map(IdentityVerificationResponse::of)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_021));
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
