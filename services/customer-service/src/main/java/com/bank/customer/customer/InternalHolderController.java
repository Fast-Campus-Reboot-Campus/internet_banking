package com.bank.customer.customer;

import com.bank.common.web.BusinessException;
import com.bank.common.web.CommonErrorCode;
import com.bank.customer.customer.domain.Customer;
import com.bank.customer.customer.dto.HolderInfoResponse;
import com.bank.customer.customer.repository.CustomerRepository;
import com.bank.customer.party.domain.Party;
import com.bank.customer.party.repository.PartyRepository;
import com.bank.customer.support.CustomerErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 예금주 확인 — 서비스 간 내부 호출용 읽기 전용 API (A-2 흐름).
 *
 * <p>호출 체인: payment-service → deposit-service → customer-service(본 엔드포인트).
 * deposit 이 Feign 으로 호출해 계좌 예금주 실명을 채우고, payment 는 그 값으로 이체 전
 * 예금주 일치 여부를 검증한다.
 *
 * <p><b>경로 컨벤션</b>: 직원 전용 {@code /api/v1/internal/**}(SecurityConfig 가 직무 역할로
 * 게이팅) 과 구분되는 <em>서비스 간</em> 내부 경로 {@code /api/internal/**}(v1 없음)을 쓴다.
 * 후자는 SecurityConfig 매처에 걸리지 않아 {@code permitAll} 로 떨어지므로(게이트웨이
 * api-gateway application.yml 에 문서화된 컨벤션) 직원 토큰 없는 서비스 호출이 통과한다.
 * 직원 인가 대신 {@code X-Caller-Service} 헤더 존재만 확인한다(내부망 신뢰 전제).
 */
@RestController
@RequestMapping("/api/internal/customers")
@RequiredArgsConstructor
public class InternalHolderController {

    private final CustomerRepository customerRepository;
    private final PartyRepository partyRepository;

    @GetMapping("/{customerId}/holder-info")
    public ResponseEntity<HolderInfoResponse> getHolderInfo(
            @PathVariable Long customerId,
            @RequestHeader(name = "X-Caller-Service", required = false) String callerService) {

        if (callerService == null || callerService.isBlank()) {
            throw new BusinessException(CommonErrorCode.COMMON_403);
        }

        Customer customer = customerRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));
        Party party = partyRepository.findByPartyIdAndDeletedAtIsNull(customer.getPartyId())
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));

        // TODO(deceasedFlag): Party 에 사망 컬럼이 없어 현재 false 고정이다. 다운스트림
        //  payment-service 는 이 값을 "확정 생존"으로 해석하면 안 되고, 사망 차단은
        //  사망 정보 소스(party 사망 컬럼/외부 연계) 연결 전까지 미검증으로 취급해야 한다.
        //  사망 컬럼 wiring 후 false 하드코딩을 실제 값으로 교체할 것.
        boolean deceasedFlag = false;
        return ResponseEntity.ok(new HolderInfoResponse(
                String.valueOf(customerId),
                party.getPartyName(),
                holderType(party.getPartyTypeCode()),
                deceasedFlag));
    }

    /** party_type_code(PERSONAL/ORGANIZATION) → 예금주 유형. Party 모델에 JOINT 없음 → 개인 기본값. */
    private static String holderType(String partyTypeCode) {
        return Party.TYPE_ORGANIZATION.equals(partyTypeCode) ? "CORPORATE" : "INDIVIDUAL";
    }
}
