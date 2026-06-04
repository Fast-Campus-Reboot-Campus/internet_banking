package com.bank.loan.virtualaccount.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 가상계좌 입금 → 상환 완결 처리.
 *
 * ⚠️ STUB(의도적 부분구현). 완성하려면 payment 입금통지 이벤트에 <b>수신계좌번호(receiverAccountNo)</b>가
 * 실려야 한다(현재 payment 의 PAYMENT_COMPLETED direction=IN payload 에는 없음 — 결제계 변경 대기).
 *
 * 필드가 추가되면 아래 흐름으로 완성한다:
 * <pre>
 *   1. String receiverAccountNo = event.path("receiverAccountNo").asText();
 *   2. CommonAccount va = commonAccountRepository.findByAccountNo(receiverAccountNo)
 *          .filter(a -&gt; VIRTUAL.equals(a.getAccountTypeCd()));   // 가상계좌 확인
 *   3. Long cntrId = va.getContractId();                          // 가상계좌 → 대출
 *   4. repaymentService.repay...(cntrId, amount, idempotencyKey = piId);  // 상환 완결(멱등)
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualAccountDepositService {

    /**
     * 가상계좌 입금통지 처리. 현재는 receiverAccountNo 미제공이라 매핑 불가 → 로그만 남긴다.
     */
    public void handleInboundDeposit(JsonNode event) {
        String piId = event.path("paymentInstructionId").asText("");
        String amount = event.path("transferAmount").asText("");
        log.info("[가상계좌 STUB] 인바운드 입금 수신 piId={} amount={} — "
                        + "payment payload 에 receiverAccountNo 가 없어 대출 매핑 보류 (결제계 변경 대기)",
                piId, amount);
    }
}
