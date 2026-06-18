import { describe, it, expect } from 'vitest'
import {
  BankRole,
  hasAnyRole,
  isEmployee,
  isHeadOffice,
  isMaskingRole,
  canViewAuditLog,
  requiresReason,
  isOtherBranch,
  primaryRoleLabel,
  branchLabel,
} from '@/lib/admin-auth'

describe('hasAnyRole', () => {
  it('required 중 하나라도 보유하면 true', () => {
    expect(hasAnyRole([BankRole.TELLER], BankRole.TELLER, BankRole.OPS)).toBe(true)
  })
  it('보유 역할이 없으면 false', () => {
    expect(hasAnyRole([BankRole.TELLER], BankRole.COMPLIANCE)).toBe(false)
  })
  it('ROLE_ADMIN 은 무엇이든 통과 (시스템 관리자)', () => {
    expect(hasAnyRole([BankRole.ADMIN], BankRole.COMPLIANCE)).toBe(true)
  })
})

describe('isEmployee', () => {
  it('CUSTOMER 만 있으면 직원 아님', () => {
    expect(isEmployee([BankRole.CUSTOMER])).toBe(false)
  })
  it('CUSTOMER 외 역할이 하나라도 있으면 직원', () => {
    expect(isEmployee([BankRole.CUSTOMER, BankRole.TELLER])).toBe(true)
  })
  it('빈 역할은 직원 아님', () => {
    expect(isEmployee([])).toBe(false)
  })
})

describe('접근 정책 — 본사/마스킹/감사', () => {
  it('isHeadOffice: 본사 직군만 true', () => {
    expect(isHeadOffice([BankRole.HQ_RISK])).toBe(true)
    expect(isHeadOffice([BankRole.COMPLIANCE])).toBe(true)
    expect(isHeadOffice([BankRole.TELLER])).toBe(false)
  })
  it('isMaskingRole: HQ_RISK 만 (TELLER 는 아님)', () => {
    expect(isMaskingRole([BankRole.HQ_RISK])).toBe(true)
    expect(isMaskingRole([BankRole.TELLER])).toBe(false)
  })
  it('canViewAuditLog: 감사/심사/지점장/창구는 가능, OPS 는 불가', () => {
    expect(canViewAuditLog([BankRole.COMPLIANCE])).toBe(true)
    expect(canViewAuditLog([BankRole.TELLER])).toBe(true)
    expect(canViewAuditLog([BankRole.OPS])).toBe(false)
  })
})

describe('requiresReason — 창구직원이고 본사 직군 아닐 때만', () => {
  it('창구직원은 조회 사유 필요', () => {
    expect(requiresReason([BankRole.TELLER])).toBe(true)
  })
  it('본사 직군은 사유 불필요', () => {
    expect(requiresReason([BankRole.HQ_RISK])).toBe(false)
  })
  it('ADMIN 은 본사로도 통과되므로 사유 불필요 (만능 통과의 부작용 고정)', () => {
    expect(requiresReason([BankRole.ADMIN])).toBe(false)
  })
})

describe('isOtherBranch — 동적 지점 비교', () => {
  it('본사 직군은 항상 false (전 지점 열람)', () => {
    expect(isOtherBranch([BankRole.HQ_RISK], '0001', '0002')).toBe(false)
  })
  it('지점 정보가 없으면 false', () => {
    expect(isOtherBranch([BankRole.TELLER], null, '0002')).toBe(false)
  })
  it('직원 지점 ≠ 대상 지점이면 타 지점', () => {
    expect(isOtherBranch([BankRole.TELLER], '0001', '0002')).toBe(true)
  })
  it('같은 지점이면 false', () => {
    expect(isOtherBranch([BankRole.TELLER], '0001', '0001')).toBe(false)
  })
})

describe('표시 라벨', () => {
  it('primaryRoleLabel: 권한 큰 역할 우선', () => {
    expect(primaryRoleLabel([BankRole.TELLER, BankRole.COMPLIANCE])).toBe('컴플라이언스/감사')
  })
  it('primaryRoleLabel: 알 수 없으면 기본 직원', () => {
    expect(primaryRoleLabel([])).toBe('직원')
  })
  it('branchLabel: 알려진/미상/null', () => {
    expect(branchLabel('0001')).toBe('강남지점')
    expect(branchLabel('9999')).toBe('지점 9999')
    expect(branchLabel(null)).toBe('-')
  })
})
