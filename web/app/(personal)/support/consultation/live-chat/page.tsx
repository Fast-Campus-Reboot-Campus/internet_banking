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
    <div className="min-h-screen" style={{ backgroundColor: '#F8FDFB' }}>
      <div className="max-w-kb-container mx-auto px-6 py-8">
        {/* 브레드크럼 */}
        <div className="flex items-center gap-1 text-[12px] text-kb-text-muted mb-6">
          <Link href="/" className="hover:underline">홈</Link>
          <span>›</span>
          <span>고객센터</span>
          <span>›</span>
          <span>고객상담</span>
          <span>›</span>
          <span className="font-semibold text-kb-text">채팅 상담</span>
        </div>

        <main className="w-full">
          <h1 className="text-[22px] font-bold text-kb-text mb-2">채팅 상담</h1>
          <p className="text-[13px] text-kb-text-muted mb-7">실시간으로 상담원과 채팅 상담을 진행하세요.</p>

          {/* 상담 신청 폼 */}
          {step === 'form' && (
            <div className="bg-white rounded-2xl shadow-sm max-w-xl overflow-hidden" style={{ border: '1px solid #5BC9A820' }}>
              <div className="px-6 py-4 border-b" style={{ borderColor: '#E2F5EF', backgroundColor: '#F8FDFB' }}>
                <p className="text-[14px] font-bold text-kb-text">상담 신청</p>
                <p className="text-[12px] text-kb-text-muted mt-0.5">
                  운영시간: 평일 09:00~18:00 (토·일·공휴일 제외)
                </p>
              </div>

              <div className="p-6">
                <table className="w-full border-collapse text-[13px] border-t-2 border-[#0D5C47] mb-6">
                  <tbody>
                    <tr>
                      <td className="bg-[#F0FAF7] border border-[#E2F5EF] px-4 py-3.5 font-semibold text-kb-text w-[120px] whitespace-nowrap">
                        고객번호
                      </td>
                      <td className="border border-[#E2F5EF] px-4 py-3.5">
                        <input value={customerNo} onChange={e => setCustomerNo(e.target.value)}
                          className="border border-[#E2F5EF] rounded-lg px-3 py-1.5 text-[13px] outline-none w-48 focus:border-[#5BC9A8] transition-colors" />
                      </td>
                    </tr>
                    <tr>
                      <td className="bg-[#F0FAF7] border border-[#E2F5EF] px-4 py-3.5 font-semibold text-kb-text whitespace-nowrap">
                        상담 유형<span className="text-[#5BC9A8] font-bold ml-0.5">★</span>
                      </td>
                      <td className="border border-[#E2F5EF] px-4 py-3.5">
                        <select value={topic} onChange={e => setTopic(e.target.value)}
                          className="border border-[#E2F5EF] rounded-lg px-3 py-1.5 text-[13px] outline-none bg-white w-full focus:border-[#5BC9A8] transition-colors">
                          {['선택', '예금/적금', '대출', '이체/송금', '카드', '분실/도난', '기타'].map(t => (
                            <option key={t}>{t}</option>
                          ))}
                        </select>
                      </td>
                    </tr>
                    <tr>
                      <td className="bg-[#F0FAF7] border border-[#E2F5EF] px-4 py-3.5 font-semibold text-kb-text align-top whitespace-nowrap">
                        문의 내용
                      </td>
                      <td className="border border-[#E2F5EF] px-4 py-3.5">
                        <p className="text-[12px] text-kb-text-muted mb-1.5">{inquiry.length}/200자</p>
                        <textarea value={inquiry}
                          onChange={e => { if (e.target.value.length <= 200) setInquiry(e.target.value) }}
                          placeholder="문의 내용을 미리 입력하시면 빠른 상담이 가능합니다."
                          className="w-full h-20 border border-[#E2F5EF] rounded-lg px-3 py-2.5 text-[13px] resize-none outline-none placeholder:text-kb-text-muted focus:border-[#5BC9A8] transition-colors" />
                      </td>
                    </tr>
                  </tbody>
                </table>

                <button onClick={startChat}
                  className="px-8 py-2.5 text-[14px] font-bold text-white rounded-lg hover:opacity-85 transition-opacity"
                  style={{ backgroundColor: '#0D5C47' }}>
                  상담원 연결
                </button>
              </div>
            </div>
          )}

          {/* 대기 */}
          {step === 'waiting' && (
            <div className="bg-white rounded-2xl shadow-sm max-w-xl flex flex-col items-center gap-4 py-14 text-center" style={{ border: '1px solid #5BC9A820' }}>
              <div className="w-12 h-12 border-4 border-[#5BC9A8] border-t-transparent rounded-full animate-spin" />
              <p className="text-[15px] font-bold text-kb-text">상담원 연결 중입니다...</p>
              <p className="text-[13px] text-kb-text-muted">잠시만 기다려 주세요.</p>
            </div>
          )}

          {/* 채팅 */}
          {step === 'chat' && (
            <div className="bg-white rounded-2xl shadow-sm max-w-xl flex flex-col overflow-hidden" style={{ height: 520, border: '1px solid #5BC9A820' }}>
              {/* 채팅 헤더 */}
              <div className="px-5 py-3.5 flex items-center justify-between rounded-t-2xl"
                style={{ backgroundColor: isConnectedToAgent ? '#0D5C47' : '#94A3B8' }}>
                <div className="flex items-center gap-2">
                  <div className="w-2.5 h-2.5 rounded-full bg-[#5BC9A8] animate-pulse" />
                  <span className="text-[14px] font-bold text-white">
                    {isConnectedToAgent ? '상담원 연결됨' : '상담 중 (봇)'}
                  </span>
                </div>
                <button onClick={handleEnd}
                  className="text-[12px] font-semibold text-white border border-white/40 rounded-lg px-3 py-1 hover:bg-white/20 transition-colors">
                  상담 종료
                </button>
              </div>

              {/* 메시지 영역 */}
              <div ref={scrollRef} className="flex-1 overflow-y-auto px-4 py-4 space-y-3 bg-[#F8FDFB]">
                {messages.map(msg => {
                  if (msg.from === 'system') {
                    return (
                      <div key={msg.id} className="text-center">
                        <span className="inline-block bg-white text-[12px] text-kb-text-muted px-3 py-1 rounded-full shadow-sm" style={{ border: '1px solid #E2F5EF' }}>
                          {msg.text}
                        </span>
                      </div>
                    )
                  }
                  return (
                    <div key={msg.id} className={`flex ${msg.from === 'user' ? 'justify-end' : 'justify-start'} gap-2`}>
                      {msg.from !== 'user' && (
                        <div className={`w-8 h-8 rounded-full flex items-center justify-center text-[11px] font-bold text-white flex-shrink-0 mt-1 ${
                          msg.from === 'agent' ? '' : 'bg-[#94A3B8]'
                        }`} style={msg.from === 'agent' ? { backgroundColor: '#0D5C47' } : {}}>
                          {msg.from === 'agent' ? '상담' : '봇'}
                        </div>
                      )}
                      <div className={`max-w-[75%] flex flex-col gap-1 ${msg.from === 'user' ? 'items-end' : 'items-start'}`}>
                        <div className={`px-3.5 py-2.5 text-[13px] leading-relaxed whitespace-pre-wrap rounded-2xl ${
                          msg.from === 'user'
                            ? 'text-white rounded-br-sm'
                            : 'bg-white text-kb-text rounded-bl-sm shadow-sm'
                        }`}
                          style={msg.from === 'user'
                            ? { backgroundColor: '#0D5C47' }
                            : { border: '1px solid #E2F5EF' }
                          }>
                          {msg.text}
                        </div>
                        <span className="text-[11px] text-kb-text-muted">{msg.time}</span>
                      </div>
                    </div>
                  )
                })}
                {sending && (
                  <div className="flex justify-start gap-2">
                    <div className="w-8 h-8 rounded-full bg-[#94A3B8] flex items-center justify-center text-[11px] font-bold text-white flex-shrink-0">봇</div>
                    <div className="bg-white px-3.5 py-2.5 rounded-2xl rounded-bl-sm text-[13px] text-kb-text-muted shadow-sm" style={{ border: '1px solid #E2F5EF' }}>
                      입력 중...
                    </div>
                  </div>
                )}
              </div>

              {/* 입력 영역 */}
              <div className="border-t px-3 py-2.5 flex gap-2 bg-white rounded-b-2xl" style={{ borderColor: '#E2F5EF' }}>
                <input value={input}
                  onChange={e => setInput(e.target.value)}
                  onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage() } }}
                  placeholder="메시지를 입력하세요"
                  className="flex-1 text-[13px] outline-none px-3 py-1.5 rounded-lg border border-[#E2F5EF] focus:border-[#5BC9A8] transition-colors" />
                <button onClick={sendMessage} disabled={!input.trim() || sending}
                  className="flex h-9 w-9 items-center justify-center rounded-lg text-white transition-colors disabled:opacity-40"
                  style={{ backgroundColor: '#0D5C47' }}>
                  <Send className="h-4 w-4" />
                </button>
              </div>
            </div>
          )}

          {/* 종료 */}
          {step === 'ended' && (
            <div className="bg-white rounded-2xl shadow-sm max-w-xl flex flex-col items-center gap-4 py-14 text-center" style={{ border: '1px solid #5BC9A820' }}>
              <div className="w-14 h-14 rounded-full flex items-center justify-center text-white text-2xl font-bold shadow-sm"
                style={{ backgroundColor: '#0D5C47' }}>
                ✓
              </div>
              <p className="text-[16px] font-bold text-kb-text">상담이 종료되었습니다.</p>
              {isConnectedToAgent && (
                <div className="flex items-center gap-2 mt-1">
                  <p className="text-[13px] text-kb-text-muted">만족도:</p>
                  {[1, 2, 3, 4, 5].map(s => (
                    <button key={s} onClick={() => setScore(s)}
                      className={`w-9 h-9 rounded-full border-2 text-[13px] font-bold transition-colors ${
                        score === s
                          ? 'text-white border-transparent'
                          : 'text-kb-text-muted hover:border-[#5BC9A8] border-[#E2F5EF]'
                      }`}
                      style={score === s ? { backgroundColor: '#0D5C47', borderColor: '#0D5C47' } : {}}>
                      {s}
                    </button>
                  ))}
                </div>
              )}
              <p className="text-[13px] text-kb-text-muted">이용해 주셔서 감사합니다.</p>
              <div className="flex gap-3 mt-2">
                <button onClick={reset}
                  className="px-6 py-2.5 text-[14px] font-semibold rounded-lg border-2 hover:bg-[#F0FAF7] transition-colors"
                  style={{ borderColor: '#0D5C47', color: '#0D5C47' }}>
                  재상담 신청
                </button>
                <Link href="/"
                  className="px-6 py-2.5 text-[14px] font-bold text-white rounded-lg hover:opacity-85 transition-opacity"
                  style={{ backgroundColor: '#0D5C47' }}>
                  메인으로
                </Link>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  )
}
