/**
 * Fraud Investigation Agent — 어드민 콘솔 ↔ Python/LangGraph 사이드카 클라이언트.
 *
 * 조사 에이전트(fraud-investigation-agent/)는 별도 Python 서비스로,
 *   python scripts/serve.py   (기본 http://localhost:8090)
 * 로 띄운다. 이 모듈은 그 HTTP API(src/agent/api.py)를 감싼다.
 *
 * 다른 어드민 API(advisory-api 등)와 같은 패턴:
 *   - baseURL 은 NEXT_PUBLIC_FRAUD_AGENT_URL 로 주입(기본 localhost:8090).
 *   - 에이전트는 별도 PoC 서비스라 별도 인증을 두지 않는다(CORS 개방, 목 LLM).
 *
 * HITL: investigate → (분석가 검토) → approve. 동작(지급정지·STR)은 approve 가
 * RBAC(FRAUD_OFFICER) 통과 시에만 실행(목). 에이전트는 권고까지만.
 */
import axios from 'axios'

const fraudApi = axios.create({
  baseURL: process.env.NEXT_PUBLIC_FRAUD_AGENT_URL || 'http://localhost:8090',
  headers: { 'Content-Type': 'application/json' },
})

// ── 타입 (src/agent/api.py 응답 스키마 미러) ──────────────────────────────

export type AttackScenario =
  | 'H1_VOICE_PHISHING'
  | 'H2_ACCOUNT_TAKEOVER'
  | 'H3_LAUNDERING'
  | 'H4_INSIDER'
  | 'H5_BENIGN'

export type RecommendationStatus =
  | 'CONFIRMED' | 'FAIL_CLOSED' | 'PROVISIONAL' | 'HOLD' | 'BENIGN'

export type LiabilityGrade = 'L4' | 'L3' | 'L2' | 'L1' | 'L0'

export type ActionType =
  | 'FREEZE_PAYMENT' | 'FILE_STR' | 'ESCALATE' | 'NONE'

export type CaseSummary = {
  name: string
  description?: string | null
  alert_id: string
  account: string
  customer_id: string
  amount: number
  payee?: string | null
  channel?: string | null
  anomaly_score: number
}

export type TraceStep = {
  loop: number
  tool: string
  reason: string
  signal: string
  source?: string | null  // "real"=실 백엔드 호출 / null=목
  decisive_fact?: string | null
  scenarios: Record<string, number>
  closed_scenarios: string[]
  budget_left: number
  gate: 'plan' | 'recommend'
}

export type ProposedAction = {
  type: ActionType
  target?: string | null
  reason?: string | null
}

export type DecisiveFact = {
  kind: 'DEATH' | 'GUARDIANSHIP'
  source: string
  detail?: string | null
}

export type Recommendation = {
  scenario: AttackScenario
  status: RecommendationStatus
  tags: string[]
  rationale_chain: string[]
  liability_grade: LiabilityGrade
  actions: ProposedAction[]
  decisive_fact?: DecisiveFact | null   // fail-closed(사망·후견) 헤드라인 근거
}

export type InvestigateResponse = {
  case: string
  description?: string | null
  alert: {
    id: string
    account: string
    customer_id: string
    tx_context: { amount: number; payee?: string | null; time?: string | null; channel?: string | null }
    anomaly_score: number
  }
  initial_scenarios: Record<string, number>
  steps: TraceStep[]
  recommendation: Recommendation
  thread_id: string
  hitl_pending: boolean
}

export type ApproveResponse = {
  thread_id: string
  approved: boolean
  executed_actions: string[]
}

// ── 호출 ──────────────────────────────────────────────────────────────────

/** 조사 입력 후보(트리아지 큐 대용) — data/cases/*.json 목록. */
export async function listFraudCases(): Promise<CaseSummary[]> {
  const { data } = await fraudApi.get<CaseSummary[]>('/api/cases')
  return data
}

/** 한 사건을 조사 루프에 태워 단계별 트레이스 + 권고를 받는다. 동작은 HITL 대기. */
export async function runInvestigation(caseName: string): Promise<InvestigateResponse> {
  const { data } = await fraudApi.post<InvestigateResponse>('/api/investigate', { case: caseName })
  return data
}

/** 분석가 승인(HITL) + RBAC. approved=false 면 거부. 동작 실행은 목. */
export async function approveInvestigation(
  threadId: string,
  actorRoles: string[],
  approved: boolean,
): Promise<ApproveResponse> {
  const { data } = await fraudApi.post<ApproveResponse>('/api/approve', {
    thread_id: threadId,
    actor_roles: actorRoles,
    approved,
  })
  return data
}

// ── 표시 라벨 ──────────────────────────────────────────────────────────────

export const SCENARIO_LABEL: Record<string, string> = {
  H1_VOICE_PHISHING:   'H1 보이스피싱',
  H2_ACCOUNT_TAKEOVER: 'H2 계정탈취',
  H3_LAUNDERING:       'H3 자금세탁',
  H4_INSIDER:          'H4 내부자',
  H5_BENIGN:           'H5 정상(오탐)',
}

// fail-closed 결정적 사실 → 헤드라인 라벨 (경합 시나리오와 다른 책임 축, L4)
export const DECISIVE_LABEL: Record<string, string> = {
  DEATH:        '사망계좌 · 권리자 적격성(L4)',
  GUARDIANSHIP: '성년후견 · 단독거래 무효(L4)',
}

export const STATUS_LABEL: Record<string, string> = {
  CONFIRMED:   '확정',
  FAIL_CLOSED: 'Fail-closed(즉시차단)',
  PROVISIONAL: '잠정(예산소진·인계)',
  HOLD:        '판단보류',
  BENIGN:      '정상(오탐)',
}

export const ACTION_LABEL: Record<string, string> = {
  FREEZE_PAYMENT: '지급정지',
  FILE_STR:       'STR 보고',
  ESCALATE:       '분석가 에스컬레이션',
  NONE:           '조치 불요',
}

export function errMsg(e: unknown, fallback: string): string {
  if (axios.isAxiosError(e)) {
    // 응답 온 에러는 서버 메시지 우선. 연결 실패(사이드카 다운 등 — 응답 없음)는
    // 'Network Error' 대신 안내 폴백을 보여준다(serve.py 실행 안내).
    return e.response ? (e.response.data?.detail || e.message || fallback) : fallback
  }
  return fallback
}
