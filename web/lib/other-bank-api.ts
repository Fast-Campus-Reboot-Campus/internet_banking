import axios from 'axios'

// payment-service-b 직접 호출 (gateway 경유 없음, 시연용)
const paymentBApi = axios.create({
  baseURL: 'http://localhost:8180',
  headers: { 'Content-Type': 'application/json' },
})

export type InboundPayment = {
  paymentInstructionId: string
  transactionNo: string
  transferAmount: number
  status: string
  requestedAt: string
  completedAt: string | null
  senderAccountNoSnap: string | null
  receiverPassbookSenderDisplay: string | null
  receiverMemo: string | null
}

export async function fetchInboundPayments(receiverAccountNo: string): Promise<InboundPayment[]> {
  const res = await paymentBApi.get<InboundPayment[]>('/api/v1/payments/inbound', {
    params: { receiverAccountNo },
  })
  return res.data
}
