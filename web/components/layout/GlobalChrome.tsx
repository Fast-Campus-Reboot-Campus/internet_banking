'use client'

import { usePathname } from 'next/navigation'
import FloatingSidebar from '@/components/layout/FloatingSidebar'
import ChatbotWidget from '@/components/chatbot/ChatbotWidget'

/**
 * 전역 플로팅 UI(개인홈 사이드바 + 챗봇) 마운트 게이트.
 * 다온은행(/other-bank)에서는 자체 사이드바(DaonMyMenu)만 쓰므로 전역 UI를 숨긴다.
 * admin 콘솔(/admin)은 자체 AdminSidebar를 쓰므로 고객용 전역 UI를 숨긴다.
 * ★ FloatingSidebar/ChatbotWidget 컴포넌트 자체는 수정하지 않음(팀 자산).
 */
export default function GlobalChrome() {
  const pathname = usePathname()
  if (pathname?.startsWith('/other-bank') || pathname?.startsWith('/admin')) return null

  return (
    <>
      <FloatingSidebar />
      <ChatbotWidget />
    </>
  )
}
