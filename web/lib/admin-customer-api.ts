// 고객·인증·컴플라이언스 직원용(admin) API 클라이언트.
// 게이트웨이(8080) 경유로 customer-service /api/v1/internal/** 를 호출한다.
// 인증: api.ts 인터셉터가 localStorage('accessToken')의 JWT를 Bearer로 첨부 → 게이트웨이가
// X-User-Role 주입 → customer-service가 역할 게이팅. (직원 역할 JWT로 로그인돼 있어야 함)

import { api } from './api'

// ── 공통 ────────────────────────────────────────────────────────────────────

/** Spring Data Page 직렬화 형태 */
export type Page<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

function unwrap<T>(res: { data: { data: T } }): T {
  return res.data.data
}

/** ISO 일시 → YYYY-MM-DD */
export const fmtDate = (v: string | null | undefined): string => (v ? v.slice(0, 10) : '-')
/** ISO 일시 → YYYY-MM-DD HH:mm */
export const fmtDateTime = (v: string | null | undefined): string =>
  v ? v.slice(0, 16).replace('T', ' ') : '-'
/** YYYYMMDD → YYYY-MM-DD */
export const fmtYmd = (v: string | null | undefined): string =>
  v && v.length === 8 ? `${v.slice(0, 4)}-${v.slice(4, 6)}-${v.slice(6, 8)}` : (v || '-')
/** API 에러 → 사용자 메시지 (403은 권한 안내) */
export function errMsg(e: unknown, fallback = '데이터를 불러오지 못했습니다.'): string {
  const status = (e as { response?: { status?: number } })?.response?.status
  return status === 403 ? '접근 권한이 없습니다 (직원 역할 필요).' : fallback
}

/** customer_status_code → 화면 라벨 */
export const STATUS_LABEL: Record<string, string> = {
  ACTIVE: '활성',
  DORMANT: '휴면',
  SUSPENDED: '정지',
  CLOSED: '탈퇴',
}
/** 화면 라벨 → customer_status_code (검색 필터용) */
export const STATUS_CODE: Record<string, string> = {
  활성: 'ACTIVE',
  휴면: 'DORMANT',
  정지: 'SUSPENDED',
  탈퇴: 'CLOSED',
}

// ── 회원/고객 (customer) ─────────────────────────────────────────────────────

export type CustomerSummary = {
  customerId: number
  partyId: number
  partyName: string
  phone: string | null
  email: string | null
  customerGradeCode: string | null
  customerStatusCode: string
  joinedAt: string | null
  lastTransactionAt: string | null
}

export type CustomerDetail = CustomerSummary & {
  creditRatingCode: string | null
  creditEvaluationDate: string | null
  zipCode: string | null
  address: string | null
  addressDetail: string | null
  joinChannelCode: string | null
  firstJoinDate: string | null
  dormantAt: string | null
  closedAt: string | null
  partyStatusCode: string | null
  birthDate: string | null
  genderCode: string | null
  nationalityCode: string | null
  pep: boolean | null
}

export type JoinStats = {
  total: number
  joinedToday: number
  joinedThisMonth: number
  byStatus: { code: string | null; count: number }[]
  byGrade: { code: string | null; count: number }[]
  byChannel: { code: string | null; count: number }[]
}

export async function searchCustomers(params: {
  keyword?: string
  status?: string
  grade?: string
  page?: number
  size?: number
}): Promise<Page<CustomerSummary>> {
  return unwrap(await api.get('/api/v1/internal/customers', { params }))
}

export async function getCustomerDetail(customerId: number): Promise<CustomerDetail> {
  return unwrap(await api.get(`/api/v1/internal/customers/${customerId}`))
}

export async function getJoinStats(): Promise<JoinStats> {
  return unwrap(await api.get('/api/v1/internal/customers/join-stats'))
}

// ── 회원 상태·등급 변경 (PATCH) ───────────────────────────────────────────────

export async function changeGrade(customerId: number, body: {
  newGradeCode: string; reasonCode: string; reasonDetail?: string; systemTriggered?: boolean
}): Promise<void> {
  await api.patch(`/api/v1/internal/customers/${customerId}/grade`, body)
}
export async function makeDormant(customerId: number, reasonDetail?: string): Promise<void> {
  await api.patch(`/api/v1/internal/customers/${customerId}/dormant`, null, { params: { reasonDetail } })
}
export async function suspendCustomer(customerId: number, reasonDetail?: string): Promise<void> {
  await api.patch(`/api/v1/internal/customers/${customerId}/suspend`, null, { params: { reasonDetail } })
}
export async function closeCustomer(customerId: number, body: {
  closeReasonCode: string; reasonDetail?: string
}): Promise<void> {
  await api.patch(`/api/v1/internal/customers/${customerId}/close`, body)
}
export async function reactivateCustomer(customerId: number, reasonDetail?: string): Promise<void> {
  await api.patch(`/api/v1/internal/customers/${customerId}/reactivate`, null, { params: { reasonDetail } })
}

// ── 조회 접근 감사로그 (access-log) ───────────────────────────────────────────
// 행위 직원은 게이트웨이가 JWT 에서 주입한 X-Employee-Id 로 식별된다(클라이언트가 직원 ID를 보내지 않음).

export type AccessLog = {
  customerAccessLogId: number
  accessorEmployeeId: number
  accessorName: string | null
  accessorRole: string | null       // BankRole grade_code 스냅샷 (예: BRANCH_MANAGER)
  accessorBranchCode: string | null
  targetCustomerId: number
  targetCustomerName: string | null
  accessActionCode: string          // CUSTOMER_DETAIL / CONTACT_VIEW
  accessReason: string | null
  accessedAt: string
}

/** 명시적 접근 기록(연락처 등 민감정보 열람). 고객 상세 조회는 백엔드가 자동 기록한다. */
export async function recordAccess(customerId: number, body: {
  actionCode: string; reason?: string
}): Promise<void> {
  await api.post(`/api/v1/internal/customers/${customerId}/access-log`, body)
}

/** 감사로그 조회 — 직원명·고객명·행위 부분일치(keyword), 지점 한정(branch) 선택. 최신순. */
export async function getAccessLogs(params: {
  keyword?: string; branch?: string; page?: number; size?: number
}): Promise<Page<AccessLog>> {
  return unwrap(await api.get('/api/v1/internal/customers/access-logs', { params }))
}

// ── 컴플라이언스 목록 (party) ─────────────────────────────────────────────────

export type EddPending = {
  partyId: number; partyName: string; amlRiskLevelCode: string
  cddLevelCode: string; kycStatusCode: string; eddNextReviewDate: string | null
}
export type SanctionedParty = {
  partyId: number; partyName: string; birthDate: string | null; nationalityCode: string | null
  ofacSanctionedYn: string; unSanctionedYn: string; euSanctionedYn: string; krSanctionedYn: string
  amlRiskLevelCode: string; sanctionLastScreenedAt: string | null
}
export type FatcaReportable = {
  partyId: number; partyName: string; birthDate: string | null; nationalityCode: string | null
  fatcaStatusCode: string; fatcaReportableYn: string; crsStatusCode: string; crsReportableYn: string
  fatcaLastReviewedAt: string | null
}
export type KycExpiring = {
  partyId: number; partyName: string; kycStatusCode: string
  kycExpiryDate: string | null; kycNextReviewDate: string | null
}
export type Minor = {
  partyId: number; partyName: string; birthDate: string | null
  genderCode: string | null; nationalityCode: string | null
}

const pageParams = (page = 0, size = 20) => ({ params: { page, size } })

export async function listEddPending(page = 0, size = 20): Promise<Page<EddPending>> {
  return unwrap(await api.get('/api/v1/internal/compliance/edd-pending', pageParams(page, size)))
}
export async function listSanctioned(page = 0, size = 20): Promise<Page<SanctionedParty>> {
  return unwrap(await api.get('/api/v1/internal/compliance/sanctioned', pageParams(page, size)))
}
export async function listFatcaCrs(page = 0, size = 20): Promise<Page<FatcaReportable>> {
  return unwrap(await api.get('/api/v1/internal/compliance/fatca-crs', pageParams(page, size)))
}
export async function listKycExpiring(targetDate: string, page = 0, size = 20): Promise<Page<KycExpiring>> {
  return unwrap(await api.get('/api/v1/internal/compliance/kyc-expiring', { params: { targetDate, page, size } }))
}
export async function listMinors(page = 0, size = 20): Promise<Page<Minor>> {
  return unwrap(await api.get('/api/v1/internal/party/minors', pageParams(page, size)))
}

// ── 제재 스크리닝 Hit 검토 ────────────────────────────────────────────────────

export type SanctionHit = {
  sanctionScreeningHitId: number; partyId: number; partyName: string
  birthDate: string | null; nationalityCode: string | null
  hitTypeCode: string; matchRate: number; screeningStatusCode: string; detectedAt: string
}
export async function listScreeningPending(page = 0, size = 20): Promise<Page<SanctionHit>> {
  return unwrap(await api.get('/api/v1/internal/compliance/screening-hits/pending', pageParams(page, size)))
}
export async function clearScreeningHit(hitId: number, comment?: string): Promise<void> {
  await api.patch(`/api/v1/internal/compliance/screening-hits/${hitId}/clear`, null, { params: { comment } })
}
export async function confirmScreeningHit(hitId: number, comment?: string): Promise<void> {
  await api.patch(`/api/v1/internal/compliance/screening-hits/${hitId}/confirm`, null, { params: { comment } })
}

// ── 대리인 위임장 검토 ────────────────────────────────────────────────────────

export type AgentReview = {
  relationId: number; ownerPartyId: number; agentPartyId: number; agentName: string
  relationTypeCode: string; relationDetailCode: string | null; representationScope: string | null
  proofUrl: string | null; relationStartDate: string; relationReviewStatusCode: string
}
export async function listAgentReviewPending(page = 0, size = 20): Promise<Page<AgentReview>> {
  return unwrap(await api.get('/api/v1/internal/party/relations/review-pending', pageParams(page, size)))
}
export async function approveAgentReview(relationId: number): Promise<void> {
  await api.patch(`/api/v1/internal/party/relations/${relationId}/approve`)
}
export async function rejectAgentReview(relationId: number): Promise<void> {
  await api.patch(`/api/v1/internal/party/relations/${relationId}/reject`)
}

// ── 중복고객 검토 ─────────────────────────────────────────────────────────────

export type DuplicateReview = {
  duplicateReviewCaseId: number; newPartyId: number; newPartyName: string
  existingPartyId: number; existingPartyName: string
  matchTypeCode: string; reviewStatusCode: string; detectedAt: string
}
export async function listDuplicatesPending(page = 0, size = 20): Promise<Page<DuplicateReview>> {
  return unwrap(await api.get('/api/v1/internal/party/duplicates/pending', pageParams(page, size)))
}
export async function markDuplicate(caseId: number, comment?: string): Promise<void> {
  await api.patch(`/api/v1/internal/party/duplicates/${caseId}/duplicate`, null, { params: { comment } })
}
export async function markDistinct(caseId: number, comment?: string): Promise<void> {
  await api.patch(`/api/v1/internal/party/duplicates/${caseId}/distinct`, null, { params: { comment } })
}
