'use client'

import {
  type FormEvent,
  type PointerEvent as ReactPointerEvent,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react'
import { createPortal } from 'react-dom'
import { Bot, MessageCircle, Phone, Send, Sparkles, X } from 'lucide-react'
import {
  ChatbotButton,
  ChatbotFeatureExecuteResponse,
  executeChatbotFeature,
  sendChatbotMessage,
  startChatbotConsultation,
} from '@/lib/consultation-api'
import ConsultModal from '@/components/layout/ConsultModal'

type ChatMessage = {
  id: string
  role: 'bot' | 'user' | 'system'
  text: string
  buttons?: ChatbotButton[]
  data?: Record<string, unknown>[]
}

type ExpandedRow = {
  key: string
  title: string
  row: Record<string, unknown>
}

const DEFAULT_CUSTOMER_NO = 'CUST001'

const TEXT = {
  deposit: '\uC608\uAE08',
  savings: '\uC801\uAE08',
  subscription: '\uCCAD\uC57D',
  saving: '\uC800\uCD95',
  recommend: '\uC0C1\uD488 \uCD94\uCC9C',
  cashflow: '\uCD5C\uADFC \uD604\uAE08\uD750\uB984',
  detail: '\uC0C1\uC138',
  won: '\uC6D0',
  count: '\uAC74',
  item: '\uD56D\uBAA9',
  title: 'AX\uD480\uB31C\uD06C \uC0C1\uB2F4 \uCC57\uBD07',
  subtitle: '\uD604\uAE08 \uD750\uB984 \uBD84\uC11D\uACFC \uC0C1\uD488 \uCD94\uCC9C',
  openChat: '\uCC57\uBD07 \uC0C1\uB2F4 \uC5F4\uAE30',
  closeChat: '\uCC57\uBD07 \uC0C1\uB2F4 \uB2EB\uAE30',
  closeDetail: '\uC0C1\uC138 \uB2EB\uAE30',
  inputPlaceholder: '\uAD81\uAE08\uD55C \uB0B4\uC6A9\uC744 \uC785\uB825\uD558\uC138\uC694',
  sendMessage: '\uBA54\uC2DC\uC9C0 \uBCF4\uB0B4\uAE30',
  loading: '\uC751\uB2F5\uC744 \uC900\uBE44\uD558\uACE0 \uC788\uC2B5\uB2C8\uB2E4.',
  prev: '\uC774\uC804',
  next: '\uB2E4\uC74C',
}

const DATA_PAGE_SIZE = 10

const PRODUCT_CHOICES = [
  { label: TEXT.deposit,      query: '\uC608\uAE08 \uC0C1\uD488 \uC54C\uB824\uC918' },
  { label: TEXT.savings,      query: '\uC801\uAE08 \uC0C1\uD488 \uC54C\uB824\uC918' },
  { label: TEXT.subscription, query: '\uCCAD\uC57D \uC0C1\uD488 \uC54C\uB824\uC918' },
  { label: TEXT.saving,       query: '\uC800\uCD95 \uC0C1\uD488 \uC54C\uB824\uC918' },
]

const FIELD_LABELS: Record<string, string> = {
  transaction_id: '\uAC70\uB798\uBC88\uD638',
  transaction_number: '\uAC70\uB798\uAD00\uB9AC\uBC88\uD638',
  account_number: '\uACC4\uC88C\uBC88\uD638',
  customer_no: '\uACE0\uAC1D\uBC88\uD638',
  transaction_type: '\uAC70\uB798\uAD6C\uBD84',
  amount: '\uAC70\uB798\uAE08\uC561',
  transaction_status: '\uAC70\uB798\uC0C1\uD0DC',
  created_at: '\uAC70\uB798\uC77C\uC2DC',
  total_balance: '\uCD1D \uC794\uC561',
  monthly_surplus: '\uC6D4\uD3C9\uADE0 \uC789\uC5EC\uC790\uAE08',
  monthly_tx_count: '\uC6D4\uD3C9\uADE0 \uAC70\uB798\uAC74\uC218',
  has_data: '\uD604\uAE08\uD750\uB984 \uB370\uC774\uD130',
  product_count: '\uCD94\uCC9C \uAC00\uB2A5 \uC0C1\uD488 \uC218',
  rank: '\uC21C\uC704',
  product_name: '\uC0C1\uD488\uBA85',
  product_type: '\uC0C1\uD488\uAD6C\uBD84',
  base_interest_rate: '\uAE30\uBCF8\uAE08\uB9AC',
  min_join_amount: '\uCD5C\uC18C \uAC00\uC785\uAE08\uC561',
  max_join_amount: '\uCD5C\uB300 \uAC00\uC785\uAE08\uC561',
  min_period_month: '\uCD5C\uC18C \uAE30\uAC04',
  max_period_month: '\uCD5C\uB300 \uAE30\uAC04',
  target_groups: '\uAC00\uC785 \uB300\uC0C1',
  is_early_termination_allowed: '\uC911\uB3C4\uD574\uC9C0',
  is_tax_benefit_available: '\uC138\uAE08 \uD61C\uD0DD',
  is_auto_renewal_available: '\uC790\uB3D9 \uAC31\uC2E0',
  product_desc: '\uC0C1\uD488 \uC124\uBA85',
}

const VALUE_LABELS: Record<string, string> = {
  DEPOSIT: '\uC785\uAE08',
  WITHDRAWAL: '\uCD9C\uAE08',
  TRANSFER: '\uC774\uCCB4',
  SAVINGS: '\uC801\uAE08',
  SUBSCRIPTION: '\uCCAD\uC57D',
  SUCCESS: '\uC131\uACF5',
  COMPLETED: '\uC644\uB8CC',
  PENDING: '\uCC98\uB9AC\uC911',
  FAILED: '\uC2E4\uD328',
}

function messageId(prefix: string) {
  return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function formatFieldLabel(key: string) {
  return FIELD_LABELS[key] || key
}

function formatDisplayValue(key: string, value: unknown) {
  if (value === null || value === undefined || value === '') return '-'
  if (typeof value === 'boolean') return value ? '\uC788\uC74C' : '\uC5C6\uC74C'
  if (typeof value === 'number') {
    const formatted = Math.round(value).toLocaleString('ko-KR')
    if (key.includes('amount') || key.includes('balance') || key.includes('surplus')) return `${formatted}${TEXT.won}`
    if (key.includes('count')) return `${formatted}${TEXT.count}`
    if (key.includes('period_month')) return `${formatted}\uAC1C\uC6D4`
    if (key === 'base_interest_rate') return `${value}%`
    if (key === 'rank') return `${formatted}\uC21C\uC704`
    return formatted
  }

  const text = String(value)
  if (VALUE_LABELS[text]) return VALUE_LABELS[text]
  if (key === 'created_at') return text.replace('T', ' ').slice(0, 16)
  if (key === 'base_interest_rate') return `${text}%`
  if (/^-?\d+(\.\d+)?$/.test(text) && (key.includes('amount') || key.includes('balance') || key.includes('surplus'))) {
    return `${Number(text).toLocaleString('ko-KR')}${TEXT.won}`
  }
  if (/^-?\d+(\.\d+)?$/.test(text) && key.includes('period_month')) return `${Number(text).toLocaleString('ko-KR')}\uAC1C\uC6D4`
  return text
}

function dataTitle(row: Record<string, unknown>, index: number) {
  return (
    row.deposit_product_name ||
    row.product_name ||
    row.account_number ||
    row.transaction_number ||
    row.transaction_id ||
    `${TEXT.item} ${index + 1}`
  )
}

function rowSummary(row: Record<string, unknown>, index: number) {
  if (row.row_type === 'recommended_product') {
    return {
      title: `${formatDisplayValue('rank', row.rank)} · ${formatDisplayValue('product_name', row.product_name)}`,
      meta: `${formatDisplayValue('product_type', row.product_type)} · ${formatFieldLabel('base_interest_rate')} ${formatDisplayValue('base_interest_rate', row.base_interest_rate)}`,
    }
  }

  if (row.row_type === 'cash_flow_summary') {
    return {
      title: '\uD604\uAE08\uD750\uB984 \uBD84\uC11D \uC694\uC57D',
      meta: `${formatFieldLabel('monthly_surplus')} ${formatDisplayValue('monthly_surplus', row.monthly_surplus)}`,
    }
  }

  if ('product_name' in row && 'base_interest_rate' in row) {
    const period = row.min_period_month && row.max_period_month
      ? `${row.min_period_month}~${row.max_period_month}개월`
      : row.min_period_month
        ? `${row.min_period_month}개월 이상`
        : '기간 무관'
    return {
      title: String(row.product_name || '상품'),
      meta: `${formatDisplayValue('base_interest_rate', row.base_interest_rate)} · ${period} · ${row.target_groups || '개인고객'}`,
    }
  }

  if ('transaction_id' in row || 'transaction_type' in row) {
    return {
      title: `${formatDisplayValue('transaction_type', row.transaction_type)} ${formatDisplayValue('amount', row.amount)}`,
      meta: `${formatDisplayValue('created_at', row.created_at)} · ${formatDisplayValue('transaction_status', row.transaction_status)}`,
    }
  }

  return {
    title: String(dataTitle(row, index)),
    meta: Object.entries(row)
      .slice(0, 2)
      .map(([key, value]) => `${formatFieldLabel(key)} ${formatDisplayValue(key, value)}`)
      .join(' · '),
  }
}

function addFeatureResult(result: ChatbotFeatureExecuteResponse): ChatMessage {
  return {
    id: messageId(result.feature_code),
    role: 'bot',
    text: result.message || '\uC694\uCCAD\uD558\uC2E0 \uB0B4\uC6A9\uC744 \uD655\uC778\uD588\uC2B5\uB2C8\uB2E4.',
    data: result.data,
  }
}

export default function ChatbotWidget() {
  const [mounted, setMounted] = useState(false)
  const [open, setOpen] = useState(false)
  const [showConsult, setShowConsult] = useState(false)
  const [loading, setLoading] = useState(false)
  const [input, setInput] = useState('')
  const [customerNo, setCustomerNo] = useState(DEFAULT_CUSTOMER_NO)
  const [chatbotConsultationId, setChatbotConsultationId] = useState<number | null>(null)
  const [expandedRow, setExpandedRow] = useState<ExpandedRow | null>(null)
  const [dataPages, setDataPages] = useState<Record<string, number>>({})
  const [panelOffset, setPanelOffset] = useState({ x: 0, y: 0 })
  const dragRef = useRef<{ startX: number; startY: number; originX: number; originY: number } | null>(null)
  const scrollRef = useRef<HTMLDivElement>(null)
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'welcome',
      role: 'bot',
      text: '\uC548\uB155\uD558\uC138\uC694. \uC6D0\uD558\uC2DC\uB294 \uC0C1\uD488\uAD70\uC744 \uACE0\uB974\uAC70\uB098 \uD604\uAE08 \uD750\uB984 \uAE30\uBC18 \uC0C1\uD488 \uCD94\uCC9C\uC744 \uBC1B\uC544\uBCF4\uC138\uC694.',
    },
  ])

  const hasStarted = chatbotConsultationId !== null

  const quickActions = useMemo(
    () => [
      ...PRODUCT_CHOICES.map((choice) => ({ type: 'product_guide' as const, ...choice })),
      { type: 'recommend' as const, label: TEXT.recommend, message: '\uB0B4 \uD604\uAE08 \uD750\uB984\uC5D0 \uB9DE\uB294 \uC0C1\uD488\uC744 \uCD94\uCC9C\uD574\uC918' },
      { type: 'cashflow' as const, label: TEXT.cashflow, message: '\uCD5C\uADFC \uD604\uAE08 \uD750\uB984\uC744 \uC54C\uB824\uC918' },
      { type: 'consult' as const, label: '\uC0C1\uB2F4\uC6D0 \uC5F0\uACB0', message: '' },
    ],
    [],
  )

  useEffect(() => {
    setMounted(true)
  }, [])

  useEffect(() => {
    function handlePointerMove(event: globalThis.PointerEvent) {
      const drag = dragRef.current
      if (!drag) return
      setPanelOffset({
        x: drag.originX + event.clientX - drag.startX,
        y: drag.originY + event.clientY - drag.startY,
      })
    }

    function handlePointerUp() {
      dragRef.current = null
    }

    window.addEventListener('pointermove', handlePointerMove)
    window.addEventListener('pointerup', handlePointerUp)
    return () => {
      window.removeEventListener('pointermove', handlePointerMove)
      window.removeEventListener('pointerup', handlePointerUp)
    }
  }, [])

  function pushMessages(next: ChatMessage[]) {
    setMessages((current) => [...current, ...next])
    window.setTimeout(() => {
      scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
    }, 50)
  }

  function startPanelDrag(event: ReactPointerEvent<HTMLElement>) {
    if (event.button !== 0) return
    dragRef.current = {
      startX: event.clientX,
      startY: event.clientY,
      originX: panelOffset.x,
      originY: panelOffset.y,
    }
    event.currentTarget.setPointerCapture?.(event.pointerId)
  }

  function setMessagePage(messageIdValue: string, page: number) {
    setExpandedRow(null)
    setDataPages((current) => ({ ...current, [messageIdValue]: page }))
  }

  async function ensureStarted() {
    if (chatbotConsultationId) return chatbotConsultationId
    const started = await startChatbotConsultation(customerNo.trim() || DEFAULT_CUSTOMER_NO)
    setChatbotConsultationId(started.chatbot_consultation_id)
    pushMessages([
      {
        id: messageId('start'),
        role: 'bot',
        text: started.message,
        buttons: started.buttons,
      },
    ])
    return started.chatbot_consultation_id
  }

  async function handleScenarioMessage(text: string, buttonValue?: string) {
    setLoading(true)
    pushMessages([{ id: messageId('user'), role: 'user', text }])
    try {
      const consultationId = await ensureStarted()
      const reply = await sendChatbotMessage(consultationId, {
        message: text,
        button_value: buttonValue ?? null,
      })
      pushMessages([{ id: messageId('bot'), role: 'bot', text: reply.message, buttons: reply.buttons }])
    } catch {
      pushMessages([{ id: messageId('error'), role: 'system', text: '\uCC57\uBD07 \uC11C\uBC84 \uC5F0\uACB0\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4. \uC0C1\uB2F4 \uC11C\uBE44\uC2A4\uB97C \uD655\uC778\uD574\uC8FC\uC138\uC694.' }])
    } finally {
      setLoading(false)
    }
  }

  async function handleFeature(featureCode: 'MY_CASH_FLOW' | 'CASH_FLOW_RECOMMEND' | 'PRODUCT_GUIDE', userText: string, replaceMessages = false) {
    setLoading(true)
    if (replaceMessages) {
      setExpandedRow(null)
      setDataPages({})
      setMessages([{ id: messageId('user'), role: 'user', text: userText }])
    } else {
      pushMessages([{ id: messageId('user'), role: 'user', text: userText }])
    }
    try {
      const consultationId = hasStarted ? chatbotConsultationId : undefined
      const result = await executeChatbotFeature(featureCode, {
        customer_no: customerNo.trim() || DEFAULT_CUSTOMER_NO,
        query: userText,
        chatbot_consultation_id: consultationId ?? undefined,
      })
      if (replaceMessages) {
        setMessages((current) => [...current, addFeatureResult(result)])
      } else {
        pushMessages([addFeatureResult(result)])
      }
    } catch {
      const errorMessage: ChatMessage = {
        id: messageId('error'),
        role: 'system',
        text: '\uC694\uCCAD \uCC98\uB9AC \uC911 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uC7A0\uC2DC \uD6C4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574\uC8FC\uC138\uC694.',
      }
      if (replaceMessages) {
        setMessages((current) => [...current, errorMessage])
      } else {
        pushMessages([errorMessage])
      }
    } finally {
      setLoading(false)
    }
  }

  async function handleQuickAction(action: (typeof quickActions)[number]) {
    if (loading) return
    if (action.type === 'consult') {
      setShowConsult(true)
      return
    }
    if (action.type === 'recommend') {
      await handleFeature('CASH_FLOW_RECOMMEND', action.message, true)
      return
    }
    if (action.type === 'cashflow') {
      await handleFeature('MY_CASH_FLOW', action.message, true)
      return
    }
    if (action.type === 'product_guide') {
      await handleFeature('PRODUCT_GUIDE', action.query, true)
      return
    }
    await handleScenarioMessage((action as { message: string }).message)
  }

  async function submitMessage(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const text = input.trim()
    if (!text || loading) return
    setInput('')
    await handleScenarioMessage(text)
  }

  const panel = open && mounted
    ? createPortal(
        <div className="fixed inset-0 z-[260] bg-black/20">
          <section
            className="fixed flex h-[680px] max-h-[calc(100vh-48px)] w-[420px] max-w-[calc(100vw-32px)] flex-col overflow-hidden rounded-lg border border-kb-border bg-white shadow-2xl"
            style={{
              left: '50vw',
              top: '50vh',
              transform: `translate(calc(-50% + ${panelOffset.x}px), calc(-50% + ${panelOffset.y}px))`,
            }}
          >
            <header
              onPointerDown={startPanelDrag}
              className="flex cursor-move select-none items-center justify-between border-b border-kb-border bg-[#2D6A4F] px-4 py-3 text-white"
            >
              <div className="flex items-center gap-2">
                <span className="flex h-9 w-9 items-center justify-center rounded-full bg-white/15">
                  <Bot className="h-5 w-5" />
                </span>
                <div>
                  <h2 className="text-sm font-bold">{TEXT.title}</h2>
                  <p className="text-xs text-white/75">{TEXT.subtitle}</p>
                </div>
              </div>
              <button
                type="button"
                onPointerDown={(event) => event.stopPropagation()}
                onClick={() => setOpen(false)}
                className="flex h-8 w-8 items-center justify-center rounded-full hover:bg-white/15"
                aria-label={TEXT.closeChat}
              >
                <X className="h-5 w-5" />
              </button>
            </header>

            <div className="border-b border-kb-border bg-[#F7F5EF] px-4 py-3">
              <label className="flex items-center gap-2 text-xs text-kb-text-muted">
                {FIELD_LABELS.customer_no}
                <input
                  value={customerNo}
                  onChange={(event) => setCustomerNo(event.target.value)}
                  className="h-8 flex-1 rounded border border-kb-border bg-white px-2 text-xs text-kb-text outline-none focus:border-[#2D6A4F]"
                />
              </label>
            </div>

            <div ref={scrollRef} className="flex-1 space-y-3 overflow-y-auto bg-[#FBFAF7] px-4 py-4">
              {messages.map((message) => (
                <div key={message.id} className={message.role === 'user' ? 'flex justify-end' : 'flex justify-start'}>
                  <div
                    className={`max-w-[88%] rounded-lg px-3 py-2 text-sm leading-relaxed ${
                      message.role === 'user'
                        ? 'bg-[#2D6A4F] text-white'
                        : message.role === 'system'
                          ? 'border border-red-200 bg-red-50 text-red-700'
                          : 'border border-kb-border bg-white text-kb-text'
                    }`}
                  >
                    <p className="whitespace-pre-wrap">{message.text}</p>

                    {message.data && message.data.length > 0 && (() => {
                      const page = dataPages[message.id] ?? 0
                      const totalPages = Math.ceil(message.data.length / DATA_PAGE_SIZE)
                      const startIndex = page * DATA_PAGE_SIZE
                      const visibleRows = message.data.slice(startIndex, startIndex + DATA_PAGE_SIZE)

                      return (
                      <div className="mt-3 space-y-2">
                        {visibleRows.map((row, index) => {
                          const absoluteIndex = startIndex + index
                          const summary = rowSummary(row, absoluteIndex)
                          const rowKey = `${message.id}-data-${absoluteIndex}`
                          const isOpen = expandedRow?.key === rowKey
                          return (
                            <div key={rowKey} className="rounded border border-kb-border bg-[#F7F5EF]">
                              <button
                                type="button"
                                onClick={() => setExpandedRow(isOpen ? null : { key: rowKey, title: summary.title, row })}
                                className="flex w-full items-center justify-between gap-3 px-3 py-2 text-left hover:bg-kb-beige"
                              >
                                <span className="min-w-0">
                                  <span className="block truncate text-xs font-bold text-kb-text">{summary.title}</span>
                                  <span className="block truncate text-[11px] text-kb-text-muted">{summary.meta}</span>
                                </span>
                                <span className="flex-none text-[11px] font-bold text-[#2D6A4F]">{TEXT.detail}</span>
                              </button>
                              {isOpen && (
                                <div className="border-t border-white bg-white px-3 py-2">
                                  <div className="mb-2 flex items-center justify-between">
                                    <p className="truncate text-xs font-bold text-kb-text">{expandedRow.title}</p>
                                    <button
                                      type="button"
                                      onClick={() => setExpandedRow(null)}
                                      className="text-[11px] font-bold text-[#2D6A4F]"
                                    >
                                      닫기
                                    </button>
                                  </div>
                                  <dl className="space-y-1 text-xs">
                                    {Object.entries(expandedRow.row)
                                      .filter(([key]) => key !== 'row_type')
                                      .map(([key, value]) => (
                                        <div key={key} className="flex justify-between gap-3 border-t border-gray-100 pt-1 first:border-t-0 first:pt-0">
                                          <dt className="flex-none text-kb-text-muted">{formatFieldLabel(key)}</dt>
                                          <dd className="min-w-0 text-right font-medium text-kb-text">{formatDisplayValue(key, value)}</dd>
                                        </div>
                                      ))}
                                  </dl>
                                </div>
                              )}
                            </div>
                          )
                        })}
                        {totalPages > 1 && (
                          <div className="flex items-center justify-between rounded border border-kb-border bg-white px-3 py-2 text-xs">
                            <button
                              type="button"
                              onClick={() => setMessagePage(message.id, Math.max(0, page - 1))}
                              disabled={page === 0}
                              className="font-bold text-[#2D6A4F] disabled:text-gray-300"
                            >
                              {TEXT.prev}
                            </button>
                            <span className="text-kb-text-muted">
                              {page + 1} / {totalPages}
                            </span>
                            <button
                              type="button"
                              onClick={() => setMessagePage(message.id, Math.min(totalPages - 1, page + 1))}
                              disabled={page >= totalPages - 1}
                              className="font-bold text-[#2D6A4F] disabled:text-gray-300"
                            >
                              {TEXT.next}
                            </button>
                          </div>
                        )}
                      </div>
                      )
                    })()}

                    {message.buttons && message.buttons.length > 0 && (
                      <div className="mt-3 flex flex-wrap gap-2">
                        {message.buttons.map((button) => (
                          <button
                            key={button.id}
                            type="button"
                            onClick={() => handleScenarioMessage(button.text, button.value)}
                            className="rounded border border-[#2D6A4F] bg-white px-2.5 py-1 text-xs font-medium text-[#2D6A4F] hover:bg-[#EAF4EF]"
                          >
                            {button.text}
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              ))}
              {loading && (
                <div className="flex justify-start">
                  <div className="rounded-lg border border-kb-border bg-white px-3 py-2 text-xs text-kb-text-muted">{TEXT.loading}</div>
                </div>
              )}
            </div>

            <div className="border-t border-kb-border bg-white px-4 py-3">
              <div className="mb-3 grid grid-cols-3 gap-2">
                {quickActions.map((action) => (
                  <button
                    key={`${action.type}-${action.label}`}
                    type="button"
                    onClick={() => handleQuickAction(action)}
                    disabled={loading}
                    className={`flex min-h-9 items-center justify-center gap-1 rounded border px-2 text-xs font-bold transition disabled:opacity-60 ${
                      action.type === 'recommend'
                        ? 'border-[#C09B3A] bg-[#FFF8DA] text-[#7A5200] hover:bg-[#FFEFA7]'
                        : action.type === 'consult'
                          ? 'border-[#2D6A4F] bg-white text-[#2D6A4F] hover:bg-[#EAF4EF]'
                          : 'border-kb-border bg-[#F7F5EF] text-kb-text hover:bg-kb-beige'
                    }`}
                  >
                    {action.type === 'recommend' && <Sparkles className="h-3.5 w-3.5" />}
                    {action.type === 'consult' && <Phone className="h-3.5 w-3.5" />}
                    {action.label}
                  </button>
                ))}
              </div>

              <form onSubmit={submitMessage} className="flex gap-2">
                <input
                  value={input}
                  onChange={(event) => setInput(event.target.value)}
                  placeholder={TEXT.inputPlaceholder}
                  className="h-10 flex-1 rounded border border-kb-border px-3 text-sm outline-none focus:border-[#2D6A4F]"
                />
                <button
                  type="submit"
                  disabled={loading || !input.trim()}
                  className="flex h-10 w-10 items-center justify-center rounded bg-[#2D6A4F] text-white transition hover:bg-[#24563F] disabled:bg-gray-300"
                  aria-label={TEXT.sendMessage}
                >
                  <Send className="h-4 w-4" />
                </button>
              </form>
            </div>

          </section>
        </div>,
        document.body,
      )
    : null

  return (
    <>
      {showConsult && <ConsultModal onClose={() => setShowConsult(false)} />}
      <button
        type="button"
        onClick={() => {
          setPanelOffset({ x: 0, y: 0 })
          setOpen(true)
        }}
        className="fixed bottom-6 right-6 z-[220] flex h-16 w-32 items-center justify-center gap-2 rounded-full bg-red-600 text-white shadow-xl transition hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500"
        aria-label={TEXT.openChat}
      >
        <MessageCircle className="h-7 w-7" />
        <span className="font-bold">CHATBOT</span>
      </button>
      {panel}
    </>
  )
}
