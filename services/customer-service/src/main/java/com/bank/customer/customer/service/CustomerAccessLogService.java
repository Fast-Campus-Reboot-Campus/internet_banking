package com.bank.customer.customer.service;

import com.bank.customer.customer.domain.CustomerAccessLog;
import com.bank.customer.customer.dto.AccessLogResponse;
import com.bank.customer.customer.repository.CustomerAccessLogRepository;
import com.bank.customer.customer.repository.CustomerRepository;
import com.bank.customer.party.domain.Employee;
import com.bank.customer.party.domain.Party;
import com.bank.customer.party.repository.EmployeeRepository;
import com.bank.customer.party.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 고객 조회 접근 감사로그 — 기록(record)과 조회(search).
 *
 * <p>직원명·역할·지점·고객명은 기록 시점에 스냅샷으로 적재한다(이후 조회는 조인 없이 단순 SELECT).
 * 행위 직원은 게이트웨이가 주입한 X-Employee-Id(employee_id)로만 식별한다.
 */
@Service
@RequiredArgsConstructor
public class CustomerAccessLogService {

    private final CustomerAccessLogRepository accessLogRepository;
    private final EmployeeRepository          employeeRepository;
    private final PartyRepository             partyRepository;
    private final CustomerRepository          customerRepository;

    /**
     * 접근 1건을 기록한다. accessorEmployeeId 가 없으면(시스템·비직원 경로) 적재하지 않는다.
     * 직원/고객 이름·역할·지점은 현재 시점 값을 스냅샷한다.
     */
    @Transactional
    public void record(Long accessorEmployeeId, Long targetCustomerId, String actionCode, String reason) {
        if (accessorEmployeeId == null) return;

        Employee emp = employeeRepository.findById(accessorEmployeeId).orElse(null);
        String accessorName   = emp != null ? partyName(emp.getPartyId()) : null;
        String accessorRole   = emp != null ? emp.getGradeCode()  : null;
        String accessorBranch = emp != null ? emp.getBranchCode() : null;

        String targetName = customerRepository.findByCustomerIdAndDeletedAtIsNull(targetCustomerId)
                .map(c -> partyName(c.getPartyId()))
                .orElse(null);

        accessLogRepository.save(CustomerAccessLog.of(
                accessorEmployeeId, accessorName, accessorRole, accessorBranch,
                targetCustomerId, targetName, actionCode, reason, OffsetDateTime.now()));
    }

    /** 감사로그 조회 — 직원명·고객명·행위 부분일치(keyword), 지점 한정(branchCode). 최신순. */
    @Transactional(readOnly = true)
    public Page<AccessLogResponse> search(String keyword, String branchCode, Pageable pageable) {
        return accessLogRepository.search(normalize(keyword), normalize(branchCode), pageable)
                .map(AccessLogResponse::of);
    }

    private String partyName(Long partyId) {
        return partyRepository.findByPartyIdAndDeletedAtIsNull(partyId)
                .map(Party::getPartyName)
                .orElse(null);
    }

    private String normalize(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
