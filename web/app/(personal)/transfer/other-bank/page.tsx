'use client'

import Link from 'next/link'
import TransferSidebar from '@/components/inquiry/TransferSidebar'

const FEATURE_CARDS = [
  {
    icon: '📋',
    title: '계좌조회',
    desc: '다른 금융기관 계좌를\n한눈에 조회',
  },
  {
    icon: '↔️',
    title: '이체',
    desc: '다른 금융기관 계좌에서\n바로 이체',
  },
  {
    icon: '💰',
    title: '잔액 모으기',
    desc: '여러 계좌 잔액을\n한 곳에 모으기',
  },
  {
    icon: '📦',
    title: '상품 가입',
    desc: '다른 금융기관 계좌로\n상품 가입',
  },
]

export default function OtherBankTransferPage() {
  return (
    <div className="max-w-kb-container mx-auto px-6">
      <div className="flex">
        <TransferSidebar />

        {/* ===== 본문 ===== */}
        <main className="flex-1 pl-8 pt-4 pb-12">
          {/* 브레드크럼 */}
          <div className="flex justify-end mb-2 text-[12px] text-kb-text-muted gap-1">
            <span>개인뱅킹</span><span>&gt;</span><span>이체</span><span>&gt;</span>
            <span>계좌이체</span><span>&gt;</span>
            <span className="font-semibold text-kb-text">다른금융 계좌이체</span>
            <span className="ml-2 text-kb-blue cursor-pointer">? 도움말</span>
          </div>

          <h1 className="text-[20px] font-bold text-kb-text mb-6">다른금융 계좌이체</h1>

          {/* 프로모션 배너 */}
          <div className="rounded-2xl bg-[#EAF3FB] px-8 py-8 mb-8 flex flex-col items-center text-center">
            <p className="text-[18px] font-bold text-kb-text mb-1">
              다른금융 계좌 등록하면
            </p>
            <p className="text-[18px] font-bold text-kb-text mb-6">
              모든 금융생활을 AXful 뱅킹 한 곳에서!
            </p>

            {/* 4개 피처 카드 */}
            <div className="flex gap-4 mb-8">
              {FEATURE_CARDS.map((card) => (
                <div
                  key={card.title}
                  className="bg-white rounded-xl px-6 py-5 flex flex-col items-center gap-2 w-[140px] shadow-sm"
                >
                  <span className="text-3xl">{card.icon}</span>
                  <p className="text-[14px] font-bold text-kb-text">{card.title}</p>
                  <p className="text-[12px] text-kb-text-muted text-center whitespace-pre-line leading-snug">
                    {card.desc}
                  </p>
                </div>
              ))}
            </div>

            <Link
              href="/transfer/other-bank/register"
              className="bg-kb-yellow px-16 py-3 text-[15px] font-bold text-kb-text rounded-lg hover:brightness-95 transition-all inline-block"
            >
              다른금융 계좌등록
            </Link>
          </div>

          {/* 안내 */}
          <div className="text-[12px] text-kb-text-muted space-y-1">
            <p>* 다른금융 계좌등록 서비스는 오픈뱅킹 서비스입니다.</p>
            <p>* 등록된 다른금융 계좌는 AXful 뱅킹 앱 및 인터넷뱅킹에서 조회·이체할 수 있습니다.</p>
            <p>* 오픈뱅킹 참가 금융기관의 계좌만 등록 가능합니다.</p>
          </div>
        </main>
      </div>
    </div>
  )
}
