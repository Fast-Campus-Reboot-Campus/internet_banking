// 관리자 콘솔 역할 기반 접근제어 헬퍼.
//
// 역할 어휘는 백엔드 common.BankRole 과 동일한 단일 출처를 쓴다. 관리자 로그인 시
// JWT 에서 추출한 실제 역할 배열을 localStorage('admin_roles')에 저장해 두며(PR-3a),
// 이 모듈이 그 배열을 읽어 메뉴/버튼 show·hide 를 판정한다.

/** common.BankRole 과 1:1. 게이팅 코드의 오타 방지를 위해 상수로 노출한다. */
export const BankRole = {
  CUSTOMER:       'ROLE_CUSTOMER',
  TELLER:         'ROLE_TELLER',
  DEPUTY_MANAGER: 'ROLE_DEPUTY_MANAGER',
  BRANCH_MANAGER: 'ROLE_BRANCH_MANAGER',
  HQ_REVIEWER:    'ROLE_HQ_REVIEWER',
  HQ_RISK:        'ROLE_HQ_RISK',
  HQ_MARKETING:   'ROLE_HQ_MARKETING',
  COMPLIANCE:     'ROLE_COMPLIANCE',
  OPS:            'ROLE_OPS',
  INTERNAL:       'ROLE_INTERNAL',
  ADMIN:          'ROLE_ADMIN',
} as const

export type BankRoleValue = (typeof BankRole)[keyof typeof BankRole]

/** localStorage 의 실제 역할 배열(JWT 기반). SSR 단계에서는 빈 배열. */
export function getAdminRoles(): string[] {
  if (typeof window === 'undefined') return []
  try {
    const raw = localStorage.getItem('admin_roles')
    const parsed = raw ? JSON.parse(raw) : []
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

/** required 중 하나라도 보유하면 true. ROLE_ADMIN 은 시스템 관리자로 항상 통과시킨다. */
export function hasAnyRole(roles: string[], ...required: string[]): boolean {
  if (roles.includes(BankRole.ADMIN)) return true
  return required.some((r) => roles.includes(r))
}

/** CUSTOMER 를 제외한 직원이면 true (break-glass 긴급 접근 등 '전 직원' 범위). */
export function isEmployee(roles: string[]): boolean {
  return roles.some((r) => r !== BankRole.CUSTOMER)
}

// ─── 표시용 라벨 (BankRole 단일 어휘) ─────────────────────────────────────────
// AdminRole 모델 재설계: 역할 어휘를 BankRole(JWT)로 단일화하고, '담당/타지점' 같은
// 동적 관계는 역할이 아니라 지점 비교 등으로 계산한다. 화면은 이 헬퍼들을 통해 게이팅한다.

/** BankRole authority → 한글 라벨 */
export const BANK_ROLE_LABEL: Record<string, string> = {
  ROLE_BRANCH_MANAGER: '지점장',
  ROLE_DEPUTY_MANAGER: '부지점장',
  ROLE_TELLER:         '창구직원',
  ROLE_HQ_REVIEWER:    '본사 심사',
  ROLE_HQ_RISK:        '리스크관리',
  ROLE_HQ_MARKETING:   '마케팅/기획',
  ROLE_COMPLIANCE:     '컴플라이언스/감사',
  ROLE_OPS:            '운영',
  ROLE_ADMIN:          '시스템관리자',
  ROLE_CUSTOMER:       '고객',
}

/** 배지 표시용 대표 역할 라벨 — 권한이 큰 역할 우선. */
const ROLE_PRIORITY = [
  BankRole.ADMIN, BankRole.COMPLIANCE, BankRole.HQ_REVIEWER, BankRole.HQ_RISK,
  BankRole.HQ_MARKETING, BankRole.BRANCH_MANAGER, BankRole.DEPUTY_MANAGER,
  BankRole.OPS, BankRole.TELLER, BankRole.CUSTOMER,
]
export function primaryRoleLabel(roles: string[]): string {
  const top = ROLE_PRIORITY.find((r) => roles.includes(r))
  return top ? BANK_ROLE_LABEL[top] : '직원'
}

/** branch_code → 지점명 (미상 코드는 '지점 {code}'). */
export const BRANCH_NAME: Record<string, string> = {
  '0000': '본사',
  '0001': '강남지점',
  '0002': '종로지점',
}
export const branchLabel = (code?: string | null): string =>
  code ? (BRANCH_NAME[code] ?? `지점 ${code}`) : '-'

// ─── 접근 정책 (BankRole 기준, 레거시 AdminRole 정책 대체) ────────────────────

/** 본사 직군 — 전 지점 데이터 열람 (지점 스코프 없음). */
export function isHeadOffice(roles: string[]): boolean {
  return hasAnyRole(roles, BankRole.COMPLIANCE, BankRole.HQ_REVIEWER, BankRole.HQ_RISK, BankRole.HQ_MARKETING)
}

/** PII 마스킹 대상 직군 (리스크관리). */
export function isMaskingRole(roles: string[]): boolean {
  return hasAnyRole(roles, BankRole.HQ_RISK)
}

/** 감사로그 조회 가능 — 감사/심사/지점장/창구. */
export function canViewAuditLog(roles: string[]): boolean {
  return hasAnyRole(roles, BankRole.COMPLIANCE, BankRole.HQ_REVIEWER, BankRole.BRANCH_MANAGER, BankRole.TELLER)
}

/** 연락처 등 민감정보 열람 시 조회 사유 필요 — 창구직원. */
export function requiresReason(roles: string[]): boolean {
  return hasAnyRole(roles, BankRole.TELLER) && !isHeadOffice(roles)
}

/**
 * '타 지점' 여부 — 동적 관계(역할 아님). 본사 직군이 아니고 직원 지점 ≠ 대상 고객 지점이면 true.
 * (담당(PRIMARY_OWNER)은 party_relation 연동 전까지 별도 판정 없이 지점 스코프로 근사한다)
 */
export function isOtherBranch(roles: string[], employeeBranch?: string | null, targetBranch?: string | null): boolean {
  if (isHeadOffice(roles)) return false
  if (!employeeBranch || !targetBranch) return false
  return employeeBranch !== targetBranch
}
