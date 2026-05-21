package com.bank.loan.prescreening.engine;

/**
 * 외부 신용평가 엔진 호출 실패. timeout, 5xx, IO 오류 등 재시도 정책 소진 후 던져진다.
 * 서비스 계층이 잡아 BusinessException(LOAN_029) 로 변환해 사용자에게 일시 장애 응답으로 노출한다.
 */
public class CreditScoreEngineException extends RuntimeException {
    public CreditScoreEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
