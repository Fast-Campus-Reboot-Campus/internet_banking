package com.bank.loan.review.service;

import com.bank.common.web.BusinessException;
import com.bank.loan.review.domain.ReviewCheckLog;
import com.bank.loan.review.dto.AddReviewCheckLogRequest;
import com.bank.loan.review.dto.ReviewCheckLogResponse;
import com.bank.loan.review.repository.LoanReviewRepository;
import com.bank.loan.review.repository.ReviewCheckLogRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 본심사 체크 로그 서비스.
 *
 *   list(revId)            본심사 결정 시점에 자동 적재된 5건 + 이후 수동 추가분을 시간순 조회
 *   add(revId, req)        심사관이 수동 체크 항목 추가 (서류·신원·부수거래·기타)
 *
 * 자동 적재 5건은 LoanReviewService.run() 트랜잭션 내에서 ReviewCheckLogger 가 처리한다.
 */
@Service
@RequiredArgsConstructor
public class ReviewCheckLogService {

    private final ReviewCheckLogRepository repository;
    private final LoanReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public List<ReviewCheckLogResponse> list(Long revId) {
        requireReview(revId);
        return repository.findByRevIdOrderByCheckedAtAscRchkIdAsc(revId).stream()
                .map(ReviewCheckLogResponse::of)
                .toList();
    }

    @Transactional
    public ReviewCheckLogResponse add(Long revId, AddReviewCheckLogRequest req) {
        requireReview(revId);

        // 수동 항목만 허용 — 자동 적재 항목 덮어쓰기 방지
        // (request DTO @Pattern 으로 1차 차단되지만 도메인 화이트리스트로 한 번 더 확인)
        if (!ReviewCheckLog.isManualItem(req.checkItemCd())) {
            throw new BusinessException(LoanErrorCode.LOAN_043,
                    "auto-item not allowed: " + req.checkItemCd());
        }

        ReviewCheckLog saved = repository.save(ReviewCheckLog.builder()
                .revId(revId)
                .checkItemCd(req.checkItemCd())
                .checkResultCd(req.checkResultCd())
                .checkRemark(req.checkRemark())
                .checkerId(req.checkerId())
                .checkedAt(OffsetDateTime.now())
                .build());
        return ReviewCheckLogResponse.of(saved);
    }

    private void requireReview(Long revId) {
        reviewRepository.findById(revId)
                .filter(r -> r.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_042));
    }
}
