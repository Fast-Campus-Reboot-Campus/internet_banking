<<<<<<< HEAD
import { api } from "./api";

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
  discountBps: number;
  conditionDesc: string;
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

  runCreditEvaluation: (applId: number) =>
    api.post<any>(`/api/loan-applications/${applId}/credit-evaluation`, {}),

  getCreditEvaluation: (applId: number) =>
    api.get<any>(`/api/loan-applications/${applId}/credit-evaluation`),

  runDsr: (applId: number, body?: { newAnnualRepayAmt?: number }) =>
    api.post<any>(`/api/loan-applications/${applId}/dsr-calculation`, body ?? {}),

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

  confirm: (applId: number, body: { reviewerId: number; confirmRemark?: string }) =>
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

  biasOverride: (revId: number, body: { overrideBy: number; overrideReason: string }) =>
    api.post<any>(`/api/loan-reviews/${revId}/bias-override`, body),

  getAdvisoryReports: (revId: number) =>
    api.get<any>(`/api/loan-reviews/${revId}/advisory-reports`),
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
  list: (params?: { year?: number; page?: number; size?: number }) =>
    api.get<any>("/api/business-calendars", { params }),

  get: (calId: number) =>
    api.get<any>(`/api/business-calendars/${calId}`),

  create: (body: object) =>
    api.post<any>("/api/business-calendars", body),

  update: (calId: number, body: object) =>
    api.patch<any>(`/api/business-calendars/${calId}`, body),

  delete: (calId: number) =>
    api.delete<any>(`/api/business-calendars/${calId}`),
};

// ─── 신용정보 보고서 ──────────────────────────────────────────

export const creditInfoReportApi = {
  list: (params?: { page?: number; size?: number }) =>
    api.get<any>("/api/credit-info-reports", { params }),

  retry: (reportId: number) =>
    api.post<any>(`/api/credit-info-reports/${reportId}/retry`, {}),

  ack: (reportId: number, body?: object) =>
    api.post<any>(`/api/credit-info-reports/${reportId}/ack`, body ?? {}),
};

// ─── 알림 발송함 ──────────────────────────────────────────────

export const notificationOutboxApi = {
  get: (outboxId: number) =>
    api.get<any>(`/api/notification-outbox/${outboxId}`),

  list: (params?: { page?: number; size?: number }) =>
    api.get<any>("/api/notification-outbox", { params }),

  retry: (outboxId: number) =>
    api.post<any>(`/api/notification-outbox/${outboxId}/retry`, {}),
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
