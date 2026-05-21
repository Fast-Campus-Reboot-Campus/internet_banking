package com.bank.loan.schedule.service;

import com.bank.common.web.BusinessException;
import com.bank.loan.contract.domain.LoanContract;
import com.bank.loan.contract.repository.LoanContractRepository;
import com.bank.loan.schedule.domain.RepaymentSchedule;
import com.bank.loan.schedule.dto.RepaymentScheduleListResponse;
import com.bank.loan.schedule.dto.RepaymentScheduleResponse;
import com.bank.loan.schedule.repository.RepaymentScheduleJdbcBatchInserter;
import com.bank.loan.schedule.repository.RepaymentScheduleRepository;
import com.bank.loan.schedule.strategy.EqualPaymentScheduleGenerator;
import com.bank.loan.schedule.strategy.RepaymentScheduleGenerator;
import com.bank.loan.schedule.strategy.RepaymentScheduleGeneratorRegistry;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 상환 스케줄 생성 · 조회 서비스.
 *
 * 생성은 drawdown 의 부수효과로 호출된다 (flows §1.1 CONTRACTED→DISBURSED 부수효과).
 * 상환방식별 회차 계산은 {@link RepaymentScheduleGenerator} 빈으로 추상화돼 있어
 * Registry 가 repayment_method_cd 로 적절한 구현체를 선택한다 — 미지원 방식은 LOAN_084.
 *
 * 회차당 applied_rate_bps 는 약정의 total_rate_bps 그대로 적용.
 * 금리 변경 시 신규 버전(V2, V3 ...)으로 재생성하고 기존 행은 SUPERSEDED 로 전이한다 — flows §2.3.
 */
@Service
@RequiredArgsConstructor
public class RepaymentScheduleService {

    /** 하위 도메인(PartialRepayment/Prepayment) 가 EQUAL 전용 가드에 참조하는 상수. */
    public static final String REPAY_METHOD_EQUAL = EqualPaymentScheduleGenerator.METHOD;

    private final RepaymentScheduleRepository repository;
    private final RepaymentScheduleJdbcBatchInserter batchInserter;
    private final LoanContractRepository contractRepository;
    private final RepaymentScheduleGeneratorRegistry generatorRegistry;

    /**
     * 최초 인출 시점에 호출. 이미 스케줄이 존재하면 멱등 처리 (재호출 시 no-op).
     * 상환방식에 해당하는 generator 가 없으면 LOAN_084 throw.
     *
     * 회차 행은 JdbcTemplate.batchUpdate 로 한 번에 insert 한다. RepaymentSchedule 은
     * IDENTITY 채번이라 saveAll() 을 써도 Hibernate 가 batch insert 를 비활성화하고
     * 회차 수 만큼 개별 insert 가 날아간다 — 인출 레이턴시가 회차 수에 비례.
     * 호출자는 반환된 엔티티의 ID 를 쓰지 않으므로 batch 후 ID 미할당 상태로 반환한다.
     */
    @Transactional
    public List<RepaymentSchedule> generateForFirstDrawdown(LoanContract contract) {
        if (repository.existsByCntrIdAndDeletedAtIsNull(contract.getCntrId())) {
            return List.of();
        }

        List<RepaymentSchedule> rows = generatorRegistry
                .find(contract.getRepaymentMethodCd())
                .generate(contract);

        batchInserter.batchInsert(rows);
        return rows;
    }

    /**
     * 특정 버전(version) 의 회차 목록을 조회한다. version 이 null/blank 이면 최신 버전을 자동 선택.
     * 중도상환·금리변경으로 발생한 V2/V3 등을 명시적으로 조회할 수 있다.
     */
    @Transactional(readOnly = true)
    public RepaymentScheduleListResponse list(Long cntrId, String version) {
        contractRepository.findByCntrIdAndDeletedAtIsNull(cntrId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_062));

        String effective = (version == null || version.isBlank())
                ? resolveLatestVersion(cntrId)
                : version;

        List<RepaymentScheduleResponse> items = repository
                .findByCntrIdAndRschVersionCdAndDeletedAtIsNullOrderByInstallmentNoAsc(cntrId, effective)
                .stream()
                .map(RepaymentScheduleResponse::of)
                .toList();
        return RepaymentScheduleListResponse.of(cntrId, effective, items);
    }

    private String resolveLatestVersion(Long cntrId) {
        String max = repository.findMaxVersion(cntrId);
        return (max == null || max.isBlank()) ? RepaymentSchedule.VERSION_INITIAL : max;
    }
}
