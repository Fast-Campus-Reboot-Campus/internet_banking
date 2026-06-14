import axios from 'axios'
import { executeDepositTransfer } from '@/lib/deposit-api'

export type ChatbotButton = {
  id: number
  text: string
  value: string
}

export type ChatbotStartResponse = {
  consultation_id: number
  chatbot_consultation_id: number
  node_id: number
  message: string
  buttons: ChatbotButton[]
}

export type ChatbotMessageResponse = ChatbotStartResponse & {
  process_method: string
  agent_transfer_required: boolean
  feature_code?: string
  feature_data?: Record<string, unknown>[]
}

export type ChatbotFeatureExecuteRequest = {
  customer_no?: string
  query?: string
  product_id?: number
  compare_product_ids?: number[]
  staff_id?: string
  chatbot_consultation_id?: number
  amount?: number
  period?: number
  product_type?: string
  purpose?: string
}

export type ChatbotFeatureExecuteResponse = {
  feature_code: string
  status: string
  message: string
  data: Record<string, unknown>[]
  requires_auth: boolean
  requires_staff_auth: boolean
}

const consultationApi = axios.create({
  baseURL: process.env.NEXT_PUBLIC_CONSULTATION_API_URL || '/api/consultation',
  headers: { 'Content-Type': 'application/json' },
})

export async function startChatbotConsultation(customerNo: string) {
  const { data } = await consultationApi.post<ChatbotStartResponse>('/chatbot/consultations/start', {
    customer_no: customerNo,
    entry_screen: 'WEB_PERSONAL',
    app_version: '0.1.0',
  })
  return data
}

export async function sendChatbotMessage(
  chatbotConsultationId: number,
  payload: { message: string; button_value?: string | null },
) {
  const { data } = await consultationApi.post<ChatbotMessageResponse>(
    `/chatbot/consultations/${chatbotConsultationId}/messages`,
    payload,
  )
  return data
}

export async function executeChatbotFeature(
  featureCode: string,
  payload: ChatbotFeatureExecuteRequest,
) {
  const { data } = await consultationApi.post<ChatbotFeatureExecuteResponse>(
    `/chatbot/features/${featureCode}/execute`,
    payload,
  )
  return data
}

// ── 상담사 채팅 ──────────────────────────────────────────────────────────────

export type AgentQueueItem = {
  chat_consultation_id: number
  consultation_id: number
  customer_no: string
  chatbot_consultation_id: number | null
  waiting_since: string | null
}

export type ChatMessage = {
  message_id: number
  sender_type: 'USER' | 'BOT' | 'AGENT'
  message: string
  sent_at: string | null
  read_yn: string
}

export type ChatConsultation = {
  chat_consultation_id: number
  consultation_id: number
  chatbot_consultation_id: number | null
  status: 'WAITING' | 'CONNECTED' | 'ENDED'
  employee_id: number | null
  agent_requested_at: string | null
  agent_connected_at: string | null
  chat_started_at: string | null
  chat_ended_at: string | null
  active_yn: string
  satisfaction_score: number | null
}

export async function getAgentQueue(): Promise<AgentQueueItem[]> {
  const { data } = await consultationApi.get<AgentQueueItem[]>('/chat/queue')
  return data
}

export async function connectAgent(chatConsultationId: number, employeeId: number): Promise<ChatConsultation> {
  const { data } = await consultationApi.post<ChatConsultation>(
    `/chat/consultations/${chatConsultationId}/connect`,
    { employee_id: employeeId },
  )
  return data
}

export async function sendChatMessage(chatConsultationId: number, message: string, senderType: 'USER' | 'AGENT'): Promise<ChatMessage> {
  const { data } = await consultationApi.post<ChatMessage>(
    `/chat/consultations/${chatConsultationId}/messages`,
    { message, sender_type: senderType },
  )
  return data
}

export async function getChatMessages(chatConsultationId: number): Promise<ChatMessage[]> {
  const { data } = await consultationApi.get<ChatMessage[]>(
    `/chat/consultations/${chatConsultationId}/messages`,
  )
  return data
}

export type TransferResult = {
  status: string
  message: string
  transaction_id: number | null
  balance_after: number | null
}

export async function executeChatbotTransfer(payload: {
  customer_no: string
  from_account_id: number
  to_account_id?: number
  to_account_number: string
  to_bank_name?: string
  amount: number
  memo?: string
}): Promise<TransferResult> {
  const { data } = await consultationApi.post<TransferResult>('/chatbot/transfer', {
    customer_no: payload.customer_no,
    from_account_id: payload.from_account_id,
    to_account_number: payload.to_account_number,
    amount: payload.amount,
    memo: payload.memo ?? '이체',
  })
  return data
}

export async function endChat(chatConsultationId: number, satisfactionScore?: number): Promise<ChatConsultation> {
  const { data } = await consultationApi.post<ChatConsultation>(
    `/chat/consultations/${chatConsultationId}/end`,
    { satisfaction_score: satisfactionScore ?? null },
  )
  return data
}

// ── 파일 분석 / 서류 제출 ──────────────────────────────────────────────────────

export type FileAnalyzeResponse = {
  analyze_type: string
  result: string
}

export type DocumentUploadResponse = {
  document_id: number
  filename: string
  doc_type: string
  status: string
  message: string
}

export async function analyzeFile(
  text: string,
  analyzeType: 'CASH_FLOW' | 'TERMS' | 'PRODUCT',
  customerNo?: string,
): Promise<FileAnalyzeResponse> {
  const { data } = await consultationApi.post<FileAnalyzeResponse>('/chatbot/file/analyze', {
    text,
    analyze_type: analyzeType,
    customer_no: customerNo,
  })
  return data
}

export async function uploadDocument(
  file: File,
  customerNo: string,
  docType: string = 'ENROLLMENT',
): Promise<DocumentUploadResponse> {
  const form = new FormData()
  form.append('file', file)
  form.append('customer_no', customerNo)
  form.append('doc_type', docType)
  const { data } = await consultationApi.post<DocumentUploadResponse>('/chatbot/documents/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}
