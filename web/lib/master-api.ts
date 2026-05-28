import axios from 'axios'

const masterApi = axios.create({
  baseURL: process.env.NEXT_PUBLIC_MASTER_API_URL || 'http://localhost:8085',
  headers: { 'Content-Type': 'application/json' },
})

masterApi.interceptors.request.use((config) => {
  if (typeof window === 'undefined') return config
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// ── 공통 코드 ─────────────────────────────────────────────────────────────

export type CommonCode = {
  codeId: number
  groupCd: string
  codeCd: string
  codeNm: string
  codeDesc?: string
  sortOrder?: number
  isActive: boolean
}

export async function getCode(groupCd: string, codeCd: string) {
  const { data } = await masterApi.get<CommonCode>(`/api/codes/${groupCd}/${codeCd}`)
  return data
}

export async function getCodes(params?: { groupCd?: string }) {
  const { data } = await masterApi.get<CommonCode[]>('/api/codes', { params })
  return data
}

export async function createCode(payload: Omit<CommonCode, 'codeId'>) {
  const { data } = await masterApi.post<CommonCode>('/api/codes', payload)
  return data
}

export async function updateCode(codeId: number, payload: Partial<CommonCode>) {
  const { data } = await masterApi.put<CommonCode>(`/api/codes/${codeId}`, payload)
  return data
}

export async function deleteCode(codeId: number) {
  const { data } = await masterApi.delete(`/api/codes/${codeId}`)
  return data
}
