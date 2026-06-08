package com.bank.customer.party.service;

import com.bank.common.security.BankRole;
import com.bank.customer.party.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 직원 디렉토리 (DB 기반).
 *
 * <p>기존 application.yml {@code employee-directory} 설정을 대체한다. 직원 party 의
 * EMPLOYEE party_role + employee 디테일을 조회해 JWT 발급에 필요한 {branch, grade, roles}
 * 를 제공한다. roles 는 grade_code(= {@link BankRole} 이름)에서 1:1 로 파생한다.
 */
@Service
@RequiredArgsConstructor
public class EmployeeDirectoryService {

    private final EmployeeRepository employeeRepository;

    /** 직원 디렉토리 조회 결과 — 직원이 아니면 비어 있다. */
    public record EmployeeInfo(Long employeeId, String branch, String grade, List<String> roles) {}

    @Transactional(readOnly = true)
    public Optional<EmployeeInfo> findByPartyId(Long partyId) {
        if (partyId == null) return Optional.empty();
        return employeeRepository.findActiveByPartyId(partyId)
                .map(e -> new EmployeeInfo(
                        e.getEmployeeId(),
                        e.getBranchCode(),
                        e.getGradeCode(),
                        rolesForGrade(e.getGradeCode())));
    }

    /** grade_code(= BankRole 이름) → 권한 문자열 1종. 미상 grade 는 일반 고객으로 강등. */
    private List<String> rolesForGrade(String gradeCode) {
        try {
            return List.of(BankRole.valueOf(gradeCode).authority());
        } catch (IllegalArgumentException e) {
            return List.of(BankRole.CUSTOMER.authority());
        }
    }
}
