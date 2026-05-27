export type ConclusionCd =
  | 'BIAS_SUSPECTED'
  | 'NO_BIAS_DETECTED'
  | 'VIOLATION_SUSPECTED'
  | 'COMPLIANT'
  | 'INSUFFICIENT_DATA'

export type AnalysisTypeCd = 'BIAS_DETECTION' | 'COMPLIANCE_VERIFICATION'

export type ReviewerRiskScore = {
  reviewerId: number
  reviewerName: string
  department: string
  biasScore: number
  complianceScore: number
  evaluationCount: number
  lastEvaluatedAt: string
}

export type AuditOpinion = {
  opinionId: number
  advrId: number | null
  revId: number
  reviewerId: number
  reviewerName: string
  analysisTypeCd: AnalysisTypeCd
  conclusionCd: ConclusionCd
  reasoningSummary: string
  confidenceScore: number
  generatedAt: string
}

export type QuarantineReport = {
  advrId: number
  revId: number
  targetReviewerId: number
  targetReviewerName: string
  advisoryTypeCd: string
  severityCd: 'WARN' | 'CRITICAL'
  advrTitle: string
  quarantinedAt: string
  generatedAt: string
}

export const MOCK_RISK_SCORES: ReviewerRiskScore[] = [
  { reviewerId: 201, reviewerName: '김도현', department: '여신심사1팀', biasScore: 75, complianceScore: 30, evaluationCount: 18, lastEvaluatedAt: '2026-05-26 14:32' },
  { reviewerId: 202, reviewerName: '이수진', department: '여신심사2팀', biasScore: 62, complianceScore: 18, evaluationCount: 14, lastEvaluatedAt: '2026-05-25 10:15' },
  { reviewerId: 203, reviewerName: '박재원', department: '여신심사1팀', biasScore: 45, complianceScore: 55, evaluationCount: 22, lastEvaluatedAt: '2026-05-26 09:47' },
  { reviewerId: 204, reviewerName: '최민지', department: '여신심사3팀', biasScore: 20, complianceScore: 10, evaluationCount: 31, lastEvaluatedAt: '2026-05-24 16:08' },
  { reviewerId: 205, reviewerName: '정승우', department: '여신심사2팀', biasScore: 10, complianceScore:  5, evaluationCount: 26, lastEvaluatedAt: '2026-05-23 11:30' },
  { reviewerId: 206, reviewerName: '한소영', department: '여신심사3팀', biasScore:  5, complianceScore:  0, evaluationCount: 19, lastEvaluatedAt: '2026-05-22 14:55' },
]

export const MOCK_AUDIT_OPINIONS: AuditOpinion[] = [
  {
    opinionId: 1001, advrId: 501, revId: 9001, reviewerId: 201, reviewerName: '김도현',
    analysisTypeCd: 'BIAS_DETECTION',
    conclusionCd: 'BIAS_SUSPECTED',
    reasoningSummary: '외국인 신청자(F-2 비자) 거절 건에서 소득·DSR 조건이 동일 내국인 승인 건과 동일함에도 "향후 국내 체류 불확실성"을 사유로 거절. 집단 귀속 표현이 심사 의견서에 포함됨. 동일 심사관 최근 3건 중 외국인 거절률 100%, 동료 평균 대비 Z=2.8σ.',
    confidenceScore: 0.87, generatedAt: '2026-05-26 14:32',
  },
  {
    opinionId: 1002, advrId: 502, revId: 9002, reviewerId: 203, reviewerName: '박재원',
    analysisTypeCd: 'COMPLIANCE_VERIFICATION',
    conclusionCd: 'VIOLATION_SUSPECTED',
    reasoningSummary: 'DSR 58.4%로 내부 기준(60%) 통과 건임에도 "소득 안정성 우려"를 사유로 거절. 심사 의견서에 정량 근거 없이 미래 소득 감소를 예측. 동일 소득·DSR 조건의 정규직 신청자 3건은 모두 승인. 금융소비자보호법 제22조 위반 의심.',
    confidenceScore: 0.91, generatedAt: '2026-05-26 09:47',
  },
  {
    opinionId: 1003, advrId: 503, revId: 9003, reviewerId: 202, reviewerName: '이수진',
    analysisTypeCd: 'BIAS_DETECTION',
    conclusionCd: 'BIAS_SUSPECTED',
    reasoningSummary: '여성 자영업자(요식업) 신청자에게 "업종 특성상 소득 변동성 큼"을 이유로 추가 담보 요구. 동일 매출·사업 연수 남성 자영업자 2건에서는 추가 담보 없이 승인. DIR(여성/남성) = 0.62로 임계값(0.80) 미달.',
    confidenceScore: 0.83, generatedAt: '2026-05-25 10:15',
  },
  {
    opinionId: 1004, advrId: null, revId: 9004, reviewerId: 204, reviewerName: '최민지',
    analysisTypeCd: 'BIAS_DETECTION',
    conclusionCd: 'NO_BIAS_DETECTED',
    reasoningSummary: '심사 의견서에 집단 귀속 표현 없음. 거절 사유가 DSR 66.1% 초과(기준 60%)로 정량 근거 명확. 동일 기간 동일 코호트 내 심사관 편차 Z=0.4σ, 정상 범위.',
    confidenceScore: 0.95, generatedAt: '2026-05-24 16:08',
  },
  {
    opinionId: 1005, advrId: null, revId: 9005, reviewerId: 205, reviewerName: '정승우',
    analysisTypeCd: 'COMPLIANCE_VERIFICATION',
    conclusionCd: 'COMPLIANT',
    reasoningSummary: '거절 의견서에 CB 신용점수 521점(내부 기준 550점 미달), LTV 83%(담보대출 상한 80% 초과) 등 정량 기준이 모두 명시됨. 동일 조건 비교 집단 대비 거절 판단 일관성 확인.',
    confidenceScore: 0.93, generatedAt: '2026-05-23 11:30',
  },
  {
    opinionId: 1006, advrId: 504, revId: 9006, reviewerId: 201, reviewerName: '김도현',
    analysisTypeCd: 'COMPLIANCE_VERIFICATION',
    conclusionCd: 'VIOLATION_SUSPECTED',
    reasoningSummary: '비정규직(파견직) 신청자에게 "고용 불안정"을 사유로 거절. 사업장 확인서 상 동일 고용주 5년 근속이 확인됨에도 계약직 유형만으로 판단. 동일 근속 정규직 신청자 승인 건과의 차별적 취급 의심.',
    confidenceScore: 0.88, generatedAt: '2026-05-26 11:05',
  },
]

export const MOCK_QUARANTINE: QuarantineReport[] = [
  {
    advrId: 501, revId: 9001, targetReviewerId: 201, targetReviewerName: '김도현',
    advisoryTypeCd: 'BIAS_DETECTION', severityCd: 'CRITICAL',
    advrTitle: '[CRITICAL] 외국인 신청자 집중 거절 — 집단 귀속 표현 탐지',
    quarantinedAt: '2026-05-26 14:33', generatedAt: '2026-05-26 14:30',
  },
  {
    advrId: 502, revId: 9002, targetReviewerId: 203, targetReviewerName: '박재원',
    advisoryTypeCd: 'REREVIEW_RECOMMEND', severityCd: 'CRITICAL',
    advrTitle: '[CRITICAL] DSR 통과 건 무근거 거절 — 규정 위반 의심',
    quarantinedAt: '2026-05-26 09:48', generatedAt: '2026-05-26 09:44',
  },
  {
    advrId: 503, revId: 9003, targetReviewerId: 202, targetReviewerName: '이수진',
    advisoryTypeCd: 'BIAS_DETECTION', severityCd: 'WARN',
    advrTitle: '[WARN] 여성 자영업자 추가 담보 차별 요구',
    quarantinedAt: '2026-05-25 10:16', generatedAt: '2026-05-25 10:12',
  },
  {
    advrId: 504, revId: 9006, targetReviewerId: 201, targetReviewerName: '김도현',
    advisoryTypeCd: 'REREVIEW_RECOMMEND', severityCd: 'WARN',
    advrTitle: '[WARN] 비정규직 신청자 고용형태 편향 거절',
    quarantinedAt: '2026-05-26 11:06', generatedAt: '2026-05-26 11:02',
  },
]

export function biasRiskLevel(score: number): 'critical' | 'high' | 'medium' | 'low' {
  if (score >= 70) return 'critical'
  if (score >= 50) return 'high'
  if (score >= 30) return 'medium'
  return 'low'
}

export function conclusionLabel(cd: ConclusionCd): string {
  const map: Record<ConclusionCd, string> = {
    BIAS_SUSPECTED:      '편향 의심',
    NO_BIAS_DETECTED:    '편향 없음',
    VIOLATION_SUSPECTED: '위반 의심',
    COMPLIANT:           '규정 준수',
    INSUFFICIENT_DATA:   '데이터 부족',
  }
  return map[cd]
}

export function analysisTypeLabel(cd: AnalysisTypeCd): string {
  return cd === 'BIAS_DETECTION' ? '편향 탐지' : '규정 준수'
}
