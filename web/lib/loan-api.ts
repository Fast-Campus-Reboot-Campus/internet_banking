/* eslint-disable @typescript-eslint/no-explicit-any -- 대출 API 응답 타입 미정의 구간, 빌드 차단 방지용 임시 처리 */
import axios from "axios";
import { getAdminGatewayHeaders } from "@/lib/admin-loan-auth";

// loan-service 전용 axios 인스턴스.
// 인증·고객 API(@/lib/api)는 customer-service를 가리키지만, 대출 엔드포인트는
// loan-service(기본 8083)를 직접 호출한다. (게이트웨이 미경유 로컬 개발 구성)
// 인증 우선순위:
//   1. accessToken(JWT) → Authorization: Bearer <token>  (개인 뱅킹 로그인)
//   2. admin_user(목업) → X-User-Id / X-User-Role 헤더   (어드민 목업 로그인)
const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_LOAN_API_URL || "http://localhost:8083",
  headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((config) => {
  if (typeof window !== "undefined") {
    // /admin 화면에선 admin 신원(게이트웨이 헤더) 우선, 그 외엔 개인 JWT
    const adminHeaders = getAdminGatewayHeaders();
    if (Object.keys(adminHeaders).length > 0) {
      Object.assign(config.headers, adminHeaders);
    } else {
      const token = localStorage.getItem("accessToken");
      if (token) config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && typeof window !== "undefined") {
      localStorage.removeItem("accessToken");
      localStorage.removeItem("access_token");
      window.location.href = "/login";
    }
    return Promise.reject(err);
  },
);

// ─── 공통 타입 ───────────────────────────────────────────────

export interface LoanProduct {
  prodId: number;
  prodName: string;
  loanTypeCd: string;
  baseRateBps: number;
  minRateBps: number;
  maxRateBps: number;
  minAmount: number;
  maxAmount: number;
  minPeriodMo: number;
  maxPeriodMo: number;
  repaymentMethodCd: string;
  targetCustomerCd: string;
  prodStatusCd: string;
}

export interface PreferentialRatePolicy {
  policyId: number;
  policyName: string;
  preferentialRateBps: number;
  conditionCd: string;
}

export interface LoanApplication {
  applId: number;
  applNo: string;
  applStatusCd: string;
  prodId: number;
  customerId: number;
  requestedAmount: number;
  requestedPeriodMo: number;
  channelCd: string;
  loanPurposeCd: string;
  repaymentMethodCd: string;
  estimatedIncomeAmt: number;
  employmentTypeCd: string;
  createdAt: string;
}

export interface LoanJourney {
  application: LoanApplication;
  prescreening?: { resultCd: string; maxAmount: number; rateBps: number };
  creditEvaluation?: { resultCd: string; creditScore: number; rateBps: number };
  dsr?: { dsrRatio: number; resultCd: string };
  review?: { resultCd: string; reviewerComment: string };
}

export interface LoanContract {
  cntrId: number;
  cntrNo: string;
  applId: number;
  customerId: number;
  cntrStatusCd: string;
  contractedAmount: number;
  totalRateBps: number;
  contractedPeriodMo: number;
  cntrStartDate: string;
  cntrEndDate: string;
  repaymentMethodCd: string;
  signedAt: string;
}

export interface RepaymentSchedule {
  seq: number;
  scheduledDt: string;
  principalAmt: number;
  interestAmt: number;
  totalAmt: number;
  paidYn: string;
}

export interface Notification {
  notifId: number;
  notifTypeCd: string;
  title: string;
  content: string;
  readYn: string;
  createdAt: string;
}

// ─── 상품 ─────────────────────────────────────────────────────

export const loanProductApi = {
  list: (params?: {
    loanTypeCd?: string;
    prodStatusCd?: string;
    page?: number;
    size?: number;
  }) => api.get<any>("/api/loan-products", { params }),

  get: (prodId: number) =>
    api.get<any>(`/api/loan-products/${prodId}`),

  preferentialRates: (prodId: number) =>
    api.get<any>(`/api/loan-products/${prodId}/preferential-rate-policies`),

  create: (body: object) =>
    api.post<any>("/api/loan-products", body),

  update: (prodId: number, body: object) =>
    api.patch<any>(`/api/loan-products/${prodId}`, body),

  discontinue: (prodId: number, body?: object) =>
    api.post<any>(`/api/loan-products/${prodId}/discontinue`, body ?? {}),
};

// ─── 신청 ─────────────────────────────────────────────────────

export const loanApplicationApi = {
  create: (body: {
    customerId: number;
    prodId: number;
    channelCd: string;
    requestedAmount: number;
    requestedPeriodMo: number;
    loanPurposeCd: string;
    repaymentMethodCd: string;
    estimatedIncomeAmt: number;
    employmentTypeCd: string;
  }) => api.post<any>("/api/loan-applications", body),

  list: (params: { customerId: number; page?: number; size?: number }) =>
    api.get<any>("/api/loan-applications", { params }),

  journey: (applId: number) =>
    api.get<any>(`/api/loan-applications/${applId}/journey`),

  submitConsent: (applId: number, body: { consentTypeCd: string; agreedYn: string }) =>
    api.post<any>(`/api/loan-applications/${applId}/credit-consents`, body),

  verifyIdentity: (applId: number, body: { idvMethodCd: string; idvTargetCd: string; mobileNo: string }) =>
    api.post<any>(`/api/loan-applications/${applId}/identity-verifications`, body),

  get: (applId: number) =>
    api.get<any>(`/api/loan-applications/${applId}`),

  cancel: (applId: number, body?: { cancelReasonCd?: string }) =>
    api.post<any>(`/api/loan-applications/${applId}/cancel`, body ?? {}),

  runPrescreening: (applId: number, body?: { prescResultCd?: string; estimatedLimit?: number }) =>
    api.post<any>(`/api/loan-applications/${applId}/prescreening`, body ?? {}),

  getPrescreening: (applId: number) =>
    api.get<any>(`/api/loan-applications/${applId}/prescreening`),

  runCreditEvaluation: (applId: number, body: { cevalEngine: string; cevalDecisionCd: string; cevalEngineVersion?: string; cevalGrade?: string; cevalScore?: number; evalLimitAmount?: number; evalRateBps?: number }) =>
    api.post<any>(`/api/loan-applications/${applId}/credit-evaluation`, body),

  getCreditEvaluation: (applId: number) =>
    api.get<any>(`/api/loan-applications/${applId}/credit-evaluation`),

  runDsr: (applId: number, body: { annualIncomeAmt: number; newAnnualRepayAmt?: number; existingAnnualRepayAmt?: number }) =>
    api.post<any>(`/api/loan-applications/${applId}/dsr-calculation`, body),

  getDsr: (applId: number) =>
    api.get<any>(`/api/loan-applications/${applId}/dsr-calculation`),

  uploadDocument: (applId: number, formData: FormData) =>
    api.post<any>(`/api/loan-applications/${applId}/documents`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    }),

  getDocuments: (applId: number) =>
    api.get<any>(`/api/loan-applications/${applId}/documents`),

  getReview: (applId: number) =>
    api.get<any>(`/api/loan-applications/${applId}/review`),
};

// ─── 보증인 동의 ───────────────────────────────────────────────

export const guarantorApi = {
  list: (applId: number) =>
    api.get<any>(`/api/loan-applications/${applId}/guarantor-agreements`),

  register: (applId: number, body: object) =>
    api.post<any>(`/api/loan-applications/${applId}/guarantor-agreements`, body),

  sign: (applId: number, gagrId: number, body: { signedDocUrl: string; signedDocHash: string }) =>
    api.post<any>(`/api/loan-applications/${applId}/guarantor-agreements/${gagrId}/sign`, body),

  cancel: (applId: number, gagrId: number, body?: { cancelReasonCd?: string; cancelRemark?: string }) =>
    api.post<any>(`/api/loan-applications/${applId}/guarantor-agreements/${gagrId}/cancel`, body ?? {}),
};

// ─── 담보 ─────────────────────────────────────────────────────

export const collateralApi = {
  list: (applId: number) =>
    api.get<any>(`/api/loan-applications/${applId}/collaterals`),

  create: (applId: number, body: object) =>
    api.post<any>(`/api/loan-applications/${applId}/collaterals`, body),

  evaluate: (colId: number, body: object) =>
    api.post<any>(`/api/collaterals/${colId}/evaluations`, body),

  calculateLtv: (colId: number, body?: object) =>
    api.post<any>(`/api/collaterals/${colId}/ltv-calculation`, body ?? {}),

  getLtv: (colId: number) =>
    api.get<any>(`/api/collaterals/${colId}/ltv-calculation`),
};

// ─── 약정 ─────────────────────────────────────────────────────

export const loanContractApi = {
  create: (applId: number, body: object) =>
    api.post<any>("/api/loan-contracts", { applId, ...body }),

  list: (params: { customerId: number; page?: number; size?: number }) =>
    api.get<any>("/api/loan-contracts", { params }),

  adminList: (params: {
    cntrStatusCd?: string; dateFrom?: string; dateTo?: string;
    page?: number; size?: number;
  }) =>
    api.get<any>("/api/admin/loan-contracts", { params }),

  get: (cntrId: number) =>
    api.get<any>(`/api/loan-contracts/${cntrId}`),

  execute: (cntrId: number, body: object) =>
    api.post<any>(`/api/loan-contracts/${cntrId}/executions`, body),

  getRepaymentSchedules: (cntrId: number) =>
    api.get<any>(`/api/loan-contracts/${cntrId}/repayment-schedules`),

  registerRepaymentAccount: (cntrId: number, body: { accountNo: string }) =>
    api.post<any>(`/api/loan-contracts/${cntrId}/repayment-account`, body),
};

// ─── 상환 ─────────────────────────────────────────────────────

export const repaymentApi = {
  pay: (cntrId: number, body: { paymentAmt: number; paymentDt: string }) =>
    api.post<any>(`/api/loan-contracts/${cntrId}/repayments`, body),

  partialPrepay: (cntrId: number, body: { prepaymentAmt: number }) =>
    api.post<any>(`/api/loan-contracts/${cntrId}/repayments/partial`, body),

  fullPrepay: (cntrId: number, body: object) =>
    api.post<any>(`/api/loan-contracts/${cntrId}/prepayments`, body),

  list: (cntrId: number) =>
    api.get<any>(`/api/loan-contracts/${cntrId}/repayments`),

  reverse: (cntrId: number, rtxId: number, body?: { reversalReasonCd?: string; reversalRemark?: string }) =>
    api.post<any>(`/api/loan-contracts/${cntrId}/repayments/${rtxId}/reversal`, body ?? {}),
};

// ─── 금리/이자 ────────────────────────────────────────────────

export const rateApi = {
  getInterestAccruals: (cntrId: number) =>
    api.get<any>(`/api/loan-contracts/${cntrId}/interest-accruals`),

  requestRateChange: (cntrId: number, body: { requestedRateBps: number; reasonCd: string }) =>
    api.post<any>(`/api/loan-contracts/${cntrId}/rate-changes`, body),

  getRateChanges: (cntrId: number) =>
    api.get<any>(`/api/loan-contracts/${cntrId}/rate-changes`),
};

// ─── 만기/해지 ────────────────────────────────────────────────

export const closureApi = {
  extendMaturity: (cntrId: number, body: { newMaturityDt: string }) =>
    api.post<any>(`/api/loan-contracts/${cntrId}/maturity/extend`, body),

  getMaturity: (cntrId: number) =>
    api.get<any>(`/api/loan-contracts/${cntrId}/maturity`),

  close: (cntrId: number, body: { closureReasonCd: string }) =>
    api.post<any>(`/api/loan-contracts/${cntrId}/closure`, body),

  getClosure: (cntrId: number) =>
    api.get<any>(`/api/loan-contracts/${cntrId}/closure`),
};

// ─── 부수 기능 ────────────────────────────────────────────────

export const loanMiscApi = {
  getCreditScore: (customerId: number) =>
    api.get<any>(`/api/credit-score`, { params: { customerId } }),

  getBusinessCalendar: (params: { yearMonth: string }) =>
    api.get<any>("/api/business-calendar", { params }),

  getStatusHistory: (targetTable: string, targetId: number) =>
    api.get<any>(`/api/status-history`, { params: { targetTable, targetId } }),

  getDelinquencySnapshots: (cntrId: number) =>
    api.get<any>(`/api/loan-contracts/${cntrId}/delinquency/snapshots`),

  getNotifications: (customerId: number) =>
    api.get<any>("/api/notifications", { params: { customerId } }),

  updateNotification: (notifId: number, body: { readYn: string }) =>
    api.patch<any>(`/api/notifications/${notifId}`, body),

  getCertificate: (cntrId: number, certTypeCd: string) =>
    api.get<any>(`/api/loan-contracts/${cntrId}/certificates`, {
      params: { certTypeCd },
    }),

  getCreditInfoReport: (cntrId: number) =>
    api.get<any>(`/api/loan-contracts/${cntrId}/credit-info-reports`),

  getDelinquency: (cntrId: number) =>
    api.get<any>(`/api/loan-contracts/${cntrId}/delinquency`),
};

// ─── 보증보험 ─────────────────────────────────────────────────

export const guaranteeInsuranceApi = {
  issue: (cntrId: number, body: object) =>
    api.post<any>(`/api/loan-contracts/${cntrId}/guarantee-insurance`, body),

  get: (cntrId: number, ginsId: number) =>
    api.get<any>(`/api/loan-contracts/${cntrId}/guarantee-insurance/${ginsId}`),

  cancel: (cntrId: number, ginsId: number, body?: { cancelReasonCd?: string }) =>
    api.post<any>(`/api/loan-contracts/${cntrId}/guarantee-insurance/${ginsId}/cancel`, body ?? {}),
};

// ─── 신용점수 미리보기 ────────────────────────────────────────

export const creditScorePreviewApi = {
  preview: (body: {
    customerId: number;
    loanTypeCd: string;
    requestedAmount: number;
    requestedPeriodMo: number;
    loanPurposeCd?: string;
    employmentTypeCd?: string;
    estimatedIncomeAmt?: number;
    consentYn: string;
  }) => api.post<any>('/api/credit-score/preview', body),
};

// ─── 헬퍼 ────────────────────────────────────────────────────

export function bpsToRate(bps: number): string {
  return (bps / 100).toFixed(2);
}

export function formatAmount(amt: number): string {
  if (amt >= 100_000_000) return `${(amt / 100_000_000).toLocaleString("ko-KR")}억원`;
  if (amt >= 10_000) return `${(amt / 10_000).toLocaleString("ko-KR")}만원`;
  return `${amt.toLocaleString("ko-KR")}원`;
}

// ─── 어드민 - 본심사 ──────────────────────────────────────────

export const adminReviewApi = {
  listPending: () =>
    api.get<any>('/api/loan-reviews/pending'),

  listPendingApprover: () =>
    api.get<any>('/api/loan-reviews/pending-approver'),

  stats: (from: string, to: string) =>
    api.get<any>('/api/loan-reviews/stats', { params: { from, to } }),

  get: (applId: number) =>
    api.get<any>(`/api/loan-applications/${applId}/review`),

  run: (applId: number, body: object) =>
    api.post<any>(`/api/loan-applications/${applId}/review`, body),

  autoDecide: (applId: number) =>
    api.post<any>(`/api/loan-applications/${applId}/review/auto-decide`, {}),

  confirm: (applId: number, body: { confirmRemark?: string }) =>
    api.post<any>(`/api/loan-applications/${applId}/review/confirm`, body),

  acknowledgeBias: (applId: number, body?: { acknowledgeRemark?: string }) =>
    api.post<any>(`/api/loan-applications/${applId}/review/acknowledge-bias`, body ?? {}),

  approverApprove: (applId: number, body: object) =>
    api.post<any>(`/api/loan-applications/${applId}/review/approver-approve`, body),

  revise: (applId: number, body: object) =>
    api.patch<any>(`/api/loan-applications/${applId}/review`, body),

  getAdvices: (revId: number) =>
    api.get<any>(`/api/loan-reviews/${revId}/advices`),

  getChecks: (revId: number) =>
    api.get<any>(`/api/loan-reviews/${revId}/checks`),

  addCheck: (revId: number, body: object) =>
    api.post<any>(`/api/loan-reviews/${revId}/checks`, body),

  biasOverride: (revId: number, body: { overrideReason: string }) =>
    api.post<any>(`/api/loan-reviews/${revId}/bias-override`, body),

  getAdvisoryReports: (revId: number) =>
    api.get<any>(`/api/loan-reviews/${revId}/advisory-reports`),

  // 본사 상신 건 목록 (ROLE_HQ_REVIEWER) — Page 응답(content/totalElements 등)
  listEscalated: (page = 0, size = 20) =>
    api.get<any>('/api/loan-reviews/escalated', { params: { page, size } }),

  // 이상거래 본사 상신 (ROLE_BRANCH_MANAGER)
  escalateToHq: (applId: number, body: { escalateReason: string }) =>
    api.post<any>(`/api/loan-applications/${applId}/review/escalate-to-hq`, body),
};

// ─── 어드민 - EOD 배치 (ROLE_OPS) ────────────────────────────
// 응답은 ApiResponse 래핑 → res.data.data. baseDate/from/to 는 YYYYMMDD.
export const eodApi = {
  run: (baseDate: string) =>
    api.post<any>('/api/internal/eod/run', null, { params: { baseDate } }),

  restart: (baseDate: string) =>
    api.post<any>('/api/internal/eod/restart', null, { params: { baseDate } }),

  history: (from?: string, to?: string) =>
    api.get<any>('/api/internal/eod/history', { params: { from, to } }),
};

// ─── 어드민 - 감사로그 (ROLE_COMPLIANCE) ──────────────────────
// 컨트롤러가 List 를 그대로 반환(ApiResponse 미사용) → res.data 가 곧 배열.
export const auditApi = {
  listBreakGlass: (actorId?: number) =>
    api.get<any>('/api/audit/break-glass', { params: { actorId } }),

  listByTarget: (targetType: string, targetId: number) =>
    api.get<any>('/api/audit/access-logs', { params: { targetType, targetId } }),
};

// ─── 어드민 - break-glass 긴급 접근 (CUSTOMER 제외 전 직원) ────
// ResponseEntity<BreakGlassResponse> 반환(ApiResponse 미사용) → res.data 가 곧 응답.
export const breakGlassApi = {
  request: (body: { applId: number; reason: string }) =>
    api.post<any>('/api/break-glass', body),
};

// ─── 어드민 - 담보·서류·우대금리 ─────────────────────────────

export const adminLoanApi = {
  updateCollateral: (colId: number, body: object) =>
    api.patch<any>(`/api/collaterals/${colId}`, body),

  releaseCollateral: (colId: number, body: object) =>
    api.post<any>(`/api/collaterals/${colId}/release`, body),

  deleteDocument: (docId: number) =>
    api.delete<any>(`/api/loan-documents/${docId}`),

  downloadDocumentUrl: (docId: number) =>
    `/api/loan-documents/${docId}/download`,

  getPreferentialPolicies: (prodId: number) =>
    api.get<any>(`/api/loan-products/${prodId}/preferential-rate-policies`),

  addPreferentialPolicy: (prodId: number, body: object) =>
    api.post<any>(`/api/loan-products/${prodId}/preferential-rate-policies`, body),
};

// ─── 영업일 캘린더 ────────────────────────────────────────────

export const businessCalendarApi = {
  list: (params?: { from?: string; to?: string; page?: number; size?: number }) =>
    api.get<any>("/api/business-calendar", { params }),

  get: (calId: number) =>
    api.get<any>(`/api/business-calendar/${calId}`),

  create: (body: object) =>
    api.post<any>("/api/business-calendar", body),

  update: (calId: number, body: object) =>
    api.put<any>(`/api/business-calendar/${calId}`, body),

  delete: (calId: number) =>
    api.delete<any>(`/api/business-calendar/${calId}`),
};

// ─── 신용정보 보고서 ──────────────────────────────────────────

export const creditInfoReportApi = {
  list: (params?: { statusCd?: string; page?: number; size?: number }) =>
    api.get<any>("/api/credit-info-reports", { params }),

  retry: (crptId: number) =>
    api.post<any>(`/api/credit-info-reports/${crptId}/retry`, {}),

  ack: (crptId: number, body?: object) =>
    api.post<any>(`/api/credit-info-reports/${crptId}/ack`, body ?? {}),
};

// ─── 알림 발송함 ──────────────────────────────────────────────

export const notificationOutboxApi = {
  get: (outboxId: number) =>
    api.get<any>(`/api/notifications/${outboxId}`),

  list: (params?: { page?: number; size?: number }) =>
    api.get<any>("/api/notifications", { params }),

  retry: (outboxId: number) =>
    api.post<any>(`/api/notifications/${outboxId}/retry`, {}),
};

// ─── 본인인증 ─────────────────────────────────────────────────

export const identityVerificationApi = {
  get: (idvId: number) =>
    api.get<any>(`/api/identity-verifications/${idvId}`),
};

export function getCustomerId(): number | null {
  if (typeof window === "undefined") return null;
  const val = localStorage.getItem("customerId");
  return val ? parseInt(val) : null;
}
