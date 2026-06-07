// 데모 전용 직원 계정 목록 (로그인 빠른 입력용).
//
// 운영 빌드 번들에 인증 전 직원 명단이 포함되지 않도록 admin-mock-data 에서 분리했다.
// login 화면이 DEMO_MODE 일 때만 동적 import() 로 로드하므로, 플래그 없는 운영 빌드에서는
// 이 모듈이 별도 청크로도 emit 되지 않는다(dead-code elimination).
//
// 역할은 로그인 후 JWT(BankRole)에서 결정되므로 여기엔 표시용 메타데이터(이름·설명)만 둔다.
// loginId 는 customer-service 시드(V3/V11/V12) 직원 계정과 1:1. 비밀번호는 데모 공통 'Employee1234!'.

export type DemoAccount = {
  /** 백엔드 직원 계정 loginId */
  loginId: string
  /** 칩 표시 이름 */
  name: string
  /** 칩 tooltip 설명(역할 힌트) */
  desc: string
}

export const DEMO_ACCOUNTS: DemoAccount[] = [
  { loginId: 'audit01',    name: '김감사',   desc: '컴플라이언스/감사 (본사)' },
  { loginId: 'review01',   name: '이심사',   desc: '본사 심사' },
  { loginId: 'risk01',     name: '박리스크', desc: '리스크관리 (PII 마스킹)' },
  { loginId: 'employee01', name: '박상우',   desc: '지점장 — 대출 최종결재·상신' },
  { loginId: 'owner01',    name: '정담당',   desc: '창구직원 (담당)' },
  { loginId: 'staff01',    name: '한직원',   desc: '창구직원' },
  { loginId: 'other01',    name: '오타지점', desc: '타 지점 창구직원' },
  // 대출 본심사 매트릭스(수동/자동 심사) 시연용.
  { loginId: 'deputy01',   name: '심사대리', desc: '부지점장 — 수동 심사' },
  { loginId: 'ops01',      name: '운영담당', desc: '운영 — 자동 심사·EOD' },
]
