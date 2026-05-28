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

// ── 결제 ──────────────────────────────────────────────────────────────────

export type PaymentStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'

export type Payment = {
  piId: number
  customerId: string
  amount: number
  status: PaymentStatus
  createdAt: string
}

export async function createPayment(payload: {
  customerId: string
  amount: number
  paymentMethod: string
  merchantId?: string
  description?: string
}) {
  const { data } = await paymentApi.post<Payment>('/api/v1/payments', payload)
  return data
}

export async function cancelPayment(piId: number, reason?: string) {
  const { data } = await paymentApi.post(`/api/v1/payments/${piId}/operator-cancel`, { reason })
  return data
}
