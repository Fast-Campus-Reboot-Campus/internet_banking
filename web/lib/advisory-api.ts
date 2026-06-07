import axios from 'axios'

const advisoryApi = axios.create({
  baseURL: process.env.NEXT_PUBLIC_ADVISORY_API_URL || 'http://localhost:8084',
  headers: { 'Content-Type': 'application/json' },
})

advisoryApi.interceptors.request.use((config) => {
  if (typeof window === 'undefined') return config
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export type AdvisoryReportSummary = {
  advrId: number
  revId: number
  ruleId?: number
  advisoryTypeCd: string
  severityCd: string
  advrStatusCd: string
  advrTitle: string
  targetReviewerId?: number
  generatedAt?: string
  firstViewedAt?: string
  resolvedAt?: string
}

export type AckResponseCd = 'MAINTAIN' | 'OVERTURN' | 'ESCALATE' | 'NEEDS_MORE_INFO'

export type AdvisoryAckBody = {
  ackResponseCd: AckResponseCd
  decisionChangeYn?: 'Y' | 'N'
  ackReasonCd?: string
  ackRemark?: string
  beforeDecisionCd?: string
  afterDecisionCd?: string
}

// ── 어드바이저리 리포트 ────────────────────────────────────────────────────

export async function getAdvisoryReports(params?: Record<string, unknown>): Promise<AdvisoryReportSummary[]> {
  const { data } = await advisoryApi.get('/api/advisory/reports', { params })
  // ApiResponse<{totalCount, items}>
  return data?.data?.items ?? []
}

export async function getAdvisoryReport(advrId: number) {
  const { data } = await advisoryApi.get(`/api/advisory/reports/${advrId}`)
  return data?.data ?? data
}

export async function viewAdvisoryReport(advrId: number) {
  const { data } = await advisoryApi.post(`/api/advisory/reports/${advrId}/view`)
  return data?.data ?? data
}

export async function ackAdvisoryReport(advrId: number, body: AdvisoryAckBody) {
  const { data } = await advisoryApi.post(`/api/advisory/reports/${advrId}/ack`, body)
  return data?.data ?? data
}

// ── 어드바이저리 규칙 ─────────────────────────────────────────────────────

export type AdvisoryRule = {
  ruleId: number
  ruleName: string
  ruleContent: string
  isActive: boolean
}

export async function getAdvisoryRules() {
  const { data } = await advisoryApi.get('/api/advisory/rules')
  return (data?.data ?? data) as AdvisoryRule[]
}

export async function updateAdvisoryRule(ruleId: number, payload: Partial<AdvisoryRule>) {
  const { data } = await advisoryApi.put(`/api/advisory/rules/${ruleId}`, payload)
  return data?.data ?? data
}

// ── 감사 의견 ─────────────────────────────────────────────────────────────

export async function getAuditOpinionsByReport(advrId: number) {
  const { data } = await advisoryApi.get(`/api/advisory/audit/opinions/by-report/${advrId}`)
  return data?.data ?? data
}

export async function getAuditOpinionsByReviewer(reviewerId: number) {
  const { data } = await advisoryApi.get(`/api/advisory/audit/opinions/by-reviewer/${reviewerId}`)
  return data?.data ?? data
}

export async function getReviewerRiskScore(reviewerId: number) {
  const { data } = await advisoryApi.get(`/api/advisory/audit/risk-scores/${reviewerId}`)
  return data?.data ?? data
}

export async function getRecentAuditOpinions() {
  const { data } = await advisoryApi.get('/api/advisory/audit/opinions/recent')
  return data?.data ?? data
}

export async function getTopBiasRiskScores() {
  const { data } = await advisoryApi.get('/api/advisory/audit/risk-scores/top/bias')
  return data?.data ?? data
}

export async function getTopComplianceRiskScores() {
  const { data } = await advisoryApi.get('/api/advisory/audit/risk-scores/top/compliance')
  return data?.data ?? data
}

export async function getQuarantineList() {
  const { data } = await advisoryApi.get('/api/advisory/audit/quarantine')
  return data?.data ?? data
}

// ── 통계 ──────────────────────────────────────────────────────────────────

export async function getReviewerStats(reviewerId: number) {
  const { data } = await advisoryApi.get(`/api/advisory/stats/reviewers/${reviewerId}`)
  return data?.data ?? data
}
