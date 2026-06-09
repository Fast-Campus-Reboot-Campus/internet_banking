package com.bank.loan.support;

import com.bank.common.web.ApiResponse;
import com.bank.loan.document.docagent.DocAgentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * doc-agent 연동 실패 전용 예외 처리.
 *
 * 서류 업로드 경로는 {@code LoanDocumentService} 에서 {@link DocAgentException} 을 직접 잡아
 * 검증 보류(PENDING)로 우아하게 강등하므로 이 핸들러까지 전파되지 않는다.
 * 이 핸들러는 그 외 doc-agent 호출 경로에서 예외가 새어 나갈 때를 위한 안전망으로,
 * 공통 GlobalExceptionHandler 의 뭉뚱그린 500("서버 오류가 발생했습니다") 대신
 * 의미 있는 503(LOAN_056) 을 반환한다.
 */
@RestControllerAdvice
public class DocAgentExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(DocAgentExceptionHandler.class);

    @ExceptionHandler(DocAgentException.class)
    public ResponseEntity<ApiResponse<Void>> handleDocAgent(DocAgentException ex) {
        LoanErrorCode c = LoanErrorCode.LOAN_056;
        log.error("doc-agent 연동 실패", ex);
        return ResponseEntity.status(c.getStatus())
                .body(ApiResponse.error(c.getCode(), c.getMessage()));
    }
}
