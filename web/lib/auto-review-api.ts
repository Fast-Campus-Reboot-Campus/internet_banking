import axios from 'axios'

const autoReviewApi = axios.create({
  baseURL: process.env.NEXT_PUBLIC_AUTO_REVIEW_API_URL || 'http://localhost:8089',
  headers: { 'Content-Type': 'application/json' },
})

autoReviewApi.interceptors.request.use((config) => {
  if (typeof window === 'undefined') return config
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export type AutoReviewInput = {
  // Layer 1: persona
  age: number
  sex?: string
  maritalStatus?: string
  occupation?: string
  district?: string
  province?: string
  housingType?: string
  educationLevel?: string
  applicantSegment?: string
  // Layer 2: financial
  annualIncomeKw?: number
  totalDebtKw?: number
  creditDebtKw?: number
  totalAssetKw?: number
  dsr?: number
  ltv?: number
  creditScoreProxy?: number
  delinquencyHistory24m?: number
  // Layer 3: application
  productCode?: string
  requestedAmountKw?: number
  requestedPeriodMo?: number
  purposeCd?: string
  purposeRedFlag?: boolean
}

export type AutoReviewEvaluateResult = {
  modelVersion: string
  pdModelVersion?: string
  pd: number
  decisionScore?: number
  proba?: Record<string, number>
  track: string
  trackDisplayName: string
  pdThreshold: number
  safetyMarginThreshold: number
  hardFailCodes: string[]
  hardFailMessages: string[]
  rationale: string
  reportStatus: string
  shadow: boolean
}

export async function evaluateAutoReview(body: AutoReviewInput): Promise<AutoReviewEvaluateResult> {
  const { data } = await autoReviewApi.post('/api/ai/auto-review/evaluate', body)
  return data?.data ?? data
}
