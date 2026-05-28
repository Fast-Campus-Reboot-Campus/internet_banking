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

  getPrescreening: (applId: number) =>
    api.get<any>(`/api/loan-applications/${applId}/prescreening`),

  getCreditEvaluation: (applId: number) =>
    api.get<any>(`/api/loan-applications/${applId}/credit-evaluation`),

  getDsr: (applId: number) =>
    api.get<any>(`/api/loan-applications/${applId}/dsr-calculation`),

  uploadDocument: (applId: number, formData: FormData) =>
    api.post<any>(`/api/loan-applications/${applId}/documents`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    }),

  getDocuments: (applId: number) =>
    api.get<any>(`/api/loan-applications/${applId}/documents`),

  submitGuarantorAgreement: (applId: number, body: object) =>
    api.post<any>(`/api/loan-applications/${applId}/guarantor-agreements`, body),

  getReview: (applId: number) =>
    api.get<any>(`/api/loan-applications/${applId}/review`),
};

// ─── 담보 ─────────────────────────────────────────────────────

export const collateralApi = {
  create: (applId: number, body: object) =>
    api.post<any>(`/api/loan-applications/${applId}/collaterals`, body),

  calculateLtv: (colId: number, body: object) =>
    api.post<any>(`/api/collaterals/${colId}/ltv-calculation`, body),
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

  updateRepaymentAccount: (cntrId: number, body: { accountNo: string }) =>
    api.patch<any>(`/api/loan-contracts/${cntrId}/repayment-account`, body),
};

// ─── 상환 ─────────────────────────────────────────────────────

export const repaymentApi = {
  pay: (cntrId: number, body: { paymentAmt: number; paymentDt: string }) =>
    api.post<any>(`/api/loan-contracts/${cntrId}/repayments`, body),

  partialPrepay: (cntrId: number, body: { prepaymentAmt: number }) =>
    api.post<any>(`/api/loan-contracts/${cntrId}/repayments/partial`, body),

  fullPrepay: (cntrId: number, body: object) =>
    api.post<any>(`/api/loan-contracts/${cntrId}/prepayments`, body),

  reverse: (cntrId: number, rtxId: number) =>
    api.post<any>(`/api/loan-contracts/${cntrId}/repayments/${rtxId}/reversal`, {}),
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
    api.post<any>(`/api/loan-contracts/${cntrId}/maturity`, body),

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

  getGuaranteeInsurance: (cntrId: number) =>
    api.get<any>(`/api/loan-contracts/${cntrId}/guarantee-insurance`),
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

export function getCustomerId(): number | null {
  if (typeof window === "undefined") return null;
  const val = localStorage.getItem("customerId");
  return val ? parseInt(val) : null;
}
