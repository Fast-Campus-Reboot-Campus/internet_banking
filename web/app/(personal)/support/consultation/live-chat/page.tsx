'use client'

import Link from 'next/link'
import { useEffect, useRef, useState } from 'react'
import { Send } from 'lucide-react'
import {
  startChatbotConsultation,
  sendChatbotMessage,
  getAgentQueue,
  sendChatMessage,
  getChatMessages,
  endChat,
} from '@/lib/consultation-api'
import type { ChatMessage } from '@/lib/consultation-api'

const SUPPORT_TABS = [
  { label: '고객상담', href: '#', active: true },
  { label: '고객정보관리', href: '#' },
  { label: '사고신고', href: '#' },
  { label: '소비자보호', href: '#' },
  { label: '금융서비스', href: '#' },
  { label: '서식/약관/설명서', href: '#' },
  { label: '상품공시실', href: '#' },
]

const LEFT_MENU = [
  { label: '자주찾는 질문', href: '#', sub: [] },
  {
    label: '상담신청',
    href: '#',
    open: true,
    sub: [
      { label: '챗봇/채팅/이메일상담', href: '/support/consultation/live-chat', active: true },
      { label: '나의상담내역', href: '#' },
      { label: '지점 상담 예약서비스', href: '/support/consultation/branch' },
    ],
  },
  { label: '고객의 소리', href: '#', sub: [] },
]

type Step = 'form' | 'waiting' | 'chat' | 'ended'

type UiMessage = {
  id: string
  from: 'user' | 'agent' | 'bot' | 'system'
  text: string
  time: string
}

function timeStr(dt?: string | null) {
  const d = dt ? new Date(dt) : new Date()
  return d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
}

const DEFAULT_CUSTOMER_NO = 'CUST001'

export default function LiveChatPage() {
  const [step, setStep] = useState<Step>('form')
  const [topic, setTopic] = useState('선택')
  const [inquiry, setInquiry] = useState('')
  const [customerNo, setCustomerNo] = useState(DEFAULT_CUSTOMER_NO)
  const [messages, setMessages] = useState<UiMessage[]>([])
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const [score, setScore] = useState<number | null>(null)

  const chatbotIdRef = useRef<number | null>(null)
  const chatIdRef = useRef<number | null>(null)
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const scrollRef = useRef<HTMLDivElement>(null)
  const lastMessageIdRef = useRef<number>(0)

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
  }, [messages])

  useEffect(() => {
    return () => { if (pollRef.current) clearInterval(pollRef.current) }
  }, [])

  function push(msg: Omit<UiMessage, 'id'>) {
    setMessages(prev => [...prev, { ...msg, id: `${Date.now()}-${Math.random()}` }])
  }

  // 메시지 폴링 (상담사 채팅 연결 후)
  function startPolling(chatConsultationId: number) {
    if (pollRef.current) clearInterval(pollRef.current)
    pollRef.current = setInterval(async () => {
      try {
        const msgs = await getChatMessages(chatConsultationId)
        const newMsgs = msgs.filter(m => m.message_id > lastMessageIdRef.current)
        if (newMsgs.length > 0) {
          lastMessageIdRef.current = newMsgs[newMsgs.length - 1].message_id
          newMsgs.forEach(m => {
            if (m.sender_type === 'AGENT') {
              push({ from: 'agent', text: m.message, time: timeStr(m.sent_at) })
            }
          })
        }
      } catch {}
    }, 2000)
  }

  // 상담원 연결 대기 폴링
  async function waitForAgent(chatbotConsultationId: number) {
    const MAX_WAIT = 60
    let waited = 0
    return new Promise<number | null>((resolve) => {
      const timer = setInterval(async () => {
        waited += 3
        try {
          const queue = await getAgentQueue()
          const found = queue.find(q => q.chatbot_consultation_id === chatbotConsultationId)
          if (found) {
            clearInterval(timer)
            resolve(found.chat_consultation_id)
            return
          }
        } catch {}
        if (waited >= MAX_WAIT) {
          clearInterval(timer)
          resolve(null)
        }
      }, 3000)
    })
  }

  async function startChat() {
    if (topic === '선택') { alert('상담 유형을 선택해주세요.'); return }
    setStep('waiting')
    try {
      const started = await startChatbotConsultation(customerNo.trim() || DEFAULT_CUSTOMER_NO)
      chatbotIdRef.current = started.chatbot_consultation_id

      // 상담 유형 및 문의 내용을 첫 메시지로 전송 → 상담원 이관 트리거
      const triggerMessage = `상담원 연결 요청\n유형: ${topic}${inquiry ? `\n문의: ${inquiry}` : ''}`
      const res = await sendChatbotMessage(started.chatbot_consultation_id, { message: triggerMessage })

      if (res.agent_transfer_required) {
        // 상담원 연결 대기
        const chatId = await waitForAgent(started.chatbot_consultation_id)
        if (chatId) {
          chatIdRef.current = chatId
          setStep('chat')
          setMessages([{
            id: '0',
            from: 'system',
            text: '상담원이 연결되었습니다.',
            time: timeStr(),
          }])
          startPolling(chatId)
        } else {
          // 타임아웃 — 봇 응답으로라도 진행
          setStep('chat')
          setMessages([{
            id: '0',
            from: 'system',
            text: '현재 대기 인원이 많습니다. 봇이 대신 응답합니다.',
            time: timeStr(),
          }, {
            id: '1',
            from: 'bot',
            text: res.message,
            time: timeStr(),
          }])
        }
      } else {
        // 봇 모드
        setStep('chat')
        setMessages([{
          id: '0',
          from: 'bot',
          text: res.message || `안녕하세요! ${topic} 상담을 도와드리겠습니다.`,
          time: timeStr(),
        }])
      }
    } catch {
      alert('상담 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.')
      setStep('form')
    }
  }

  async function sendMessage() {
    const text = input.trim()
    if (!text || sending) return
    setInput('')
    push({ from: 'user', text, time: timeStr() })
    setSending(true)

    try {
      if (chatIdRef.current) {
        // 상담사 채팅
        await sendChatMessage(chatIdRef.current, text, 'USER')
      } else if (chatbotIdRef.current) {
        // 봇 채팅
        const res = await sendChatbotMessage(chatbotIdRef.current, { message: text })
        push({ from: 'bot', text: res.message, time: timeStr() })

        if (res.agent_transfer_required && !chatIdRef.current) {
          push({ from: 'system', text: '상담원 연결을 요청했습니다. 잠시 기다려 주세요.', time: timeStr() })
          const chatId = await waitForAgent(chatbotIdRef.current)
          if (chatId) {
            chatIdRef.current = chatId
            push({ from: 'system', text: '상담원이 연결되었습니다.', time: timeStr() })
            startPolling(chatId)
          }
        }
      }
    } catch {
      push({ from: 'system', text: '메시지 전송에 실패했습니다.', time: timeStr() })
    } finally {
      setSending(false)
    }
  }

  async function handleEnd() {
    if (pollRef.current) clearInterval(pollRef.current)
    if (chatIdRef.current) {
      try { await endChat(chatIdRef.current, score ?? undefined) } catch {}
    }
    setStep('ended')
  }

  function reset() {
    chatbotIdRef.current = null
    chatIdRef.current = null
    lastMessageIdRef.current = 0
    setStep('form')
    setTopic('선택')
    setInquiry('')
    setMessages([])
    setScore(null)
  }

  const isConnectedToAgent = !!chatIdRef.current

  return (
    <div className="min-h-screen bg-white">
      {/* 고객센터 탭 */}
      <div className="bg-[#5D3D2B]">
        <div className="max-w-kb-container mx-auto px-6">
          <div className="flex">
            {SUPPORT_TABS.map(tab => (
              <Link key={tab.label} href={tab.href}
                className={`px-6 py-3 text-[14px] font-medium transition-colors ${
                  tab.active ? 'bg-[#5BC9A8] text-kb-text font-bold' : 'text-white hover:bg-white/10'
                }`}>
                {tab.label}
              </Link>
            ))}
          </div>
        </div>
      </div>

      {/* 본문 */}
      <div className="max-w-kb-container mx-auto px-6 py-6">
        <div className="flex justify-end mb-3 text-[12px] text-kb-text-muted gap-1 items-center">
          <Link href="#" className="hover:underline">고객센터</Link><span>&gt;</span>
          <Link href="#" className="hover:underline">고객상담</Link><span>&gt;</span>
          <span className="text-kb-blue">채팅상담</span>
        </div>

        <div className="flex gap-6">
          {/* 사이드바 */}
          <aside className="w-[200px] flex-shrink-0">
            <div className="border border-kb-border">
              <div className="bg-[#5D3D2B] px-4 py-3">
                <span className="text-white font-bold text-[14px]">고객상담</span>
              </div>
              {LEFT_MENU.map(item => (
                <div key={item.label}>
                  <Link href={item.href}
                    className="flex items-center justify-between px-4 py-3 text-[13px] border-t border-kb-border hover:bg-kb-beige-light text-kb-text-body">
                    {item.label}
                    {item.sub.length > 0 && <span className="text-xs text-kb-text-muted">▼</span>}
                  </Link>
                  {item.open && item.sub.map(sub => (
                    <Link key={sub.label} href={sub.href}
                      className={`block pl-6 pr-4 py-2.5 text-[12px] border-t border-kb-border transition-colors ${
                        sub.active
                          ? 'bg-[#5BC9A8] text-kb-text font-bold'
                          : 'hover:bg-kb-beige-light text-kb-text-muted hover:text-kb-text'
                      }`}>
                      {sub.label}
                    </Link>
                  ))}
                </div>
              ))}
            </div>
          </aside>

          {/* 메인 */}
          <main className="flex-1 min-w-0">
            <h1 className="text-[20px] font-bold text-kb-text mb-4">채팅 상담</h1>

            {/* 상담 신청 폼 */}
            {step === 'form' && (
              <div className="border border-kb-border p-6 max-w-xl">
                <p className="text-[13px] text-kb-text-muted mb-5">
                  상담 유형을 선택하고 상담원 연결을 신청하세요.<br />
                  운영시간: 평일 09:00~18:00 (토·일·공휴일 제외)
                </p>
                <table className="w-full border-collapse text-[13px] border-t-2 border-kb-text mb-5">
                  <tbody>
                    <tr>
                      <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text w-[120px] whitespace-nowrap">
                        고객번호
                      </td>
                      <td className="border border-kb-border px-4 py-3">
                        <input value={customerNo} onChange={e => setCustomerNo(e.target.value)}
                          className="border border-kb-border px-3 py-1.5 text-[13px] outline-none w-48" />
                      </td>
                    </tr>
                    <tr>
                      <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text whitespace-nowrap">
                        상담 유형<span className="text-[#5BC9A8] font-bold ml-0.5">★</span>
                      </td>
                      <td className="border border-kb-border px-4 py-3">
                        <select value={topic} onChange={e => setTopic(e.target.value)}
                          className="border border-kb-border px-3 py-1.5 text-[13px] outline-none bg-white w-full">
                          {['선택', '예금/적금', '대출', '이체/송금', '카드', '분실/도난', '기타'].map(t => (
                            <option key={t}>{t}</option>
                          ))}
                        </select>
                      </td>
                    </tr>
                    <tr>
                      <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text align-top whitespace-nowrap">
                        문의 내용
                      </td>
                      <td className="border border-kb-border px-4 py-3">
                        <p className="text-[12px] text-kb-text-muted mb-1">{inquiry.length}/200자</p>
                        <textarea value={inquiry}
                          onChange={e => { if (e.target.value.length <= 200) setInquiry(e.target.value) }}
                          placeholder="문의 내용을 미리 입력하시면 빠른 상담이 가능합니다."
                          className="w-full h-20 border border-kb-border px-3 py-2 text-[13px] resize-none outline-none placeholder:text-kb-text-muted" />
                      </td>
                    </tr>
                  </tbody>
                </table>
                <button onClick={startChat}
                  className="px-10 py-2.5 text-[14px] font-bold hover:opacity-90 transition-opacity"
                  style={{ backgroundColor: '#5BC9A8', color: '#000' }}>
                  상담원 연결
                </button>
              </div>
            )}

            {/* 대기 */}
            {step === 'waiting' && (
              <div className="border border-kb-border p-12 max-w-xl flex flex-col items-center gap-4 text-center">
                <div className="w-12 h-12 border-4 border-[#5BC9A8] border-t-transparent rounded-full animate-spin" />
                <p className="text-[15px] font-bold text-kb-text">상담원 연결 중입니다...</p>
                <p className="text-[13px] text-kb-text-muted">잠시만 기다려 주세요.</p>
              </div>
            )}

            {/* 채팅 */}
            {step === 'chat' && (
              <div className="border border-kb-border max-w-xl flex flex-col" style={{ height: 500 }}>
                <div className="px-4 py-3 border-b border-kb-border flex items-center justify-between"
                  style={{ backgroundColor: isConnectedToAgent ? '#5BC9A8' : '#e9e9e9' }}>
                  <span className="text-[14px] font-bold text-kb-text">
                    {isConnectedToAgent ? '상담원 연결됨' : '상담 중 (봇)'}
                  </span>
                  <button onClick={handleEnd}
                    className="text-[12px] text-kb-text border border-kb-text px-3 py-1 hover:bg-white/30 transition-colors">
                    상담 종료
                  </button>
                </div>

                <div ref={scrollRef} className="flex-1 overflow-y-auto px-4 py-4 space-y-3 bg-[#FAFAF7]">
                  {messages.map(msg => {
                    if (msg.from === 'system') {
                      return (
                        <div key={msg.id} className="text-center text-[12px] text-kb-text-muted py-1">
                          {msg.text}
                        </div>
                      )
                    }
                    return (
                      <div key={msg.id} className={`flex ${msg.from === 'user' ? 'justify-end' : 'justify-start'} gap-2`}>
                        {msg.from !== 'user' && (
                          <div className={`w-8 h-8 rounded-full flex items-center justify-center text-[11px] font-bold text-white flex-shrink-0 mt-1 ${
                            msg.from === 'agent' ? 'bg-[#5BC9A8]' : 'bg-kb-text-muted'
                          }`}>
                            {msg.from === 'agent' ? '상담' : '봇'}
                          </div>
                        )}
                        <div className={`max-w-[75%] flex flex-col gap-1 ${msg.from === 'user' ? 'items-end' : 'items-start'}`}>
                          <div className={`px-3 py-2 text-[13px] leading-relaxed whitespace-pre-wrap rounded-lg ${
                            msg.from === 'user'
                              ? 'bg-[#5D3D2B] text-white'
                              : 'bg-white border border-kb-border text-kb-text'
                          }`}>
                            {msg.text}
                          </div>
                          <span className="text-[11px] text-kb-text-muted">{msg.time}</span>
                        </div>
                      </div>
                    )
                  })}
                  {sending && (
                    <div className="flex justify-start gap-2">
                      <div className="w-8 h-8 rounded-full bg-kb-text-muted flex items-center justify-center text-[11px] font-bold text-white flex-shrink-0">봇</div>
                      <div className="bg-white border border-kb-border px-3 py-2 rounded-lg text-[13px] text-kb-text-muted">입력 중...</div>
                    </div>
                  )}
                </div>

                <div className="border-t border-kb-border px-3 py-2 flex gap-2 bg-white">
                  <input value={input}
                    onChange={e => setInput(e.target.value)}
                    onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage() } }}
                    placeholder="메시지를 입력하세요"
                    className="flex-1 text-[13px] outline-none px-2 py-1" />
                  <button onClick={sendMessage} disabled={!input.trim() || sending}
                    className="flex h-9 w-9 items-center justify-center rounded bg-[#5D3D2B] text-white disabled:bg-gray-300 transition-colors">
                    <Send className="h-4 w-4" />
                  </button>
                </div>
              </div>
            )}

            {/* 종료 */}
            {step === 'ended' && (
              <div className="border border-kb-border p-12 max-w-xl flex flex-col items-center gap-4 text-center">
                <div className="w-12 h-12 rounded-full bg-[#5BC9A8] flex items-center justify-center text-white text-2xl font-bold">✓</div>
                <p className="text-[15px] font-bold text-kb-text">상담이 종료되었습니다.</p>
                {isConnectedToAgent && (
                  <div className="flex gap-2 mt-1">
                    <p className="text-[13px] text-kb-text-muted self-center">만족도:</p>
                    {[1, 2, 3, 4, 5].map(s => (
                      <button key={s} onClick={() => setScore(s)}
                        className={`w-8 h-8 rounded-full border text-[13px] font-bold transition-colors ${
                          score === s ? 'bg-[#5BC9A8] border-[#5BC9A8] text-white' : 'border-kb-border text-kb-text-muted hover:border-[#5BC9A8]'
                        }`}>{s}</button>
                    ))}
                  </div>
                )}
                <p className="text-[13px] text-kb-text-muted">이용해 주셔서 감사합니다.</p>
                <div className="flex gap-3 mt-2">
                  <button onClick={reset}
                    className="border border-kb-border px-5 py-2 text-[13px] text-kb-text-body hover:bg-kb-beige-light transition-colors">
                    재상담 신청
                  </button>
                  <Link href="/"
                    className="px-5 py-2 text-[13px] font-bold hover:opacity-90 transition-opacity"
                    style={{ backgroundColor: '#5BC9A8', color: '#000' }}>
                    메인으로
                  </Link>
                </div>
              </div>
            )}
          </main>
        </div>
      </div>
    </div>
  )
}
