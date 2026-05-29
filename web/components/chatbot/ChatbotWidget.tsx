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
import { api } from '@/lib/api'
import { ArrowLeftRight, Bot, Home, MessageCircle, PackageSearch, Phone, Send, Sparkles, X } from 'lucide-react'
import {
  ChatbotButton,
  ChatbotFeatureExecuteResponse,
  executeChatbotFeature,
  executeChatbotTransfer,
  sendChatbotMessage,
  startChatbotConsultation,
} from '@/lib/consultation-api'
import { getCurrentDepositCustomerId, fetchDepositAccountViewModels } from '@/lib/deposit-api'
import ConsultModal from '@/components/layout/ConsultModal'

type ChatMessage = {
  id: string
  role: 'bot' | 'user' | 'system'
  text: string
  buttons?: ChatbotButton[]
  data?: Record<string, unknown>[]
  link?: { text: string; href: string }
  featureCode?: string
  loginForm?: boolean
}

type TransferStep = 'form' | 'confirm' | 'processing' | 'done' | 'error'

type MyAccount = { account_id: number; account_number: string; balance: number; account_alias: string | null }

type TransferState = {
  step: TransferStep
  fromAccountId: number
  fromAccountNumber: string
  fromBalance: number
  toAccountNumber: string
  toTab: 'direct' | 'my_accounts'
  myAccounts: MyAccount[]
  amount: string
  memo: string
  resultMessage: string
  balanceAfter: number | null
}

type ExpandedRow = {
  key: string
  title: string
  row: Record<string, unknown>
}

const DEFAULT_CUSTOMER_NO = ''

const TEXT = {
  deposit: '\uC608\uAE08',
  savings: '\uC801\uAE08',
  subscription: '\uCCAD\uC57D',
  saving: '\uC800\uCD95',
  recommend: '\uC0C1\uD488 \uCD94\uCC9C',
  cashflow: '\uCD5C\uADFC \uD604\uAE08\uD750\uB984',
  myProducts: '\uB0B4 \uC0C1\uD488',
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
    featureCode: result.feature_code,
    ...(result.requires_auth ? { loginForm: true } : {}),
  }
}

export default function ChatbotWidget() {
  const [mounted, setMounted] = useState(false)
  const [open, setOpen] = useState(false)
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [showConsult, setShowConsult] = useState(false)
  const [loading, setLoading] = useState(false)
  const [input, setInput] = useState('')
  const [customerNo, setCustomerNo] = useState(DEFAULT_CUSTOMER_NO)
  const [chatbotConsultationId, setChatbotConsultationId] = useState<number | null>(null)
  const [expandedRow, setExpandedRow] = useState<ExpandedRow | null>(null)
  const [dataPages, setDataPages] = useState<Record<string, number>>({})
  const [panelOffset, setPanelOffset] = useState({ x: 0, y: 0 })
  const [transferState, setTransferState] = useState<TransferState | null>(null)
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
      { type: 'my_products' as const, label: TEXT.myProducts, message: '\uB0B4 \uC0C1\uD488 \uBCF4\uC5EC\uC918' },
      { type: 'recommend' as const, label: TEXT.recommend, message: '\uB0B4 \uD604\uAE08 \uD750\uB984\uC5D0 \uB9DE\uB294 \uC0C1\uD488\uC744 \uCD94\uCC9C\uD574\uC918' },
      { type: 'cashflow' as const, label: TEXT.cashflow, message: '\uCD5C\uADFC \uD604\uAE08 \uD750\uB984\uC744 \uC54C\uB824\uC918' },
      { type: 'consult' as const, label: '\uC0C1\uB2F4\uC6D0 \uC5F0\uACB0', message: '' },
    ],
    [],
  )

  useEffect(() => {
    setMounted(true)
    const cid = localStorage.getItem('customerId')
    if (cid) setCustomerNo(cid)
    setIsLoggedIn(!!localStorage.getItem('accessToken') && !!localStorage.getItem('user'))
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

  async function handleFeature(featureCode: 'MY_ACCOUNTS' | 'MY_PRODUCTS' | 'MY_CASH_FLOW' | 'CASH_FLOW_RECOMMEND' | 'PRODUCT_GUIDE', userText: string, replaceMessages = false) {
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

    const AUTH_REQUIRED = new Set(['my_products', 'transfer', 'cashflow', 'recommend'])
    if (AUTH_REQUIRED.has(action.type)) {
      if (!localStorage.getItem('accessToken') || !localStorage.getItem('user')) {
        pushMessages([{
          id: messageId('auth'),
          role: 'bot',
          text: '로그인 후 이용하실 수 있는 서비스입니다.',
          loginForm: true,
        }])
        return
      }
    }

    if (action.type === 'consult') {
      setShowConsult(true)
      return
    }
    if (action.type === 'recommend') {
      await handleFeature('CASH_FLOW_RECOMMEND', action.message, true)
      return
    }
    if (action.type === 'my_products') {
      if (!customerNo.trim()) {
        pushMessages([{ id: messageId('auth'), role: 'bot', text: '로그인 후 이용하실 수 있는 서비스입니다.', loginForm: true }])
        return
      }
      setLoading(true)
      pushMessages([{ id: messageId('user'), role: 'user', text: action.message }])
      try {
        // 계좌 조회와 동일한 소스: deposit API → localStorage 순서
        let rows: Record<string, unknown>[] = []
        try {
          const apiAccounts = await fetchDepositAccountViewModels(customerNo)
          rows = apiAccounts.map((a) => ({
            account_id: a.apiAccountId ?? 0,
            account_number: a.number,
            product_name: a.name,
            product_type: a.type,
            saving_type: a.savingType ?? null,
            balance: a.balance,
            account_status: a.accountStatus ?? 'ACTIVE',
            maturity_at: a.maturityDate ?? null,
            started_at: a.createdAt ?? null,
            is_withdrawable: a.type === '입출금',
          }))
        } catch {
          // deposit API 미응답 시 localStorage fallback (계좌 조회와 동일한 방식)
          const raw = typeof window !== 'undefined' ? localStorage.getItem('joinedAccounts') : null
          if (raw) {
            const stored = JSON.parse(raw) as Array<Record<string, unknown>>
            rows = stored.map((a) => ({
              account_id: a.id ?? 0,
              account_number: a.number ?? '',
              product_name: a.name ?? '',
              product_type: a.type,
              balance: a.balance ?? 0,
              account_status: 'ACTIVE',
              maturity_at: a.maturityDate ?? null,
              started_at: a.createdAt ?? null,
              is_withdrawable: a.type === '입출금',
            }))
          }
        }
        const TYPE_ORDER: Record<string, number> = {
          '입출금': 0,
          '예금': 1,
          '정기적금': 2,
          '자유적금': 3,
          '적금': 2,
          '청약': 4,
        }
        const sortedRows = [...rows].sort((a, b) => {
          const typeA = a.product_type === '적금'
            ? (a.saving_type === 'FREE' ? '자유적금' : '정기적금')
            : String(a.product_type ?? '')
          const typeB = b.product_type === '적금'
            ? (b.saving_type === 'FREE' ? '자유적금' : '정기적금')
            : String(b.product_type ?? '')
          return (TYPE_ORDER[typeA] ?? 9) - (TYPE_ORDER[typeB] ?? 9)
        })
        pushMessages([{
          id: messageId('MY_PRODUCTS'),
          role: 'bot',
          text: sortedRows.length > 0 ? '가입 상품 조회를 완료했습니다.' : '조회된 가입 상품이 없습니다.',
          data: sortedRows,
          featureCode: 'MY_PRODUCTS',
        }])
      } catch {
        pushMessages([{ id: messageId('error'), role: 'system', text: '요청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.' }])
      } finally {
        setLoading(false)
      }
      return
    }
    if (action.type === 'transfer') {
      setLoading(true)
      pushMessages([{ id: messageId('user'), role: 'user', text: action.message }])
      try {
        const result = await executeChatbotFeature('MY_TRANSFERS', {
          customer_no: customerNo.trim() || DEFAULT_CUSTOMER_NO,
        })
        pushMessages([{
          ...addFeatureResult(result),
          link: { text: '새 이체 시작하기 →', href: '/transfer/account' },
        }])
      } catch {
        pushMessages([{ id: messageId('error'), role: 'system', text: '요청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.' }])
      } finally {
        setLoading(false)
      }
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

  async function startTransfer(accountId: number, accountNumber: string, balance: number) {
    setTransferState({
      step: 'form',
      fromAccountId: accountId,
      fromAccountNumber: accountNumber,
      fromBalance: balance,
      toAccountNumber: '',
      toTab: 'my_accounts',
      myAccounts: [],
      amount: '',
      memo: '이체',
      resultMessage: '',
      balanceAfter: null,
    })
    try {
      let allAccounts: MyAccount[] = []
      try {
        const apiAccounts = await fetchDepositAccountViewModels(getCurrentDepositCustomerId())
        allAccounts = apiAccounts.map((a) => ({
          account_id: a.apiAccountId ?? 0,
          account_number: a.number,
          balance: a.balance,
          account_alias: a.name ?? null,
        }))
      } catch {
        const raw = typeof window !== 'undefined' ? localStorage.getItem('joinedAccounts') : null
        if (raw) {
          const stored = JSON.parse(raw) as Array<Record<string, unknown>>
          allAccounts = stored.map((a) => ({
            account_id: Number(a.id ?? 0),
            account_number: String(a.number ?? ''),
            balance: Number(a.balance ?? 0),
            account_alias: a.name ? String(a.name) : null,
          }))
        }
      }
      const accounts: MyAccount[] = allAccounts.filter((a) => a.account_id !== accountId)
      setTransferState((s) => s && { ...s, myAccounts: accounts })
    } catch {
      // 계좌 로드 실패 시 직접 입력으로 전환
      setTransferState((s) => s && { ...s, toTab: 'direct' })
    }
  }

  async function confirmTransfer() {
    if (!transferState) return
    const amount = parseInt(transferState.amount.replace(/,/g, ''), 10)
    if (!amount || amount <= 0) return
    setTransferState((s) => s && { ...s, step: 'confirm' })
  }

  async function executeTransfer() {
    if (!transferState) return
    const amount = parseInt(transferState.amount.replace(/,/g, ''), 10)
    setTransferState((s) => s && { ...s, step: 'processing' })
    try {
      const result = await executeChatbotTransfer({
        customer_no: customerNo.trim() || DEFAULT_CUSTOMER_NO,
        from_account_id: transferState.fromAccountId,
        to_account_number: transferState.toAccountNumber,
        amount,
        memo: transferState.memo || '이체',
      })
      if (result.status === 'OK') {
        setTransferState((s) => s && { ...s, step: 'done', resultMessage: result.message, balanceAfter: result.balance_after })
        pushMessages([{ id: messageId('transfer'), role: 'bot', text: `✓ ${result.message}` }])
      } else {
        setTransferState((s) => s && { ...s, step: 'error', resultMessage: result.message })
      }
    } catch {
      setTransferState((s) => s && { ...s, step: 'error', resultMessage: '이체 처리 중 오류가 발생했습니다.' })
    }
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
            className="fixed flex w-[420px] max-w-[calc(100vw-32px)] flex-col overflow-hidden rounded-lg border border-kb-border bg-white shadow-2xl relative"
            style={{
              left: '50vw',
              top: `max(8px, calc(50vh - 340px + ${panelOffset.y}px))`,
              height: `min(680px, calc(100vh - 16px))`,
              transform: `translateX(calc(-50% + ${panelOffset.x}px))`,
            }}
          >
            <header
              onPointerDown={startPanelDrag}
              className="flex cursor-move select-none items-center justify-between border-b border-kb-border bg-[#2D6A4F] px-3 py-3 text-white"
            >
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onPointerDown={(e) => e.stopPropagation()}
                  onClick={() => {
                    setMessages([{ id: 'welcome', role: 'bot', text: '안녕하세요. 원하시는 상품군을 고르거나 현금 흐름 기반 상품 추천을 받아보세요.' }])
                    setChatbotConsultationId(null)
                    setExpandedRow(null)
                    setDataPages({})
                    setTransferState(null)
                  }}
                  className="flex h-8 w-8 items-center justify-center rounded-full hover:bg-white/15 flex-shrink-0"
                  aria-label="챗봇 홈"
                >
                  <Home className="h-4 w-4" />
                </button>
                <span className="flex h-8 w-8 items-center justify-center rounded-full bg-white/15 flex-shrink-0">
                  <Bot className="h-4 w-4" />
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
                className="flex h-8 w-8 items-center justify-center rounded-full hover:bg-white/15 flex-shrink-0"
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

            {transferState ? (<>
                <div className="flex items-center justify-between border-b border-kb-border bg-white px-4 py-3">
                  <button
                    type="button"
                    onClick={() => {
                      if (transferState.step === 'confirm') {
                        setTransferState((s) => s && { ...s, step: 'form' })
                      } else {
                        setTransferState(null)
                      }
                    }}
                    className="flex items-center gap-1 text-xs font-medium text-kb-text-muted hover:text-kb-text"
                  >
                    <Home className="h-4 w-4" />
                  </button>
                  <span className="text-sm font-bold text-kb-text">챗봇 이체</span>
                  <button type="button" onClick={() => setTransferState(null)} className="text-kb-text-muted hover:text-kb-text">
                    <X className="h-4 w-4" />
                  </button>
                </div>

                <div className="flex-1 overflow-y-auto px-4 py-4 space-y-4">
                  {/* 출금 계좌 */}
                  <div className="rounded-lg border border-kb-border bg-[#F7F5EF] px-4 py-3">
                    <p className="mb-1 text-[11px] text-kb-text-muted">출금 계좌</p>
                    <p className="text-sm font-bold text-kb-text">{transferState.fromAccountNumber}</p>
                    <p className="text-[11px] text-kb-text-muted">
                      잔액 {transferState.fromBalance.toLocaleString('ko-KR')}원
                    </p>
                  </div>

                  {transferState.step === 'form' && (
                    <>
                      <div>
                        <div className="mb-2 flex rounded border border-kb-border overflow-hidden">
                          <button
                            type="button"
                            onClick={() => setTransferState((s) => s && { ...s, toTab: 'my_accounts', toAccountNumber: '' })}
                            className={`flex-1 py-1.5 text-xs font-bold transition ${transferState.toTab === 'my_accounts' ? 'bg-[#2D6A4F] text-white' : 'bg-white text-kb-text-muted hover:bg-kb-beige'}`}
                          >
                            내 계좌
                          </button>
                          <button
                            type="button"
                            onClick={() => setTransferState((s) => s && { ...s, toTab: 'direct', toAccountNumber: '' })}
                            className={`flex-1 py-1.5 text-xs font-bold transition ${transferState.toTab === 'direct' ? 'bg-[#2D6A4F] text-white' : 'bg-white text-kb-text-muted hover:bg-kb-beige'}`}
                          >
                            직접 입력
                          </button>
                        </div>

                        {transferState.toTab === 'my_accounts' ? (
                          transferState.myAccounts.length === 0 ? (
                            <p className="rounded border border-kb-border bg-[#F7F5EF] px-3 py-3 text-center text-xs text-kb-text-muted">
                              다른 계좌가 없습니다.
                            </p>
                          ) : (
                            <div className="space-y-1.5">
                              {transferState.myAccounts.map((acc) => (
                                <button
                                  key={acc.account_id}
                                  type="button"
                                  onClick={() => setTransferState((s) => s && { ...s, toAccountNumber: acc.account_number })}
                                  className={`w-full rounded border px-3 py-2 text-left text-xs transition ${
                                    transferState.toAccountNumber === acc.account_number
                                      ? 'border-[#2D6A4F] bg-[#EAF4EF]'
                                      : 'border-kb-border bg-[#F7F5EF] hover:bg-kb-beige'
                                  }`}
                                >
                                  <span className="block font-bold text-kb-text">
                                    {acc.account_alias ?? acc.account_number}
                                  </span>
                                  <span className="text-[11px] text-kb-text-muted">
                                    {acc.account_number} · 잔액 {acc.balance.toLocaleString('ko-KR')}원
                                  </span>
                                </button>
                              ))}
                            </div>
                          )
                        ) : (
                          <input
                            type="text"
                            value={transferState.toAccountNumber}
                            onChange={(e) => setTransferState((s) => s && { ...s, toAccountNumber: e.target.value })}
                            placeholder="계좌번호 입력 (예: 12345678901234)"
                            className="w-full rounded border border-kb-border px-3 py-2 text-sm outline-none focus:border-[#2D6A4F]"
                          />
                        )}
                      </div>
                      <div>
                        <label className="mb-1 block text-xs font-bold text-kb-text">이체 금액</label>
                        <input
                          type="text"
                          inputMode="numeric"
                          value={transferState.amount ? Number(transferState.amount.replace(/,/g, '')).toLocaleString('ko-KR') : ''}
                          onChange={(e) => {
                            const raw = e.target.value.replace(/,/g, '')
                            if (/^\d*$/.test(raw)) setTransferState((s) => s && { ...s, amount: raw })
                          }}
                          placeholder="금액 입력"
                          className="w-full rounded border border-kb-border px-3 py-2 text-sm outline-none focus:border-[#2D6A4F]"
                        />
                        <div className="mt-2 flex flex-wrap gap-1.5">
                          {[10000, 50000, 100000, 500000].map((v) => (
                            <button
                              key={v}
                              type="button"
                              onClick={() => setTransferState((s) => s && { ...s, amount: String((parseInt(s.amount || '0', 10)) + v) })}
                              className="rounded border border-kb-border bg-[#F7F5EF] px-2 py-1 text-[11px] font-medium text-kb-text hover:bg-kb-beige"
                            >
                              +{(v / 10000)}만
                            </button>
                          ))}
                          <button
                            type="button"
                            onClick={() => setTransferState((s) => s && { ...s, amount: String(s.fromBalance) })}
                            className="rounded border border-kb-border bg-[#F7F5EF] px-2 py-1 text-[11px] font-medium text-kb-text hover:bg-kb-beige"
                          >
                            전액
                          </button>
                        </div>
                      </div>
                      <div>
                        <label className="mb-1 block text-xs font-bold text-kb-text">메모 (선택)</label>
                        <input
                          type="text"
                          value={transferState.memo}
                          onChange={(e) => setTransferState((s) => s && { ...s, memo: e.target.value })}
                          placeholder="이체"
                          className="w-full rounded border border-kb-border px-3 py-2 text-sm outline-none focus:border-[#2D6A4F]"
                        />
                      </div>
                      <button
                        type="button"
                        onClick={confirmTransfer}
                        disabled={!transferState.toAccountNumber || !transferState.amount}
                        className="w-full rounded bg-[#2D6A4F] py-3 text-sm font-bold text-white hover:bg-[#24563F] disabled:bg-gray-300"
                      >
                        다음
                      </button>
                    </>
                  )}

                  {transferState.step === 'confirm' && (
                    <>
                      <div className="rounded-lg border border-kb-border bg-white px-4 py-3 space-y-2 text-sm">
                        <div className="flex justify-between">
                          <span className="text-kb-text-muted">수취 계좌</span>
                          <span className="font-bold text-kb-text">{transferState.toAccountNumber}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-kb-text-muted">이체 금액</span>
                          <span className="font-bold text-[#1a5fa8]">
                            {parseInt(transferState.amount, 10).toLocaleString('ko-KR')}원
                          </span>
                        </div>
                        {transferState.memo && (
                          <div className="flex justify-between">
                            <span className="text-kb-text-muted">메모</span>
                            <span className="text-kb-text">{transferState.memo}</span>
                          </div>
                        )}
                      </div>
                      <div className="flex gap-2">
                        <button
                          type="button"
                          onClick={() => setTransferState((s) => s && { ...s, step: 'form' })}
                          className="flex-1 rounded border border-kb-border py-3 text-sm font-bold text-kb-text hover:bg-kb-beige"
                        >
                          수정
                        </button>
                        <button
                          type="button"
                          onClick={executeTransfer}
                          className="flex-1 rounded bg-[#2D6A4F] py-3 text-sm font-bold text-white hover:bg-[#24563F]"
                        >
                          이체 확인
                        </button>
                      </div>
                    </>
                  )}

                  {transferState.step === 'processing' && (
                    <div className="flex items-center justify-center py-8 text-sm text-kb-text-muted">
                      이체 처리 중...
                    </div>
                  )}

                  {(transferState.step === 'done' || transferState.step === 'error') && (
                    <>
                      <div className={`rounded-lg border px-4 py-4 text-center ${transferState.step === 'done' ? 'border-green-200 bg-green-50' : 'border-red-200 bg-red-50'}`}>
                        <p className={`text-lg font-bold mb-1 ${transferState.step === 'done' ? 'text-green-700' : 'text-red-700'}`}>
                          {transferState.step === 'done' ? '이체 완료' : '이체 실패'}
                        </p>
                        <p className="text-sm text-kb-text">{transferState.resultMessage}</p>
                        {transferState.step === 'done' && transferState.balanceAfter !== null && (
                          <p className="mt-2 text-xs text-kb-text-muted">
                            잔여 잔액: {transferState.balanceAfter.toLocaleString('ko-KR')}원
                          </p>
                        )}
                      </div>
                      <button
                        type="button"
                        onClick={() => setTransferState(null)}
                        className="w-full rounded bg-[#2D6A4F] py-3 text-sm font-bold text-white hover:bg-[#24563F]"
                      >
                        닫기
                      </button>
                    </>
                  )}
                </div>
              </>) : (<>

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
                              <div className="flex w-full items-center gap-2 px-3 py-2">
                                <button
                                  type="button"
                                  onClick={() => setExpandedRow(isOpen ? null : { key: rowKey, title: summary.title, row })}
                                  className="min-w-0 flex-1 text-left"
                                >
                                  <span className="block truncate text-xs font-bold text-kb-text">{summary.title}</span>
                                  <span className="block truncate text-[11px] text-kb-text-muted">{summary.meta}</span>
                                </button>
                                <div className="flex flex-none items-center gap-1.5">
                                  {message.featureCode === 'MY_PRODUCTS' && row.is_withdrawable === true && (
                                    <button
                                      type="button"
                                      onClick={(e) => {
                                        e.stopPropagation()
                                        startTransfer(
                                          Number(row.account_id),
                                          String(row.account_number ?? ''),
                                          Number(row.balance ?? 0),
                                        )
                                      }}
                                      className="rounded border border-[#1a5fa8] bg-[#EAF2FB] px-2 py-0.5 text-[10px] font-bold text-[#1a5fa8] hover:bg-[#D0E6F7]"
                                    >
                                      이체
                                    </button>
                                  )}
                                  {message.featureCode === 'MY_PRODUCTS' && row.product_type !== '입출금' && (
                                    <a
                                      href="/products/deposit/inquiry/terminate"
                                      onClick={(e) => e.stopPropagation()}
                                      className="rounded border border-[#E05555] bg-white px-2 py-0.5 text-[10px] font-bold text-[#E05555] hover:bg-red-50"
                                    >
                                      해지
                                    </a>
                                  )}
                                  <button
                                    type="button"
                                    onClick={() => setExpandedRow(isOpen ? null : { key: rowKey, title: summary.title, row })}
                                    className="text-[11px] font-bold text-[#2D6A4F]"
                                  >
                                    {TEXT.detail}
                                  </button>
                                </div>
                              </div>
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
                    {message.link && (
                      <div className="mt-3">
                        <a
                          href={message.link.href}
                          className="inline-flex items-center gap-1.5 rounded bg-[#2D6A4F] px-3 py-1.5 text-xs font-bold text-white hover:bg-[#24563F]"
                        >
                          <ArrowLeftRight className="h-3 w-3" />
                          {message.link.text}
                        </a>
                      </div>
                    )}
                    {message.loginForm && (
                      <InlineLoginForm onSuccess={() => {
                        setIsLoggedIn(true)
                        const cid = localStorage.getItem('customerId')
                        if (cid) setCustomerNo(cid)
                        handleQuickAction({ type: 'my_products', label: TEXT.myProducts, message: '내 상품 보여줘' })
                      }} />
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
                          : action.type === 'my_products'
                            ? 'border-[#2D6A4F] bg-[#EAF4EF] text-[#2D6A4F] hover:bg-[#D8EEE3]'
                          : action.type === 'transfer'
                            ? 'border-[#1a5fa8] bg-[#EAF2FB] text-[#1a5fa8] hover:bg-[#D0E6F7]'
                          : 'border-kb-border bg-[#F7F5EF] text-kb-text hover:bg-kb-beige'
                    }`}
                  >
                    {action.type === 'recommend' && <Sparkles className="h-3.5 w-3.5" />}
                    {action.type === 'consult' && <Phone className="h-3.5 w-3.5" />}
                    {action.type === 'my_products' && <PackageSearch className="h-3.5 w-3.5" />}
                    {action.type === 'transfer' && <ArrowLeftRight className="h-3.5 w-3.5" />}
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
            </>)}

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
        className="fixed bottom-6 right-28 z-[320] flex h-16 w-32 items-center justify-center gap-2 rounded-full bg-red-600 text-white shadow-xl transition hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500"
        aria-label={TEXT.openChat}
      >
        <MessageCircle className="h-7 w-7" />
        <span className="font-bold">CHATBOT</span>
      </button>
      {panel}
    </>
  )
}

function InlineLoginForm({ onSuccess }: { onSuccess: () => void }) {
  const [tab, setTab] = useState<'cert' | 'id'>('cert')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  // 금융인증서
  const [pin, setPin] = useState('')

  // 아이디 로그인
  const [loginId, setLoginId] = useState('')
  const [password, setPassword] = useState('')

  function switchTab(t: 'cert' | 'id') {
    setTab(t)
    setError('')
    setPin('')
    setLoginId('')
    setPassword('')
  }

  async function handleCertLogin() {
    if (pin.length !== 6) { setError('PIN 6자리를 입력해주세요.'); return }
    setError('')
    setLoading(true)
    try {
      const res = await fetch('/api/auth/cert-login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ cert_id: 'cert_1', pin }),
      })
      if (res.ok) {
        const data = await res.json()
        localStorage.setItem('accessToken', data.access_token)
        localStorage.setItem('access_token', data.access_token)
        localStorage.setItem('user', JSON.stringify(data.user))
        onSuccess()
      } else {
        setError('인증서 비밀번호가 맞지 않습니다.')
      }
    } catch {
      setError('네트워크 오류가 발생했습니다.')
    } finally {
      setLoading(false)
    }
  }

  async function handleIdLogin() {
    if (!loginId || !password) { setError('아이디와 비밀번호를 입력해주세요.'); return }
    setError('')
    setLoading(true)
    try {
      const { data } = await api.post('/api/v1/auth/login', { loginId, password })
      localStorage.setItem('accessToken', data.data.accessToken)
      localStorage.setItem('access_token', data.data.accessToken)
      localStorage.setItem('customerId', String(data.data.customerId))
      try {
        const me = await api.get('/api/v1/customers/me')
        localStorage.setItem('user', JSON.stringify({ name: me.data.data.name }))
      } catch {}
      onSuccess()
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } }
      setError(e.response?.data?.message ?? '로그인에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="mt-3">
      {/* 탭 */}
      <div className="flex border-b border-kb-border mb-3">
        <button
          type="button"
          onClick={() => switchTab('cert')}
          className={`flex-1 py-1.5 text-[10px] font-bold transition-colors ${tab === 'cert' ? 'border-b-2 border-[#2D6A4F] text-[#2D6A4F]' : 'text-kb-text-muted'}`}
        >
          금융인증서
        </button>
        <button
          type="button"
          onClick={() => switchTab('id')}
          className={`flex-1 py-1.5 text-[10px] font-bold transition-colors ${tab === 'id' ? 'border-b-2 border-[#2D6A4F] text-[#2D6A4F]' : 'text-kb-text-muted'}`}
        >
          아이디 로그인
        </button>
      </div>

      {/* 금융인증서 탭 */}
      {tab === 'cert' && (
        <div className="space-y-2">
          <p className="text-[10px] text-kb-text-muted">금융인증서 PIN 6자리를 입력해주세요.</p>
          <div className="flex gap-1 justify-center">
            {Array.from({ length: 6 }).map((_, i) => (
              <div
                key={i}
                className={`w-7 h-7 rounded flex items-center justify-center border ${i < pin.length ? 'bg-[#2D6A4F] border-[#2D6A4F]' : 'border-kb-border'}`}
              >
                {i < pin.length && <span className="text-white text-[8px]">●</span>}
              </div>
            ))}
          </div>
          <div className="grid grid-cols-3 gap-1">
            {['1','2','3','4','5','6','7','8','9','','0','⌫'].map((d) => (
              <button
                key={d}
                type="button"
                disabled={loading || d === ''}
                onClick={() => {
                  if (d === '⌫') { setPin((p) => p.slice(0, -1)); return }
                  if (d === '') return
                  if (pin.length < 6) setPin((p) => p + d)
                }}
                className={`py-2 text-xs font-medium rounded border border-kb-border transition-colors ${d === '' ? 'invisible' : 'hover:bg-kb-beige'}`}
              >
                {d}
              </button>
            ))}
          </div>
          <button
            type="button"
            onClick={handleCertLogin}
            disabled={loading || pin.length !== 6}
            className="w-full rounded bg-[#2D6A4F] py-1.5 text-xs font-bold text-white hover:bg-[#24563F] disabled:opacity-60"
          >
            {loading ? '로그인 중...' : '확인'}
          </button>
        </div>
      )}

      {/* 아이디 로그인 탭 */}
      {tab === 'id' && (
        <div className="space-y-2">
          <input
            type="text"
            placeholder="아이디"
            value={loginId}
            onChange={(e) => setLoginId(e.target.value)}
            className="w-full rounded border border-kb-border px-2 py-1.5 text-xs outline-none focus:border-[#2D6A4F]"
          />
          <input
            type="password"
            placeholder="비밀번호"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleIdLogin()}
            className="w-full rounded border border-kb-border px-2 py-1.5 text-xs outline-none focus:border-[#2D6A4F]"
          />
          <button
            type="button"
            onClick={handleIdLogin}
            disabled={loading}
            className="w-full rounded bg-[#2D6A4F] py-1.5 text-xs font-bold text-white hover:bg-[#24563F] disabled:opacity-60"
          >
            {loading ? '로그인 중...' : '로그인'}
          </button>
        </div>
      )}

      {error && <p className="mt-2 text-[10px] text-red-500">{error}</p>}
    </div>
  )
}
