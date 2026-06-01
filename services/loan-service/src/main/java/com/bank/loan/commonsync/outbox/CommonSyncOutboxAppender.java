package com.bank.loan.commonsync.outbox;

import com.bank.loan.commonsync.dto.ProductSyncPayload;
import com.bank.loan.product.domain.LoanProduct;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * common_sync_outbox 적재 헬퍼.
 *
 * 도메인 서비스가 직접 repository 를 건드리지 않게 하고,
 * idempotency_key 중복 검사·status 초기값·백오프 초기값 등 outbox 표준을 한곳에서 관리.
 *
 * 트랜잭션:
 *   enqueueXxxInCurrentTx — REQUIRED (호출자 트랜잭션 참여, 도메인 저장과 원자적).
 *   백필 배치는 트랜잭션 없이 호출 → 각 enqueueProductInCurrentTx 가 자체 tx 시작 (행 단위 격리).
 *
 * 멱등:
 *   idempotencyKey = "TARGET_TYPE:sourceId" (common_sync_outbox.idempotency_key UNIQUE 제약).
 *   동일 키 존재 시 skip.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommonSyncOutboxAppender {

    private static final String BIZ_DIV_LOAN = "LOAN";

    private final CommonSyncOutboxRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * 상품 등록/활성화 시 도메인 트랜잭션 내에서 호출.
     * 이미 outbox 가 존재하면 skip (재등록·멱등 보호).
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void enqueueProductInCurrentTx(LoanProduct product) {
        String key = idempotencyKey(CommonSyncOutbox.TARGET_PRODUCT, product.getProdId());
        if (repository.findByIdempotencyKeyAndDeletedAtIsNull(key).isPresent()) {
            log.debug("[common-sync-appender] skip duplicate key={}", key);
            return;
        }
        String payload = serializeProductPayload(product);
        repository.save(buildOutbox(
                CommonSyncOutbox.TARGET_PRODUCT,
                product.getProdId(),
                product.getProdCd(),
                payload,
                key));
        log.debug("[common-sync-appender] enqueued PRODUCT prodId={} key={}", product.getProdId(), key);
    }

    // -------------------------------------------------------------------------
    // 내부 유틸
    // -------------------------------------------------------------------------

    private static String idempotencyKey(String targetType, Long sourceId) {
        return targetType + ":" + sourceId;
    }

    private CommonSyncOutbox buildOutbox(String targetTypeCd, Long sourceId, String sourceNo,
                                         String payload, String idempotencyKey) {
        return CommonSyncOutbox.builder()
                .targetTypeCd(targetTypeCd)
                .sourceId(sourceId)
                .sourceNo(sourceNo)
                .payload(payload)
                .status(CommonSyncOutbox.STATUS_PENDING)
                .attemptNo(0)
                .maxAttempt(CommonSyncOutbox.DEFAULT_MAX_ATTEMPT)
                .nextAttemptAt(OffsetDateTime.now())
                .idempotencyKey(idempotencyKey)
                .build();
    }

    private String serializeProductPayload(LoanProduct p) {
        try {
            ProductSyncPayload payload = new ProductSyncPayload(
                    p.getProdCd(),
                    BIZ_DIV_LOAN,
                    p.getProdName(),
                    p.getLoanTypeCd(),
                    p.getTargetCustomerCd(),
                    p.getMinAmount(),
                    p.getMaxAmount(),
                    p.getMinPeriodMo(),
                    p.getMaxPeriodMo(),
                    p.getSaleStartDate(),
                    p.getSaleEndDate(),
                    p.getProdStatusCd()
            );
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "ProductSyncPayload 직렬화 실패: prodId=" + p.getProdId(), e);
        }
    }
}
