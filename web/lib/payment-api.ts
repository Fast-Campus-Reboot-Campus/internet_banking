import axios from 'axios'

const paymentApi = axios.create({
  baseURL: process.env.NEXT_PUBLIC_PAYMENT_API_URL || 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' },
})

paymentApi.interceptors.request.use((config) => {
  if (typeof window === 'undefined') return config
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

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
