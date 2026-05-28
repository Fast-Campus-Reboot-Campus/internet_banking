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

// ── 어드바이저리 리포트 ────────────────────────────────────────────────────

export type AdvisoryReport = {
  advrId: number
  revId: number
  reviewerId: number
  status: string
  createdAt: string
}

export async function getAdvisoryReports(params?: Record<string, unknown>) {
  const { data } = await advisoryApi.get<AdvisoryReport[]>('/api/advisory/reports', { params })
  return data
}

export async function getAdvisoryReport(advrId: number) {
  const { data } = await advisoryApi.get<AdvisoryReport>(`/api/advisory/reports/${advrId}`)
  return data
}

export async function viewAdvisoryReport(advrId: number) {
  const { data } = await advisoryApi.post(`/api/advisory/reports/${advrId}/view`)
  return data
}

export async function ackAdvisoryReport(advrId: number) {
  const { data } = await advisoryApi.post(`/api/advisory/reports/${advrId}/ack`)
  return data
}

// ── 어드바이저리 규칙 ─────────────────────────────────────────────────────

export type AdvisoryRule = {
  ruleId: number
  ruleName: string
  ruleContent: string
  isActive: boolean
}

export async function getAdvisoryRules() {
  const { data } = await advisoryApi.get<AdvisoryRule[]>('/api/advisory/rules')
  return data
}

export async function updateAdvisoryRule(ruleId: number, payload: Partial<AdvisoryRule>) {
  const { data } = await advisoryApi.put(`/api/advisory/rules/${ruleId}`, payload)
  return data
}

// ── 감사 의견 ─────────────────────────────────────────────────────────────

export async function getAuditOpinionsByReport(advrId: number) {
  const { data } = await advisoryApi.get(`/api/advisory/audit/opinions/by-report/${advrId}`)
  return data
}

export async function getAuditOpinionsByReviewer(reviewerId: number) {
  const { data } = await advisoryApi.get(`/api/advisory/audit/opinions/by-reviewer/${reviewerId}`)
  return data
}

export async function getReviewerRiskScore(reviewerId: number) {
  const { data } = await advisoryApi.get(`/api/advisory/audit/risk-scores/${reviewerId}`)
  return data
}

export async function getRecentAuditOpinions() {
  const { data } = await advisoryApi.get('/api/advisory/audit/opinions/recent')
  return data
}

export async function getTopBiasRiskScores() {
  const { data } = await advisoryApi.get('/api/advisory/audit/risk-scores/top/bias')
  return data
}

export async function getTopComplianceRiskScores() {
  const { data } = await advisoryApi.get('/api/advisory/audit/risk-scores/top/compliance')
  return data
}

export async function getQuarantineList() {
  const { data } = await advisoryApi.get('/api/advisory/audit/quarantine')
  return data
}

// ── 통계 ──────────────────────────────────────────────────────────────────

export async function getReviewerStats(reviewerId: number) {
  const { data } = await advisoryApi.get(`/api/advisory/stats/reviewers/${reviewerId}`)
  return data
}
