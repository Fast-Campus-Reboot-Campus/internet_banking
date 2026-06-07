// 데모 전용 직원 계정 목록.
//
// 운영 빌드 번들에 인증 전 직원 명단이 포함되지 않도록 admin-mock-data 에서 분리했다.
// login 화면이 DEMO_MODE 일 때만 동적 import() 로 로드하므로, 플래그 없는 운영 빌드에서는
// 이 모듈이 별도 청크로도 emit 되지 않는다(dead-code elimination).
//
// loginId 는 customer-service V11/V12 마이그레이션이 시드한 직원 계정과 1:1 매칭된다.
// 비밀번호는 데모 공통 'Employee1234!'.

import type { AdminUser } from './admin-mock-data'

export const DEMO_ACCOUNTS: AdminUser[] = [
  { id: 'A001', name: '김감사',   role: 'ROLE_HQ_AUDIT',      branchId: 'HQ',   branchName: '본사 감사부',          loginId: 'audit01'  },
  { id: 'A002', name: '이심사',   role: 'ROLE_HQ_REVIEW',     branchId: 'HQ',   branchName: '본사 심사부',          loginId: 'review01' },
  { id: 'A003', name: '박리스크', role: 'ROLE_HQ_RISK',       branchId: 'HQ',   branchName: '본사 리스크관리부',    loginId: 'risk01'   },
  { id: 'A004', name: '최마케팅', role: 'ROLE_HQ_MARKETING',  branchId: 'HQ',   branchName: '본사 마케팅/기획부',   loginId: 'mkt01'    },
  { id: 'A005', name: '정담당',   role: 'ROLE_PRIMARY_OWNER', branchId: 'B001', branchName: '강남지점',            loginId: 'owner01'  },
  { id: 'A006', name: '한직원',   role: 'ROLE_BRANCH_STAFF',  branchId: 'B001', branchName: '강남지점',            loginId: 'staff01'  },
  { id: 'A007', name: '오타지점', role: 'ROLE_OTHER_BRANCH',  branchId: 'B002', branchName: '종로지점',            loginId: 'other01'  },
  // V12 시드 — 대출 본심사 매트릭스(수동/자동 심사) 시연용. 실제 대출 게이팅은 JWT 의
  // BankRole grade(DEPUTY_MANAGER/OPS)로 동작하며, 아래 role 은 레거시 콘솔 표시용 근사값이다.
  { id: 'A008', name: '심사대리', role: 'ROLE_HQ_REVIEW',     branchId: 'HQ',   branchName: '본사 심사부',          loginId: 'deputy01' },
  { id: 'A009', name: '운영담당', role: 'ROLE_HQ_REVIEW',     branchId: 'HQ',   branchName: '본사 운영팀',          loginId: 'ops01'    },
]
