import axios from 'axios'

const loanApi = axios.create({
  baseURL: process.env.NEXT_PUBLIC_LOAN_API_URL || 'http://localhost:8083',
  headers: { 'Content-Type': 'application/json' },
})

loanApi.interceptors.request.use((config) => {
  if (typeof window === 'undefined') return config
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// ── 대출 신청 ──────────────────────────────────────────────────────────────

export type LoanApplicationStatus =
  | 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED' | 'CANCELLED' | 'EXECUTED'

export type LoanApplication = {
  applId: number
  customerId: string
  productId: number
  applyAmount: number
  appliedAt: string
  status: LoanApplicationStatus
}

export async function applyLoan(payload: {
  customerId: string
  productId: number
  applyAmount: number
}) {
  const { data } = await loanApi.post<LoanApplication>('/api/loan-applications', payload)
  return data
}

export async function cancelLoanApplication(applId: number) {
  const { data } = await loanApi.post(`/api/loan-applications/${applId}/cancel`)
  return data
}

export async function getLoanApplicationJourney(applId: number) {
  const { data } = await loanApi.get(`/api/loan-applications/${applId}/journey`)
  return data
}

// ── 대출 계약 ──────────────────────────────────────────────────────────────

export type LoanContract = {
  cntrId: number
  applId: number
  customerId: string
  loanAmount: number
  interestRate: number
  startDate: string
  endDate: string
  status: string
}

export async function createLoanContract(applId: number, payload: Record<string, unknown>) {
  const { data } = await loanApi.post<LoanContract>('/api/loan-contracts', { applId, ...payload })
  return data
}

export async function getLoanContract(cntrId: number) {
  const { data } = await loanApi.get<LoanContract>(`/api/loan-contracts/${cntrId}`)
  return data
}

export async function getLoanContractInterestAccruals(cntrId: number) {
  const { data } = await loanApi.get(`/api/loan-contracts/${cntrId}/interest-accruals`)
  return data
}

export async function getLoanContractCertificates(cntrId: number) {
  const { data } = await loanApi.get(`/api/loan-contracts/${cntrId}/certificates`)
  return data
}

export async function closeLoanContract(cntrId: number, payload: Record<string, unknown>) {
  const { data } = await loanApi.post(`/api/loan-contracts/${cntrId}/closure`, payload)
  return data
}

export async function getLoanClosureStatus(cntrId: number) {
  const { data } = await loanApi.get(`/api/loan-contracts/${cntrId}/closure`)
  return data
}

// ── 담보 ──────────────────────────────────────────────────────────────────

export async function getLoanCollaterals(applId: number) {
  const { data } = await loanApi.get(`/api/loan-applications/${applId}/collaterals`)
  return data
}

export async function addLoanCollateral(applId: number, payload: Record<string, unknown>) {
  const { data } = await loanApi.post(`/api/loan-applications/${applId}/collaterals`, payload)
  return data
}

// ── 신용 평가 ─────────────────────────────────────────────────────────────

export async function submitCreditEvaluation(applId: number, payload: Record<string, unknown>) {
  const { data } = await loanApi.post(`/api/loan-applications/${applId}/credit-evaluation`, payload)
  return data
}

export async function getCreditEvaluation(applId: number) {
  const { data } = await loanApi.get(`/api/loan-applications/${applId}/credit-evaluation`)
  return data
}

// ── 영업일 캘린더 ─────────────────────────────────────────────────────────

export async function checkBusinessDay(date: string) {
  const { data } = await loanApi.get('/api/business-calendar/check', { params: { date } })
  return data
}

export async function getBusinessCalendars() {
  const { data } = await loanApi.get('/api/business-calendar')
  return data
}
