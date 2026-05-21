package com.bank.loan.review.service;

import com.bank.common.web.BusinessException;
import com.bank.loan.review.dto.ReviewCheckLogResponse;
import com.bank.loan.review.repository.LoanReviewRepository;
import com.bank.loan.review.repository.ReviewCheckLogRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 본심사 체크 로그 조회 서비스. 단건 조회/등록은 자동 적재 경로만 사용 — 본 서비스는 GET 목록만.
 */
@Service
@RequiredArgsConstructor
public class ReviewCheckLogService {

    private final ReviewCheckLogRepository repository;
    private final LoanReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public List<ReviewCheckLogResponse> list(Long revId) {
        reviewRepository.findById(revId)
                .filter(r -> r.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_042));
        return repository.findByRevIdOrderByCheckedAtAscRchkIdAsc(revId).stream()
                .map(ReviewCheckLogResponse::of)
                .toList();
    }
}
