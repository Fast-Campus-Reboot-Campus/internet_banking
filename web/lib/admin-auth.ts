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
