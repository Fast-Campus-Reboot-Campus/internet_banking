import { api } from './api'

export type ReviewerRiskScoreDto = {
  reviewerId: number
  biasScore: number
  complianceScore: number
  evaluationCount: number
  lastEvaluatedAt: string | null
}

export type AiAuditOpinionDto = {
  opinionId: number
  advrId: number | null
  revId: number
  reviewerId: number
  analysisTypeCd: string
  conclusionCd: string
  reasoningSummary: string
  confidenceScore: number
  inputTokens: number
  outputTokens: number
  generatedAt: string
}

export type QuarantineReportDto = {
  advrId: number
  revId: number
  targetReviewerId: number
  advisoryTypeCd: string
  severityCd: 'WARN' | 'CRITICAL'
  advrTitle: string
  quarantinedAt: string
  generatedAt: string
}

function unwrap<T>(res: { data: { data: T } }): T {
  return res.data.data
}

export async function fetchTopBiasRiskScores(limit = 20): Promise<ReviewerRiskScoreDto[]> {
  return unwrap(await api.get('/api/advisory/audit/risk-scores/top/bias', { params: { limit } }))
}

export async function fetchRecentOpinions(limit = 20): Promise<AiAuditOpinionDto[]> {
  return unwrap(await api.get('/api/advisory/audit/opinions/recent', { params: { limit } }))
}

export async function fetchQuarantineReports(): Promise<QuarantineReportDto[]> {
  return unwrap(await api.get('/api/advisory/audit/quarantine'))
}
