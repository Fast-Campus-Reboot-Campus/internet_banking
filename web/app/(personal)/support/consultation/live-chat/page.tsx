'use client'

import { useEffect, useRef, useState } from 'react'
import Link from 'next/link'

type Message = {
  id: number
  from: 'agent' | 'user' | 'system'
  text: string
  time: string
}

function getTime() {
  return new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
}

export default function LiveChatPage() {
  const [phase, setPhase] = useState<'waiting' | 'chatting' | 'ended'>('waiting')
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [msgId, setMsgId] = useState(1)
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const timer = setTimeout(() => {
      setMessages([
        {
          id: 0,
          from: 'system',
          text: '상담원과 연결되었습니다.',
          time: getTime(),
        },
        {
          id: 1,
          from: 'agent',
          text: '안녕하세요. AX뱅크 상담원 김민지입니다. 무엇을 도와드릴까요?',
          time: getTime(),
        },
      ])
      setMsgId(2)
      setPhase('chatting')
    }, 2500)

    return () => clearTimeout(timer)
  }, [])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  function sendMessage() {
    const text = input.trim()
    if (!text) return

    const userMsg: Message = { id: msgId, from: 'user', text, time: getTime() }
    setMessages(prev => [...prev, userMsg])
    setInput('')
    setMsgId(prev => prev + 1)

    setTimeout(() => {
      const replies: Record<string, string> = {
        default: '말씀하신 내용을 확인하겠습니다. 잠시만 기다려 주세요.',
        금리: '금리는 상품별 금리 안내 페이지에서 확인하실 수 있습니다. 추가로 궁금한 사항이 있으시면 말씀해 주세요.',
        이체: '이체 관련 문의이신가요? 이체 한도, 수수료, 오류 중 어떤 내용인지 알려주시면 안내해 드리겠습니다.',
        가입: '가입하시려는 상품명을 알려주시면 절차를 안내해 드리겠습니다.',
        예금: '예금 상품 문의를 도와드리겠습니다. 목돈 예치 기간과 금액을 알려주시면 적합한 상품을 추천해 드릴게요.',
        적금: '적금 상품 문의를 도와드리겠습니다. 매월 납입 가능 금액과 목표 기간을 알려주세요.',
        청약: '청약 상품 문의를 도와드리겠습니다. 가입 조건과 납입 방식 안내가 필요하신가요?',
      }
      const key = Object.keys(replies).find(k => k !== 'default' && text.includes(k)) ?? 'default'
      const agentMsg: Message = {
        id: msgId + 1,
        from: 'agent',
        text: replies[key],
        time: getTime(),
      }

      setMessages(prev => [...prev, agentMsg])
      setMsgId(prev => prev + 2)
    }, 1200)
  }

  function endChat() {
    setMessages(prev => [
      ...prev,
      {
        id: msgId,
        from: 'system',
        text: '채팅 상담이 종료되었습니다. 이용해 주셔서 감사합니다.',
        time: getTime(),
      },
    ])
    setPhase('ended')
  }

  return (
    <div className="mx-auto max-w-[700px] px-4 py-8">
      <div
        className="mb-4 flex items-center justify-between px-4 py-3 text-white"
        style={{ backgroundColor: '#5BC9A8' }}
      >
        <div className="flex items-center gap-2">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            className="h-5 w-5"
            stroke="white"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
          <span className="text-[15px] font-bold">AX뱅크 채팅상담</span>
        </div>
        <Link href="/products/deposit" className="text-[12px] text-white/80 underline hover:text-white">
          나가기
        </Link>
      </div>

      {phase === 'waiting' && (
        <div className="flex flex-col items-center justify-center gap-4 border border-kb-border bg-[#FAFAFA] py-16">
          <div className="h-12 w-12 animate-spin rounded-full border-4 border-[#5BC9A8] border-t-transparent" />
          <p className="text-[14px] font-semibold text-kb-text">상담원 연결 중...</p>
          <p className="text-[12px] text-kb-text-muted">잠시만 기다려 주세요. 상담원을 연결하고 있습니다.</p>
          <div className="mt-2 flex items-center gap-2 border border-kb-border bg-white px-4 py-2 text-[12px] text-kb-text-body">
            <svg
              viewBox="0 0 20 20"
              fill="none"
              className="h-4 w-4 text-[#5BC9A8]"
              stroke="currentColor"
              strokeWidth="2"
            >
              <circle cx="10" cy="10" r="8" />
              <line x1="10" y1="6" x2="10" y2="10" />
              <line x1="10" y1="13" x2="10" y2="14" />
            </svg>
            현재 대기 인원 1명 · 예상 연결 시간 약 1분
          </div>
        </div>
      )}

      {(phase === 'chatting' || phase === 'ended') && (
        <>
          <div className="flex flex-col border border-kb-border bg-white" style={{ height: 420 }}>
            <div className="flex items-center gap-3 border-b border-kb-border bg-[#F5F5F5] px-4 py-2">
              <div
                className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full text-[13px] font-bold text-white"
                style={{ backgroundColor: '#5BC9A8' }}
              >
                김
              </div>
              <div>
                <p className="text-[13px] font-semibold text-kb-text">상담원 김민지</p>
                <p className="text-[11px] text-kb-text-muted">AX뱅크 고객상담센터</p>
              </div>
              <div className="ml-auto flex items-center gap-1">
                <span className="h-2 w-2 rounded-full bg-green-500" />
                <span className="text-[11px] text-kb-text-muted">연결됨</span>
              </div>
            </div>

            <div className="flex-1 space-y-3 overflow-y-auto px-4 py-4">
              {messages.map(msg => (
                <div key={msg.id}>
                  {msg.from === 'system' && (
                    <div className="flex justify-center">
                      <span className="rounded-full bg-[#F0F0F0] px-3 py-1 text-[11px] text-kb-text-muted">
                        {msg.text}
                      </span>
                    </div>
                  )}
                  {msg.from === 'agent' && (
                    <div className="flex items-end gap-2">
                      <div
                        className="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full text-[11px] font-bold text-white"
                        style={{ backgroundColor: '#5BC9A8' }}
                      >
                        김
                      </div>
                      <div>
                        <div className="max-w-[300px] border border-[#C5EAE0] bg-[#F0FAF7] px-3 py-2 text-[13px] leading-relaxed text-kb-text">
                          {msg.text}
                        </div>
                        <p className="mt-0.5 text-[10px] text-kb-text-muted">{msg.time}</p>
                      </div>
                    </div>
                  )}
                  {msg.from === 'user' && (
                    <div className="flex items-end justify-end gap-2">
                      <div>
                        <div className="max-w-[300px] bg-[#5BC9A8] px-3 py-2 text-right text-[13px] leading-relaxed text-white">
                          {msg.text}
                        </div>
                        <p className="mt-0.5 text-right text-[10px] text-kb-text-muted">{msg.time}</p>
                      </div>
                    </div>
                  )}
                </div>
              ))}
              <div ref={bottomRef} />
            </div>
          </div>

          {phase === 'chatting' && (
            <div className="flex items-center gap-2 border border-t-0 border-kb-border bg-white px-3 py-2">
              <input
                type="text"
                value={input}
                onChange={e => setInput(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && sendMessage()}
                placeholder="메시지를 입력하세요."
                className="flex-1 border border-kb-border px-2 py-1.5 text-[13px] outline-none"
              />
              <button
                onClick={sendMessage}
                className="px-4 py-1.5 text-[13px] font-bold text-white transition-colors"
                style={{ backgroundColor: '#5BC9A8' }}
              >
                전송
              </button>
              <button
                onClick={endChat}
                className="border border-kb-border px-4 py-1.5 text-[13px] text-kb-text-muted transition-colors hover:bg-kb-beige-light"
              >
                상담종료
              </button>
            </div>
          )}

          {phase === 'ended' && (
            <div className="flex justify-center gap-3 border border-t-0 border-kb-border bg-[#FAFAFA] px-4 py-3">
              <Link
                href="/products/deposit"
                className="bg-kb-yellow px-8 py-2 text-[13px] font-bold text-kb-text transition-colors hover:bg-kb-yellow-dark"
              >
                홈으로
              </Link>
              <button
                onClick={() => window.location.reload()}
                className="border border-kb-border px-8 py-2 text-[13px] text-kb-text-body transition-colors hover:bg-kb-beige-light"
              >
                재상담
              </button>
            </div>
          )}
        </>
      )}

      <div className="mt-3 space-y-0.5 text-[11px] text-kb-text-muted">
        <p>채팅상담 운영시간: 평일 09:00~18:00 (토·일·공휴일 제외)</p>
        <p>상담 내용은 고객 보호를 위해 기록·저장될 수 있습니다.</p>
      </div>
    </div>
  )
}
