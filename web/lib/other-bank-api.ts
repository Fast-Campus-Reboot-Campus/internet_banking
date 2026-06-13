import axios from 'axios'

// payment-service-b 호출 — Next.js BFF 프록시(/api/other-bank) 경유.
// 배포(HTTPS)에서 브라우저가 HTTP 백엔드를 직접 부르면 Mixed Content 로 차단되므로
// same-origin 라우트가 중계한다. 실제 백엔드 주소는 서버 전용 env PAYMENT_B_API_URL.
const paymentBApi = axios.create({
  baseURL: '/api/other-bank',
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
