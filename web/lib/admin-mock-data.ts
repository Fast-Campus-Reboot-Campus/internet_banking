// ─── 역할 정의 ────────────────────────────────────────────────────────────────

export type AdminRole =
  | 'ROLE_HQ_AUDIT'
  | 'ROLE_HQ_REVIEW'
  | 'ROLE_HQ_RISK'
  | 'ROLE_HQ_MARKETING'
  | 'ROLE_PRIMARY_OWNER'
  | 'ROLE_BRANCH_STAFF'
  | 'ROLE_OTHER_BRANCH'

export interface AdminUser {
  id: string
  name: string
  role: AdminRole
  branchId: string
  branchName: string
}

export const ADMIN_ACCOUNTS: AdminUser[] = [
  { id: 'A001', name: '김감사',   role: 'ROLE_HQ_AUDIT',      branchId: 'HQ',   branchName: '본사 감사부' },
  { id: 'A002', name: '이심사',   role: 'ROLE_HQ_REVIEW',     branchId: 'HQ',   branchName: '본사 심사부' },
  { id: 'A003', name: '박리스크', role: 'ROLE_HQ_RISK',       branchId: 'HQ',   branchName: '본사 리스크관리부' },
  { id: 'A004', name: '최마케팅', role: 'ROLE_HQ_MARKETING',  branchId: 'HQ',   branchName: '본사 마케팅/기획부' },
  { id: 'A005', name: '정담당',   role: 'ROLE_PRIMARY_OWNER', branchId: 'B001', branchName: '강남지점' },
  { id: 'A006', name: '한직원',   role: 'ROLE_BRANCH_STAFF',  branchId: 'B001', branchName: '강남지점' },
  { id: 'A007', name: '오타지점', role: 'ROLE_OTHER_BRANCH',  branchId: 'B002', branchName: '종로지점' },
]

export const ROLE_LABELS: Record<AdminRole, string> = {
  ROLE_HQ_AUDIT:      '감사부 (본사)',
  ROLE_HQ_REVIEW:     '심사부 (본사)',
  ROLE_HQ_RISK:       '리스크관리부 (본사)',
  ROLE_HQ_MARKETING:  '마케팅/기획부 (본사)',
  ROLE_PRIMARY_OWNER: '담당 직원 (지점)',
  ROLE_BRANCH_STAFF:  '지점 직원 (동일지점)',
  ROLE_OTHER_BRANCH:  '타 지점 직원',
}

// ─── 고객 (기존) ──────────────────────────────────────────────────────────────

export interface CustomerRecord {
  id: string; name: string; ssn: string; phone: string
  accountNumber: string; balance: number; branchId: string; branchName: string
  primaryOwnerId: string; riskScore: number; joinedAt: string
}
export interface AuditLog {
  id: string; accessorId: string; accessorName: string; accessorRole: AdminRole
  targetCustomerId: string; targetCustomerName: string; action: string
  reason: string | null; accessedAt: string; branchId: string
}

export const MOCK_CUSTOMERS: CustomerRecord[] = [
  { id:'C001', name:'홍길동', ssn:'901010-1234567', phone:'010-1234-5678', accountNumber:'123-456-789012', balance:12500000, branchId:'B001', branchName:'강남지점', primaryOwnerId:'A005', riskScore:32, joinedAt:'2019-03-15' },
  { id:'C002', name:'김영희', ssn:'850520-2345678', phone:'010-2345-6789', accountNumber:'234-567-890123', balance:87300000, branchId:'B001', branchName:'강남지점', primaryOwnerId:'A005', riskScore:15, joinedAt:'2020-07-22' },
  { id:'C003', name:'이철수', ssn:'780303-1456789', phone:'010-3456-7890', accountNumber:'345-678-901234', balance:3200000,  branchId:'B001', branchName:'강남지점', primaryOwnerId:'A005', riskScore:68, joinedAt:'2021-01-10' },
  { id:'C004', name:'박지은', ssn:'950815-2567890', phone:'010-4567-8901', accountNumber:'456-789-012345', balance:45000000, branchId:'B002', branchName:'종로지점', primaryOwnerId:'A007', riskScore:22, joinedAt:'2018-11-30' },
  { id:'C005', name:'최준호', ssn:'001224-3678901', phone:'010-5678-9012', accountNumber:'567-890-123456', balance:920000,   branchId:'B002', branchName:'종로지점', primaryOwnerId:'A007', riskScore:81, joinedAt:'2023-05-01' },
  { id:'C006', name:'정미래', ssn:'880912-2789012', phone:'010-6789-0123', accountNumber:'678-901-234567', balance:230000000, branchId:'B001', branchName:'강남지점', primaryOwnerId:'A005', riskScore:9, joinedAt:'2017-06-14' },
]
export const MOCK_AUDIT_LOGS: AuditLog[] = [
  { id:'L001', accessorId:'A005', accessorName:'정담당',   accessorRole:'ROLE_PRIMARY_OWNER', targetCustomerId:'C001', targetCustomerName:'홍길동', action:'고객 상세 조회',   reason:null,              accessedAt:'2026-05-25 09:12:34', branchId:'B001' },
  { id:'L002', accessorId:'A006', accessorName:'한직원',   accessorRole:'ROLE_BRANCH_STAFF',  targetCustomerId:'C002', targetCustomerName:'김영희', action:'자산 현황 조회',   reason:'대출 상담 요청',  accessedAt:'2026-05-25 09:45:11', branchId:'B001' },
  { id:'L003', accessorId:'A001', accessorName:'김감사',   accessorRole:'ROLE_HQ_AUDIT',      targetCustomerId:'C005', targetCustomerName:'최준호', action:'거래내역 전체 조회', reason:null,            accessedAt:'2026-05-25 10:03:22', branchId:'HQ'   },
  { id:'L004', accessorId:'A006', accessorName:'한직원',   accessorRole:'ROLE_BRANCH_STAFF',  targetCustomerId:'C003', targetCustomerName:'이철수', action:'상담 이력 조회',   reason:'이체 오류 민원',  accessedAt:'2026-05-25 10:30:55', branchId:'B001' },
  { id:'L005', accessorId:'A002', accessorName:'이심사',   accessorRole:'ROLE_HQ_REVIEW',     targetCustomerId:'C005', targetCustomerName:'최준호', action:'대출 신청 심사',   reason:null,              accessedAt:'2026-05-25 11:15:08', branchId:'HQ'   },
  { id:'L006', accessorId:'A005', accessorName:'정담당',   accessorRole:'ROLE_PRIMARY_OWNER', targetCustomerId:'C006', targetCustomerName:'정미래', action:'고객 상세 조회',   reason:null,              accessedAt:'2026-05-25 11:48:33', branchId:'B001' },
  { id:'L007', accessorId:'A003', accessorName:'박리스크', accessorRole:'ROLE_HQ_RISK',       targetCustomerId:'C005', targetCustomerName:'최준호', action:'리스크 스코어 조회', reason:null,            accessedAt:'2026-05-25 13:22:17', branchId:'HQ'   },
]

// ─── 마스킹 유틸 ──────────────────────────────────────────────────────────────

export function applyMasking(customer: CustomerRecord, role: AdminRole) {
  switch (role) {
    case 'ROLE_HQ_AUDIT': case 'ROLE_PRIMARY_OWNER': return { ...customer }
    case 'ROLE_HQ_REVIEW':    return { ...customer, ssn: customer.ssn.slice(0,7)+'-*******', phone: customer.phone.slice(0,9)+'****' }
    case 'ROLE_HQ_RISK':      return { ...customer, name: customer.name[0]+'*'.repeat(customer.name.length-1), ssn:'******-*******', phone:customer.phone.slice(0,4)+'-****-****', accountNumber:customer.accountNumber.slice(0,4)+'-***-******' }
    case 'ROLE_HQ_MARKETING': return { ...customer, name: customer.name[0]+'*'.repeat(customer.name.length-1), ssn:'******-*******', phone:'010-****-****', accountNumber:'***-***-******' }
    case 'ROLE_BRANCH_STAFF': return { ...customer, ssn:'******-*******' }
    case 'ROLE_OTHER_BRANCH': return null
  }
}
export function canViewAuditLog(role: AdminRole) { return ['ROLE_HQ_AUDIT','ROLE_HQ_REVIEW','ROLE_PRIMARY_OWNER','ROLE_BRANCH_STAFF'].includes(role) }
export function requiresReason(role: AdminRole)  { return role === 'ROLE_BRANCH_STAFF' }

// ─── A-C-001 제재대상 스크리닝 ────────────────────────────────────────────────

export type ScreeningStatus = '검토대기' | '검토중' | '승인' | '거절'
export type HitType = 'OFAC SDN 매칭' | '국내 PEP' | 'UN 제재명단' | 'EU 제재'
export interface ScreeningRecord {
  id: string; customerId: string; name: string; birthDate: string
  nationality: string; hitType: HitType; matchRate: number
  detectedAt: string; status: ScreeningStatus; reviewer: string | null
}
export const MOCK_SCREENINGS: ScreeningRecord[] = [
  { id:'C0012345', customerId:'C0012345', name:'김**', birthDate:'1985-03-15', nationality:'KR', hitType:'OFAC SDN 매칭', matchRate:92, detectedAt:'2026-05-10 14:23', status:'검토대기', reviewer:null },
  { id:'C0012346', customerId:'C0012346', name:'박**', birthDate:'1978-11-02', nationality:'KR', hitType:'국내 PEP',      matchRate:87, detectedAt:'2026-05-10 11:08', status:'검토중',   reviewer:'김검원' },
  { id:'C0012347', customerId:'C0012347', name:'이**', birthDate:'1991-06-22', nationality:'US', hitType:'UN 제재명단',   matchRate:95, detectedAt:'2026-05-09 16:55', status:'검토대기', reviewer:null },
  { id:'C0012348', customerId:'C0012348', name:'정**', birthDate:'1965-02-08', nationality:'KR', hitType:'국내 PEP',      matchRate:81, detectedAt:'2026-05-09 09:14', status:'승인',     reviewer:'이심사' },
  { id:'C0012349', customerId:'C0012349', name:'최**', birthDate:'1988-12-30', nationality:'CN', hitType:'EU 제재',       matchRate:89, detectedAt:'2026-05-08 13:42', status:'거절',     reviewer:'김검원' },
  { id:'C0012350', customerId:'C0012350', name:'장**', birthDate:'1972-07-19', nationality:'KR', hitType:'국내 PEP',      matchRate:84, detectedAt:'2026-05-08 10:20', status:'검토대기', reviewer:null },
]

// ─── A-C-002 EDD 심사 ─────────────────────────────────────────────────────────

export type EDDType = '고위험 국가' | 'PEP' | '고액거래'
export type EDDStatus = '심사' | '서류요청'
export interface EDDRecord {
  id: string; customerId: string; name: string; eddType: EDDType
  fundSource: boolean; jobIncome: boolean; transactionPurpose: boolean
  realOwner: boolean; submittedAt: string; docCount: string; status: EDDStatus
  detail?: { cddRisk: string; assignedTo: string }
}
export const MOCK_EDD: EDDRecord[] = [
  { id:'EDD-2026-0148', customerId:'C019234', name:'김**', eddType:'고위험 국가', fundSource:true, jobIncome:true, transactionPurpose:true, realOwner:false, submittedAt:'2026-05-10', docCount:'3/3', status:'심사', detail:{ cddRisk:'고위험 (자동 산정)', assignedTo:'컴플라이언스팀 김** (책임자: 박**)' } },
  { id:'EDD-2026-0147', customerId:'C019233', name:'박**', eddType:'PEP',        fundSource:true, jobIncome:false, transactionPurpose:true, realOwner:false, submittedAt:'2026-05-10', docCount:'2/3', status:'서류요청' },
  { id:'EDD-2026-0146', customerId:'C019230', name:'(주)**김**', eddType:'고액거래', fundSource:true, jobIncome:true, transactionPurpose:true, realOwner:true, submittedAt:'2026-05-09', docCount:'4/4', status:'심사' },
  { id:'EDD-2026-0145', customerId:'C019228', name:'이**', eddType:'PEP',        fundSource:true, jobIncome:true, transactionPurpose:true, realOwner:false, submittedAt:'2026-05-08', docCount:'3/3', status:'심사' },
]

// ─── A-C-003 중복 고객 ───────────────────────────────────────────────────────

export type DupMatchType = '이름+생년월일' | 'CI 충돌'
export type DupStatus = '검토대기' | '검토중' | '복본'
export interface DuplicateRecord {
  id: string; newCustomerId: string; existingCustomerId: string
  name: string; birthDate: string; matchType: DupMatchType
  detectedAt: string; status: DupStatus
}
export const MOCK_DUPLICATES: DuplicateRecord[] = [
  { id:'DUP-2026-0093', newCustomerId:'C0019501', existingCustomerId:'C0008273', name:'김민수', birthDate:'1985-03-15', matchType:'이름+생년월일', detectedAt:'2026-05-10', status:'검토대기' },
  { id:'DUP-2026-0092', newCustomerId:'C0019487', existingCustomerId:'C0005412', name:'이지은', birthDate:'1990-08-22', matchType:'이름+생년월일', detectedAt:'2026-05-10', status:'검토대기' },
  { id:'DUP-2026-0091', newCustomerId:'C0019472', existingCustomerId:'C0003251', name:'박철수', birthDate:'1978-11-02', matchType:'CI 충돌',     detectedAt:'2026-05-09', status:'복본' },
]

// ─── A-C-004 실명확인 증표 위변조 ────────────────────────────────────────────

export type DocType = '주민등록증' | '운전면허증' | '여권 (US)'
export type DocStatus = '검토대기' | '검토중' | '거절'
export interface IDVerifyRecord {
  id: string; customerId: string; name: string; docType: DocType
  maskedDocNumber: string; suspicionType: string; apiResult: string
  submittedAt: string; status: DocStatus; aiScore?: number
}
export const MOCK_ID_VERIFY: IDVerifyRecord[] = [
  { id:'DOC-2026-0212', customerId:'C019650', name:'김**', docType:'주민등록증', maskedDocNumber:'850315-1******', suspicionType:'정보부 진위확인 실패', apiResult:'NOT_FOUND', submittedAt:'2026-05-10', status:'검토대기', aiScore:0.87 },
  { id:'DOC-2026-0211', customerId:'C019648', name:'박**', docType:'운전면허증', maskedDocNumber:'11-23-*******', suspicionType:'OCR 불일치',         apiResult:'OK',         submittedAt:'2026-05-10', status:'검토대기' },
  { id:'DOC-2026-0210', customerId:'C019642', name:'Smith J.', docType:'여권 (US)', maskedDocNumber:'52*****',       suspicionType:'이미지 조작 의심',  apiResult:'OK (API)',   submittedAt:'2026-05-09', status:'검토중' },
]

// ─── A-C-005 얼굴인증 라우팅 ─────────────────────────────────────────────────

export type FaceRoutingStatus = '대기' | '영업점 배정' | '완료 (대면확인)'
export interface FaceRoutingRecord {
  id: string; customerId: string; name: string; failureType: string
  similarityScore: number; liveness: string; failedAt: string
  nearestBranch: string; status: FaceRoutingStatus
}
export const MOCK_FACE_ROUTING: FaceRoutingRecord[] = [
  { id:'FACE-2026-0521', customerId:'C019712', name:'김**', failureType:'유사도 미달',    similarityScore:0.62, liveness:'통과', failedAt:'2026-05-11 09:23', nearestBranch:'강남역지점', status:'대기' },
  { id:'FACE-2026-0520', customerId:'C019710', name:'박**', failureType:'딥페이크 의심',  similarityScore:0.91, liveness:'실패', failedAt:'2026-05-11 08:45', nearestBranch:'여의도본점', status:'영업점 배정' },
  { id:'FACE-2026-0519', customerId:'C019705', name:'이**', failureType:'유사도 미달',    similarityScore:0.71, liveness:'통과', failedAt:'2026-05-10 17:12', nearestBranch:'판교테크노밸리', status:'완료 (대면확인)' },
]

// ─── A-C-006 대리인 위임장 ───────────────────────────────────────────────────

export type AgentStatus = '검토대기' | '거절 (인감 위조 의심)' | '승인 (권한 액팅)'
export interface AgentRecord {
  id: string; ownerId: string; agentName: string; relationship: string
  delegationType: string; scope: string; documents: string
  submittedAt: string; status: AgentStatus
}
export const MOCK_AGENTS: AgentRecord[] = [
  { id:'AGT-2026-0042', ownerId:'C019801', agentName:'김**', relationship:'배우자', delegationType:'임의대리', scope:'조회+이체', documents:'위임장/민감/가족관계증명서', submittedAt:'2026-05-10', status:'검토대기' },
  { id:'AGT-2026-0041', ownerId:'C019795', agentName:'박**', relationship:'자녀',   delegationType:'법정대리', scope:'전체',     documents:'신분증/가족관계증명서',       submittedAt:'2026-05-09', status:'검토대기' },
  { id:'AGT-2026-0040', ownerId:'C019789', agentName:'이**', relationship:'지인',   delegationType:'임의대리', scope:'조회',     documents:'위임장/민감',               submittedAt:'2026-05-08', status:'거절 (인감 위조 의심)' },
]

// ─── A-C-007 미성년자 ────────────────────────────────────────────────────────

export type MinorStatus = '검토대기' | '거절'
export interface MinorRecord {
  id: string; minorName: string; age: number; guardianName: string
  relationship: string; relationshipCheck: string
  guardianVerified: boolean; submittedAt: string; status: MinorStatus
}
export const MOCK_MINORS: MinorRecord[] = [
  { id:'MIN-2026-0078', minorName:'김**', age:12, guardianName:'김** (부)', relationship:'부', relationshipCheck:'일치 (가족관계증명)', guardianVerified:true,  submittedAt:'2026-05-10', status:'검토대기' },
  { id:'MIN-2026-0077', minorName:'박**', age:8,  guardianName:'박** (모)', relationship:'모', relationshipCheck:'일치',               guardianVerified:true,  submittedAt:'2026-05-10', status:'검토대기' },
  { id:'MIN-2026-0076', minorName:'이**', age:10, guardianName:'이** (조부)', relationship:'조부', relationshipCheck:'관계 불일치',    guardianVerified:true,  submittedAt:'2026-05-09', status:'거절' },
]

// ─── A-C-101 회원 목록 ───────────────────────────────────────────────────────

export type MemberStatus = '활성' | '휴면' | '정지' | '탈퇴'
export type RiskLevel = '저' | '중' | '고'
export interface MemberRecord {
  id: string; name: string; birthDate: string; phone: string
  memberType: string; status: MemberStatus; riskLevel: RiskLevel
  lastLogin: string; joinedAt: string
}
export const MOCK_MEMBERS: MemberRecord[] = [
  { id:'C0019812', name:'김**', birthDate:'1985-03-15', phone:'010-****-1234', memberType:'뱅킹이체회원', status:'활성', riskLevel:'저', lastLogin:'2026-05-11 09:23', joinedAt:'2018-04-12' },
  { id:'C0019811', name:'박**', birthDate:'1990-08-22', phone:'010-****-5678', memberType:'뱅킹이체회원', status:'활성', riskLevel:'중', lastLogin:'2026-05-10 18:45', joinedAt:'2020-11-03' },
  { id:'C0019810', name:'이**', birthDate:'1978-11-02', phone:'010-****-9012', memberType:'조회용ID 회원', status:'휴면', riskLevel:'저', lastLogin:'2024-08-15 14:12', joinedAt:'2017-02-28' },
  { id:'C0019809', name:'정**', birthDate:'1965-02-08', phone:'010-****-3456', memberType:'뱅킹이체회원', status:'정지', riskLevel:'고', lastLogin:'2025-12-20 11:08', joinedAt:'2015-07-19' },
  { id:'C0019808', name:'최**', birthDate:'1991-06-22', phone:'010-****-7890', memberType:'뱅킹이체회원', status:'활성', riskLevel:'저', lastLogin:'2026-05-11 07:51', joinedAt:'2022-09-14' },
  { id:'C0019807', name:'장**', birthDate:'1972-07-19', phone:'010-****-2345', memberType:'뱅킹이체회원', status:'활성', riskLevel:'중', lastLogin:'2026-05-09 22:34', joinedAt:'2019-03-22' },
  { id:'C0019806', name:'윤**', birthDate:'1988-12-30', phone:'010-****-6789', memberType:'뱅킹이체회원', status:'활성', riskLevel:'저', lastLogin:'2026-05-10 16:00', joinedAt:'2021-05-08' },
]

// ─── A-C-103 회원 상태 관리 ──────────────────────────────────────────────────

export interface StatusChangeRecord {
  changedAt: string; customerId: string; name: string
  fromStatus: MemberStatus; toStatus: MemberStatus
  reason: string; processor: string; note: string
}
export const MOCK_STATUS_CHANGES: StatusChangeRecord[] = [
  { changedAt:'2026-05-11 09:14', customerId:'C0019812', name:'김**', fromStatus:'활성', toStatus:'정지', reason:'사고 의심 (이상거래)', processor:'김검원', note:'FDS Alert #2023' },
  { changedAt:'2026-05-10 16:42', customerId:'C0019745', name:'박**', fromStatus:'휴면', toStatus:'활성', reason:'고객 요청 (로그인)', processor:'자동', note:'5년만에 재로그인' },
  { changedAt:'2026-05-10 11:05', customerId:'C0019702', name:'이**', fromStatus:'활성', toStatus:'정지', reason:'제재 조치 (OFAC Hit)', processor:'이심사', note:'스크리닝 결과' },
  { changedAt:'2026-05-09 14:30', customerId:'C0019650', name:'정**', fromStatus:'활성', toStatus:'탈퇴', reason:'고객 요청', processor:'자동', note:'온라인 탈퇴 신청' },
]

// ─── A-C-104 약관 관리 ───────────────────────────────────────────────────────

export type TermStatus = '적용중' | '예고 발송'
export interface TermRecord {
  id: string; name: string; type: '필수' | '선택'; scope: string
  version: string; effectiveDate: string; status: TermStatus; consentCount: number
}
export const MOCK_TERMS: TermRecord[] = [
  { id:'TC-001', name:'전자금융거래기본약관',        type:'필수', scope:'가입',     version:'v4.2', effectiveDate:'2024-09-01', status:'적용중',   consentCount:2189402 },
  { id:'TC-002', name:'전자금융서비스이용약관',       type:'필수', scope:'가입',     version:'v3.8', effectiveDate:'2024-09-01', status:'적용중',   consentCount:2189402 },
  { id:'TC-003', name:'개인(신용)정보 수집·이용 동의서', type:'필수', scope:'가입',  version:'v5.1', effectiveDate:'2025-01-15', status:'적용중',   consentCount:2165022 },
  { id:'TC-101', name:'[은행] 마케팅 활용 및 광고성 수신 동의', type:'선택', scope:'마케팅', version:'v2.3', effectiveDate:'2025-03-01', status:'적용중', consentCount:1432887 },
  { id:'TC-102', name:'[계열사] 마케팅 활용 및 광고성 수신 동의', type:'선택', scope:'마케팅', version:'v2.1', effectiveDate:'2025-03-01', status:'적용중', consentCount:987432 },
  { id:'TC-005', name:'전자금융거래기본약관 (v4.3 개정안)', type:'필수', scope:'가입', version:'v4.3 (예정)', effectiveDate:'2026-06-01', status:'예고 발송', consentCount:0 },
]

// ─── A-C-105 동의이력 ────────────────────────────────────────────────────────

export interface ConsentLog {
  id: string; date: string; customerId: string; termId: string
  termVersion: string; termName: string; consentType: '동의' | '미동의' | '철회'
  ip: string; device: string; hash: string
}
export const MOCK_CONSENT_LOGS: ConsentLog[] = [
  { id:'LOG-26050923847', date:'2026-05-09 14:23:08', customerId:'C0019812', termId:'TC-001', termVersion:'v4.2', termName:'전자금융거래기본약관', consentType:'동의',  ip:'192.168.*.*', device:'iOS 17.4 / Safari', hash:'a8f3...c921' },
  { id:'LOG-26050923846', date:'2026-05-09 14:23:08', customerId:'C0019812', termId:'TC-002', termVersion:'v3.8', termName:'전자금융서비스이용약관', consentType:'동의',  ip:'192.168.*.*', device:'iOS 17.4 / Safari', hash:'b2e7...d445' },
  { id:'LOG-26050923845', date:'2026-05-09 14:23:08', customerId:'C0019812', termId:'TC-003', termVersion:'v5.1', termName:'개인(신용)정보 수집·이용 동의서', consentType:'동의', ip:'192.168.*.*', device:'iOS 17.4 / Safari', hash:'c1d9...e823' },
  { id:'LOG-26050923844', date:'2026-05-09 14:22:51', customerId:'C0019812', termId:'TC-101', termVersion:'v2.3', termName:'[은행] 마케팅 동의', consentType:'미동의', ip:'192.168.*.*', device:'iOS 17.4 / Safari', hash:'d3a2...f917' },
  { id:'LOG-26050823811', date:'2026-05-08 11:14:22', customerId:'C0019789', termId:'TC-101', termVersion:'v2.3', termName:'[은행] 마케팅 동의', consentType:'철회',   ip:'10.0.*.*',    device:'Windows / Chrome', hash:'e4b3...g021' },
]

// ─── A-C-106 FATCA/CRS ───────────────────────────────────────────────────────

export type FATCAType = 'FATCA' | 'CRS' | '비합조'
export interface FATCARecord {
  customerId: string; name: string; nationality: string; taxCountry: string
  tin: string; type: FATCAType; w9Status: string
  isSubmitted: boolean; submittedAt: string; reportDeadline: string
}
export const MOCK_FATCA: FATCARecord[] = [
  { customerId:'C0019812', name:'김**', nationality:'KR', taxCountry:'US', tin:'SSN ***-**-1234', type:'FATCA',  w9Status:'W-9',  isSubmitted:true,  submittedAt:'2026-04-12', reportDeadline:'2026-09-30' },
  { customerId:'C0019799', name:'Smith J.', nationality:'US', taxCountry:'US', tin:'SSN ***-**-5678', type:'FATCA', w9Status:'W-9', isSubmitted:true, submittedAt:'2026-04-10', reportDeadline:'2026-09-30' },
  { customerId:'C0019712', name:'Tanaka Y.', nationality:'JP', taxCountry:'JP', tin:'TIN *********9012', type:'CRS', w9Status:'자가증명서', isSubmitted:true, submittedAt:'2026-04-08', reportDeadline:'2026-09-30' },
  { customerId:'C0019650', name:'Wang L.', nationality:'CN', taxCountry:'CN', tin:'-', type:'비합조', w9Status:'미제출', isSubmitted:false, submittedAt:'-', reportDeadline:'자동 보고대상 분류' },
]

// ─── A-C-107 가입 현황 대시보드 ───────────────────────────────────────────────

export const JOIN_STATS = {
  todayApplications: 1247,
  todayCompleted: 1089,
  completionRate: 87.3,
  pendingReview: 158,
  rejectionRate: 2.1,
  funnel: [
    { step:'1. 약관 동의',    entered:9847, completed:9520, dropRate:3.3, mainReason:'약관 검토 부담' },
    { step:'2. 본인확인',     entered:9520, completed:9012, dropRate:5.3, mainReason:'휴대폰 인증 실패' },
    { step:'3. 실명확인 증표', entered:9012, completed:8234, dropRate:8.6, mainReason:'증표 촬영 어려움' },
    { step:'4. 얼굴인증',     entered:8234, completed:7891, dropRate:4.2, mainReason:'유사도 미달 → 영업점' },
    { step:'5. 회원정보 입력', entered:7891, completed:7650, dropRate:3.1, mainReason:'주소 입력 번거로움' },
    { step:'6. 인증수단 등록', entered:7650, completed:7542, dropRate:1.4, mainReason:'-' },
    { step:'7. 가입 완료',    entered:7542, completed:7489, dropRate:0.7, mainReason:'-' },
  ],
  channels: [
    { channel:'앱 (스타뱅킹)', count:876, rate:70.2 },
    { channel:'웹', count:312, rate:25.0 },
    { channel:'영업점', count:59, rate:4.8 },
  ],
  alerts: ['△ 제재 Hit 5건 검토 대기 (4시간 초과)', '△ EDD 심사대기 8건', '↑ 얼굴인증 실패율 평균 대비 +2.1%'],
}

// ─── A-M-001 이벤트 ──────────────────────────────────────────────────────────

export type EventStatus = '진행중' | '마감임박' | '종료'
export interface EventRecord {
  id: string; name: string; type: string; period: string
  target: string; applicantCount: number; prize: string; status: EventStatus
}
export const MOCK_EVENTS: EventRecord[] = [
  { id:'EV-2026-024', name:'케이봇뱀 포트폴리오 고객 챌린지', type:'응모형', period:'05.04~06.30', target:'신규 포트폴리오 가입자', applicantCount:235, prize:'호텔숙박권/네블릿', status:'진행중' },
  { id:'EV-2026-023', name:'KB X KB증권 한번에 ABLE',       type:'미션',   period:'04.27~05.31', target:'전체',              applicantCount:402, prize:'5만원 상품권',    status:'마감임박' },
  { id:'EV-2026-022', name:'SSG.COM 환전 이벤트',           type:'응모형', period:'04.29~06.30', target:'외환 거래 고객',      applicantCount:546, prize:'네이탁',          status:'진행중' },
  { id:'EV-2026-021', name:'KB국민인증서 무료 발급',         type:'가입 이벤트', period:'04.23~05.22', target:'인증서 미발급자', applicantCount:595, prize:'스타벅스 쿠폰',  status:'종료' },
  { id:'EV-2026-020', name:'첫만남 첫상품 쿠폰팩',           type:'가입 이벤트', period:'04.01~06.30', target:'신규 가입자',    applicantCount:502, prize:'4만원 쿠폰팩',   status:'진행중' },
  { id:'EV-2026-019', name:'개인사업자 신용대출 이자 지원',   type:'응모형', period:'03.18~05.15', target:'개인사업자',          applicantCount:4369, prize:'이자 환급',     status:'마감임박' },
]

// ─── A-M-003 응모자 ──────────────────────────────────────────────────────────

export interface ApplicantRecord {
  id: string; customerId: string; name: string; phone: string
  appliedAt: string; product: string; joinAmount: number
  autoTransferAmount: number; conditionMet: boolean
}
export const MOCK_APPLICANTS: ApplicantRecord[] = [
  { id:'AP-26050024-0187', customerId:'C0019812', name:'김**', phone:'010-****-1234', appliedAt:'2026-05-10 14:23', product:'케이봇뱀 밋충형', joinAmount:500000, autoTransferAmount:200000, conditionMet:true },
  { id:'AP-26050024-0186', customerId:'C0019811', name:'박**', phone:'010-****-5678', appliedAt:'2026-05-10 11:08', product:'케이봇뱀 AI',     joinAmount:300000, autoTransferAmount:150000, conditionMet:true },
  { id:'AP-26050024-0185', customerId:'C0019810', name:'이**', phone:'010-****-9012', appliedAt:'2026-05-10 09:47', product:'케이봇뱀 로보뱀', joinAmount:250000, autoTransferAmount:100000, conditionMet:true },
  { id:'AP-26050024-0184', customerId:'C0019809', name:'정**', phone:'010-****-3456', appliedAt:'2026-05-09 16:55', product:'케이봇뱀 테마',   joinAmount:150000, autoTransferAmount:50000,  conditionMet:false },
]

// ─── A-M-004 당첨자 ──────────────────────────────────────────────────────────

export type WinnerStatus = '발송완료' | '발송대기' | '주소 확인중'
export interface WinnerRecord {
  id: string; rank: string; customerId: string; name: string
  phone: string; prize: string; paymentMethod: string
  paymentDate: string; status: WinnerStatus
}
export const MOCK_WINNERS: WinnerRecord[] = [
  { id:'WN-26050024-001', rank:'1등', customerId:'C0019812', name:'김**', phone:'010-****-1234', prize:'신라호텔 숙박권', paymentMethod:'모바일쿠폰', paymentDate:'2026-05-15', status:'발송완료' },
  { id:'WN-26050024-002', rank:'1등', customerId:'C0019799', name:'박**', phone:'010-****-5678', prize:'신라호텔 숙박권', paymentMethod:'모바일쿠폰', paymentDate:'2026-05-15', status:'발송대기' },
  { id:'WN-26050024-006', rank:'2등', customerId:'C0019712', name:'이**', phone:'010-****-9012', prize:'iPad mini',     paymentMethod:'택배',       paymentDate:'2026-05-18', status:'주소 확인중' },
]

// ─── A-M-005 배너 ────────────────────────────────────────────────────────────

export type BannerStatus = '노출중' | '종료예정' | '종료'
export interface BannerRecord {
  id: string; color: string; title: string; position: string
  period: string; linkUrl: string; clickCount: number; status: BannerStatus
}
export const MOCK_BANNERS: BannerRecord[] = [
  { id:'BN-26-051', color:'#F59E0B', title:'여섯시은행 9To6 Bank',    position:'웹·앱 메인', period:'05.01~06.30', linkUrl:'/branch/9to6',         clickCount:12847, status:'노출중' },
  { id:'BN-26-052', color:'#10B981', title:'주거취약계층 이주비 대출',  position:'웹·앱 메인', period:'04.20~07.31', linkUrl:'/loan/welfare',         clickCount:8420,  status:'노출중' },
  { id:'BN-26-053', color:'#FDE68A', title:'2026 KB굿잡 박람회',      position:'웹 메인',   period:'04.15~05.31', linkUrl:'/event/goodjob',        clickCount:5231,  status:'노출중' },
  { id:'BN-26-054', color:'#FECDD3', title:'케이봇뱀 챌린지',         position:'앱 메인',   period:'05.04~06.30', linkUrl:'/event/EV-2026-024',    clickCount:3012,  status:'노출중' },
  { id:'BN-26-050', color:'#E5E7EB', title:'(예정) ABLE 이벤트',      position:'웹 메인',   period:'04.27~05.31', linkUrl:'/event/EV-2026-023',    clickCount:2304,  status:'종료예정' },
]

// ─── A-M-006 마케팅 발송 ─────────────────────────────────────────────────────

export type CampaignStatus = '예약' | '진행중' | '완료'
export interface CampaignRecord {
  id: string; name: string; channel: 'SMS' | '이메일' | '앱 푸시'
  targetSegment: string; targetCount: number
  scheduledAt: string; result: string | null; status: CampaignStatus
}
export const MOCK_CAMPAIGNS: CampaignRecord[] = [
  { id:'CP-2026-098', name:'케이봇뱀 챌린지 안내', channel:'SMS',   targetSegment:'마케팅 동의 + 투자 관심 고객', targetCount:147832,   scheduledAt:'2026-05-12 10:00 (예약)', result:null,                           status:'예약' },
  { id:'CP-2026-097', name:'여섯시은행 안내',       channel:'앱 푸시', targetSegment:'전체 앱 사용자',            targetCount:1892304,  scheduledAt:'2026-05-10 14:00',        result:'발송 1,820,112 / 실패 72,192', status:'완료' },
  { id:'CP-2026-096', name:'FATCA 자가증명 안내',   channel:'이메일', targetSegment:'FATCA 대상 미제출자',         targetCount:412,      scheduledAt:'2026-05-09 09:00',        result:'발송 412 / 오픈율 67.3%',      status:'완료' },
  { id:'CP-2026-095', name:'9To6 Bank 오픈 안내',   channel:'SMS',   targetSegment:'특화 지점 근방 거주 고객',     targetCount:87231,    scheduledAt:'2026-05-08 11:00',        result:'발송 87,231 / 도달 86,789',    status:'완료' },
]

// ─── A-M-007 마케팅 통계 ─────────────────────────────────────────────────────

export const MARKETING_STATS = {
  totalSent: 3400000,
  avgOpenRate: 42.7,
  avgConversionRate: 3.8,
  optOutRate: 0.4,
  channels: [
    { channel:'SMS',    sent:1832401, deliveryRate:99.2, openClickRate:78.3, conversionRate:4.2 },
    { channel:'이메일', sent:987234,  deliveryRate:97.8, openClickRate:42.7, conversionRate:2.8 },
    { channel:'앱 푸시', sent:612892, deliveryRate:89.4, openClickRate:32.1, conversionRate:5.1 },
  ],
  topEvents: [
    { name:'개인사업자 신용대출 이자 지원', applicants:4369, conversionRate:12.4 },
    { name:'SSG.COM 환전 이벤트',          applicants:546,  conversionRate:3.2  },
    { name:'KB국민인증서 무료 발급',        applicants:595,  conversionRate:8.1  },
    { name:'첫만남 첫상품 쿠폰팩',          applicants:502,  conversionRate:5.7  },
    { name:'KB X KB증권 한번에 ABLE',      applicants:402,  conversionRate:4.3  },
  ],
  consent: [
    { label:'[은행] 마케팅', count:1432887, rate:61.2 },
    { label:'[계열사] 마케팅', count:987432, rate:42.2 },
    { label:'[오픈뱅킹] 혜택', count:654210, rate:28.0 },
  ],
}
