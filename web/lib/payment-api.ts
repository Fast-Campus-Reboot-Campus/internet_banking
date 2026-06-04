import axios from 'axios'

const paymentApi = axios.create({
  baseURL: process.env.NEXT_PUBLIC_PAYMENT_API_URL || 'http://localhost:8087',
  headers: { 'Content-Type': 'application/json' },
})

paymentApi.interceptors.request.use((config) => {
  if (typeof window === 'undefined') return config
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// ── 즉시이체 ──────────────────────────────────────────────────────────────

export type InstantTransferPayload = {
  senderAccountId: string
  receiverBankCode: string
  receiverAccountNo: string
  receiverHolderName: string
  transferAmount: number
  receiverMemo: string | null
  senderMemo: string | null
  channel: string
  receiverPassbookSenderDisplay: string | null
}

export type TransferRequestHeaders = {
  userId: string
  authTokenId: string
  idempotencyKey: string
}

export type InstantTransferResult = {
  paymentInstructionId: string
  transactionNo: string
  status: string
  completedAt: string | null
  failureCategory: string | null
}

// 요청 멱등키 — 이체 "의도"당 1회 생성해 재시도 시 동일 키를 재사용한다.
// (중복 제출 방지용 클라이언트 요청 키. 결제계 내부 deposit 호출 멱등키와는 별개.)
export function newIdempotencyKey(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return `transfer-${crypto.randomUUID()}`
  }
  return `transfer-${Date.now()}-${Math.random().toString(36).slice(2)}`
}

export async function createInstantTransfer(
  payload: InstantTransferPayload,
  headers: TransferRequestHeaders
): Promise<InstantTransferResult> {
  const { data } = await paymentApi.post<InstantTransferResult>(
    '/api/v1/payments',
    payload,
    {
      headers: {
        'X-User-Id': headers.userId,
        'X-Auth-Token-Id': headers.authTokenId,
        'X-Idempotency-Key': headers.idempotencyKey,
      },
    }
  )
  return data
}

// ── 결제(운영자용) ─────────────────────────────────────────────────────────

export type PaymentStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'

export type Payment = {
  piId: number
  customerId: string
  amount: number
  status: PaymentStatus
  createdAt: string
}

export async function cancelPayment(piId: number, reason?: string) {
  const { data } = await paymentApi.post(`/api/v1/payments/${piId}/operator-cancel`, { reason })
  return data
}
