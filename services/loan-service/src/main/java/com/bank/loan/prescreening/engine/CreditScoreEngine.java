package com.bank.loan.prescreening.engine;

/**
 * 외부 신용평가(가심사) 엔진 어댑터 — 가심사 단계가 호출하는 외부 시스템 경계.
 *
 * 운영 구현체는 외부 신용정보사(KCB/NICE) HTTP API 또는 사내 PD 모델 서비스를 호출.
 * 본 단계에선 {@link MockCreditScoreEngine} 만 제공하며, 후속 단계에서 운영용 구현으로 교체.
 *
 * 구현체는 stateless · idempotent (같은 입력 → 같은 출력) 를 권장한다 — 재시도 안전성 확보용.
 */
public interface CreditScoreEngine {

    CreditScoreResult evaluate(CreditScoreRequest request);
}
