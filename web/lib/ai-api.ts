import axios from 'axios'

const aiApi = axios.create({
  baseURL: process.env.NEXT_PUBLIC_AI_API_URL || 'http://localhost:8086',
  headers: { 'Content-Type': 'application/json' },
})

aiApi.interceptors.request.use((config) => {
  if (typeof window === 'undefined') return config
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// ── RAG 검색 ──────────────────────────────────────────────────────────────

export type RagSearchResult = {
  docId: string
  content: string
  score: number
  metadata?: Record<string, unknown>
}

export async function ragSearch(payload: { query: string; topK?: number }) {
  const { data } = await aiApi.post<RagSearchResult[]>('/rag/search', payload)
  return data
}

// ── RAG 문서 관리 (관리자) ────────────────────────────────────────────────

export type RagDocument = {
  docId: string
  title: string
  content: string
  status: string
  createdAt: string
}

export async function getRagDocuments() {
  const { data } = await aiApi.get<RagDocument[]>('/internal/rag/documents')
  return data
}

export async function getRagDocument(docId: string) {
  const { data } = await aiApi.get<RagDocument>(`/internal/rag/documents/${docId}`)
  return data
}

export async function uploadRagDocument(payload: { title: string; content: string }) {
  const { data } = await aiApi.post<RagDocument>('/internal/rag/documents', payload)
  return data
}

export async function reindexRagDocument(docId: string) {
  const { data } = await aiApi.post(`/internal/rag/documents/${docId}/reindex`)
  return data
}

export async function getRagDocumentIngestionLogs(docId: string) {
  const { data } = await aiApi.get(`/internal/rag/documents/${docId}/ingestion-logs`)
  return data
}

export async function bootstrapRagDocuments() {
  const { data } = await aiApi.post('/internal/rag/documents/bootstrap')
  return data
}
