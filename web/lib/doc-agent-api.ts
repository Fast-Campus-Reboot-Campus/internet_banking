import axios from 'axios'

const docAgentApi = axios.create({
  baseURL: process.env.NEXT_PUBLIC_DOC_AGENT_API_URL || 'http://localhost:8087',
  headers: { 'Content-Type': 'application/json' },
})

docAgentApi.interceptors.request.use((config) => {
  if (typeof window === 'undefined') return config
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export type DocSubmission = {
  submission_id: string
  application_id: string
  doc_code: string
  verify_status: string
  human_review_status: string
  forgery_score: number | '-'
  legal_hold: boolean
  created_at: string
}

export type HumanReviewDecision = 'CLEARED' | 'CONFIRMED_FORGERY'

// ── 휴먼리뷰 큐 ──────────────────────────────────────────────────────────

export async function getHumanReviewQueue(): Promise<DocSubmission[]> {
  const { data } = await docAgentApi.get('/api/documents/queue')
  return Array.isArray(data) ? data : []
}

export async function decideHumanReview(submissionId: string, decision: HumanReviewDecision, reviewerId: string) {
  const { data } = await docAgentApi.post(`/api/documents/${submissionId}/review`, {
    decision,
    reviewer_id: reviewerId,
  })
  return data
}

// ── 리걸홀드 ──────────────────────────────────────────────────────────────

export async function enableLegalHold(submissionId: string) {
  const { data } = await docAgentApi.patch(`/api/documents/${submissionId}/legal-hold/enable`)
  return data
}

export async function disableLegalHold(submissionId: string) {
  const { data } = await docAgentApi.patch(`/api/documents/${submissionId}/legal-hold/disable`)
  return data
}
