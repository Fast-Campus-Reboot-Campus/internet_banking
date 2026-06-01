package com.bank.loan.commonsync.service;

import com.bank.loan.commonsync.outbox.CommonSyncOutboxAppender;
import com.bank.loan.contract.domain.LoanContract;
import com.bank.loan.contract.repository.LoanContractRepository;
import com.bank.loan.product.domain.LoanProduct;
import com.bank.loan.product.repository.LoanProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * common_db 동기화 백필 서비스.
 *
 * 이미 존재하는 loan_db 레코드 중 common_id 브리지 컬럼이 채워지지 않은 것들을
 * common_sync_outbox 에 일괄 적재한다. 적재 후 CommonSyncDispatchService 가 비동기로 처리.
 *
 * 트랜잭션:
 *   백필 루프는 트랜잭션 없이 실행.
 *   각 enqueueXxxInCurrentTx 호출이 REQUIRED 전파로 자체 트랜잭션 시작 — 행 단위 격리.
 *   한 건 실패가 나머지 백필을 막지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommonSyncBackfillService {

    public static final int DEFAULT_PAGE_SIZE = 500;

    private final LoanProductRepository loanProductRepository;
    private final LoanContractRepository loanContractRepository;
    private final CommonSyncOutboxAppender appender;

    /**
     * productId 가 null 인 (common_db 미동기화) 상품을 pageSize 건씩 픽업해 outbox 적재.
     *
     * @return 이번 호출에서 새로 적재된 outbox 건수
     */
    public int backfillProducts(int pageSize) {
        List<LoanProduct> unsynced = loanProductRepository
                .findByProductIdIsNullAndDeletedAtIsNullOrderByProdIdAsc(
                        PageRequest.of(0, pageSize));

        int enqueued = 0;
        for (LoanProduct p : unsynced) {
            try {
                appender.enqueueProductInCurrentTx(p);
                enqueued++;
            } catch (RuntimeException e) {
                log.warn("[common-sync-backfill] PRODUCT prodId={} skipped: {}",
                        p.getProdId(), e.getMessage());
            }
        }
        log.info("[common-sync-backfill] PRODUCT enqueued={}/{}", enqueued, unsynced.size());
        return enqueued;
    }

    /**
     * ACTIVE/CLOSED 상태이면서 contractId 가 null 인 (common_contract 미동기화) 계약을
     * pageSize 건씩 픽업해 outbox 적재.
     * SIGNED 는 아직 ACTIVE 전이 전이므로 대상 제외.
     *
     * @return 이번 호출에서 새로 적재된 outbox 건수
     */
    public int backfillContracts(int pageSize) {
        List<LoanContract> unsynced = loanContractRepository
                .findByContractIdIsNullAndCntrStatusCdInAndDeletedAtIsNullOrderByCntrIdAsc(
                        List.of(LoanContract.STATUS_ACTIVE, LoanContract.STATUS_CLOSED),
                        PageRequest.of(0, pageSize));

        int enqueued = 0;
        for (LoanContract c : unsynced) {
            try {
                appender.enqueueContractInCurrentTx(c);
                enqueued++;
            } catch (RuntimeException e) {
                log.warn("[common-sync-backfill] CONTRACT cntrId={} skipped: {}",
                        c.getCntrId(), e.getMessage());
            }
        }
        log.info("[common-sync-backfill] CONTRACT enqueued={}/{}", enqueued, unsynced.size());
        return enqueued;
    }
}
