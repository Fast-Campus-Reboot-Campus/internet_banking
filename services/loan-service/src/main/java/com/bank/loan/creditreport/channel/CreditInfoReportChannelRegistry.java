package com.bank.loan.creditreport.channel;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * agencyCd → adapter 룩업.
 *
 * Spring 이 모든 {@link CreditInfoReportChannelAdapter} 빈을 주입한다.
 * 같은 agencyCd 로 중복 등록되면 부팅 시점에 즉시 실패시킨다 (Map 빌드에서 duplicate key).
 */
@Component
public class CreditInfoReportChannelRegistry {

    private final Map<String, CreditInfoReportChannelAdapter> byAgency;

    public CreditInfoReportChannelRegistry(List<CreditInfoReportChannelAdapter> adapters) {
        this.byAgency = adapters.stream()
                .collect(Collectors.toUnmodifiableMap(
                        CreditInfoReportChannelAdapter::getAgencyCd,
                        Function.identity()));
    }

    public CreditInfoReportChannelAdapter resolve(String agencyCd) {
        CreditInfoReportChannelAdapter adapter = byAgency.get(agencyCd);
        if (adapter == null) {
            // 운영 구성 오류 — 신고 row 가 알 수 없는 agencyCd 로 적재됐다는 뜻.
            // BusinessException 이 아니라 시스템 오류 (운영자에게 명확한 시그널).
            throw new IllegalStateException("no adapter for agencyCd=" + agencyCd);
        }
        return adapter;
    }
}
