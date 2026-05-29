'use client'

import Link from 'next/link'
import { useState, useEffect } from 'react'
import { Account, formatNumber } from '@/lib/mock-data'
import InquirySidebar from '@/components/inquiry/InquirySidebar'
import { fetchDepositAccountViewModels, getCurrentDepositCustomerId } from '@/lib/deposit-api'

const ACCOUNT_TABS = ['예금', '펀드', '신탁/ISA', '대출', '외화/골드', '보험/공제', '퇴직연금', '전체계좌']

const MANAGEMENT_ITEMS = [
  '계좌별명관리', '계좌개설확인서(통장사본)', '바른조회등록',
  '전자금융거래 제한', '통지(SMS) 서비스', '출금계좌등록/삭제',
  '계좌숨기기', '계약서류 관리', '한도제한해제 신청',
]

function EmptyState({ message, subMessage, actionHref, actionLabel }: {
  message: string
  subMessage?: string
  actionHref?: string
  actionLabel?: string
}) {
  return (
    <div className="border border-kb-border rounded-lg px-6 py-5 flex items-center gap-5 bg-white">
      <div className="flex-shrink-0">
        <svg viewBox="0 0 56 56" fill="none" className="w-12 h-12">
          <rect x="8" y="6" width="32" height="40" rx="2" fill="#E8E8E8" stroke="#CCCCCC" strokeWidth="1.5"/>
          <line x1="14" y1="16" x2="34" y2="16" stroke="#BBBBBB" strokeWidth="2"/>
          <line x1="14" y1="22" x2="34" y2="22" stroke="#BBBBBB" strokeWidth="2"/>
          <line x1="14" y1="28" x2="26" y2="28" stroke="#BBBBBB" strokeWidth="2"/>
          <circle cx="38" cy="40" r="9" fill="white" stroke="#AAAAAA" strokeWidth="2"/>
          <line x1="34" y1="36" x2="42" y2="44" stroke="#AAAAAA" strokeWidth="2"/>
          <line x1="42" y1="36" x2="34" y2="44" stroke="#AAAAAA" strokeWidth="2"/>
        </svg>
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-[13px] text-kb-text-muted font-semibold">{message}</p>
        {subMessage && (
          <p className="text-[12px] text-kb-text-muted mt-0.5 leading-relaxed">{subMessage}</p>
        )}
      </div>
      {actionHref && actionLabel && (
        <Link href={actionHref}
          className="flex-shrink-0 border border-kb-border px-5 py-1.5 text-[13px] text-kb-text-body hover:bg-kb-beige-light whitespace-nowrap">
          {actionLabel}
        </Link>
      )}
    </div>
  )
}

/* ── 섹션 헤더 공통 컴포넌트 ── */
function SectionHeader({ dotColor, label, count, balance, open, onToggle, showOrder = true }: {
  dotColor: string
  label: string
  count: number
  balance: string
  open: boolean
  onToggle: () => void
  showOrder?: boolean
}) {
  return (
    <div className="flex items-center justify-between mb-3 pb-2 border-b border-kb-border">
      <div className="flex items-center gap-2">
        <span className={`w-2.5 h-2.5 rounded-full ${dotColor} inline-block flex-shrink-0`} />
        <span className="text-[13px] font-bold text-kb-text">{label} ({count}계좌)</span>
        <span className="text-[13px] font-semibold" style={{ color: '#C23B00' }}>잔액 {balance}원</span>
      </div>
      <button onClick={onToggle} className="flex items-center gap-1 text-[12px] text-kb-text-muted hover:text-kb-text">
        {showOrder && (
          <svg viewBox="0 0 16 16" fill="none" className="w-3.5 h-3.5" stroke="currentColor" strokeWidth="1.5">
            <line x1="1" y1="4" x2="7" y2="4"/><line x1="3" y1="6" x2="7" y2="6"/>
            <line x1="9" y1="4" x2="15" y2="4"/><line x1="9" y1="6" x2="13" y2="6"/>
          </svg>
        )}
        {showOrder ? `계좌순서변경 ${open ? '˄' : '˅'}` : (open ? '˄' : '˅')}
      </button>
    </div>
  )
}

function normalizeAccountType(account: Account): Account['type'] {
  if (['입출금', '적금', '예금', '청약'].includes(account.type)) {
    return account.type
  }

  if (account.name.includes('청약')) return '청약'
  if (account.name.includes('적금')) return '적금'
  if (account.name.includes('통장')) return '입출금'
  return '예금'
}

export default function AccountsPage() {
  const [activeTab, setActiveTab] = useState('예금')
  const [balanceVisible, setBalanceVisible] = useState(true)
  const [mgmtOpen, setMgmtOpen] = useState<string | null>(null)
  const [userName, setUserName] = useState<string | null>(null)
  const [checkingOpen, setCheckingOpen] = useState(true)
  const [savingsOpen, setSavingsOpen] = useState(true)
  const [depositOpen, setDepositOpen] = useState(true)
  const [loanOpen, setLoanOpen] = useState(true)
  const [allDepOpen, setAllDepOpen] = useState(true)
  const [allFundOpen, setAllFundOpen] = useState(true)
  const [allTrustOpen, setAllTrustOpen] = useState(true)
  const [allLoanAllOpen, setAllLoanAllOpen] = useState(true)
  const [allFxOpen, setAllFxOpen] = useState(true)
  const [allInsOpen, setAllInsOpen] = useState(true)
  const [allRetireOpen, setAllRetireOpen] = useState(true)
  const [joinedAccounts, setJoinedAccounts] = useState<Account[]>([])
  const [accountOverrides, setAccountOverrides] = useState<Record<string, number>>({})

  useEffect(() => {
    try {
      const stored = localStorage.getItem('user')
      if (stored) setUserName(JSON.parse(stored).name)
    } catch {}
    let fallbackAccounts: Account[] = []
    try {
      const raw = localStorage.getItem('joinedAccounts')
      if (raw) {
        const parsed = (JSON.parse(raw) as Account[]).map(account => ({
          ...account,
          type: normalizeAccountType(account),
        }))
        fallbackAccounts = parsed
        localStorage.setItem('joinedAccounts', JSON.stringify(parsed))
      }
    } catch {}
    try {
      const raw = localStorage.getItem('accountOverrides')
      if (raw) setAccountOverrides(JSON.parse(raw))
    } catch {}

    let cancelled = false
    async function loadDepositAccounts() {
      try {
        const apiAccounts = await fetchDepositAccountViewModels(getCurrentDepositCustomerId())
        if (!cancelled) {
          setJoinedAccounts(apiAccounts.length > 0 ? apiAccounts : fallbackAccounts)
        }
      } catch {
        if (!cancelled) setJoinedAccounts(fallbackAccounts)
      }
    }

    loadDepositAccounts()
    return () => {
      cancelled = true
    }
  }, [])

  const now = new Date()
  const datetime = `${now.getFullYear()}.${String(now.getMonth()+1).padStart(2,'0')}.${String(now.getDate()).padStart(2,'0')} ${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}:${String(now.getSeconds()).padStart(2,'0')}`

  const allAccounts        = [...joinedAccounts].map(account => ({
    ...account,
    type: normalizeAccountType(account),
    balance: account.balance + (accountOverrides[account.id] || 0),
    availableBalance: account.availableBalance + (accountOverrides[account.id] || 0),
  }))
  const checkingAccounts  = allAccounts.filter(a => a.type === '입출금')
  const savingsAccounts   = allAccounts.filter(a => a.type === '적금')
  const pureDepositAccounts = allAccounts.filter(a => a.type === '예금' || a.type === '청약')
  const depositTabAccounts = allAccounts.filter(a => ['입출금', '적금', '예금', '청약'].includes(a.type))
  const depositTabBalance  = depositTabAccounts.reduce((s, a) => s + a.balance, 0)
  const depositTabCount    = depositTabAccounts.length
  const totalBalance      = depositTabAccounts.reduce((s, a) => s + a.balance, 0)

  const bal = (n: number) => balanceVisible ? formatNumber(n) : '●●●●●●●'

  return (
    <div className="max-w-kb-container mx-auto px-6">
      <div className="flex">

        {/* ===== 사이드바 ===== */}
        <InquirySidebar />

        {/* ===== 본문 ===== */}
        <main className="flex-1 pl-8 pt-4 pb-12">

          {/* 브레드크럼 */}
          <div className="flex justify-end mb-2 text-[12px] text-kb-text-muted gap-1">
            <span>개인뱅킹</span><span>&gt;</span>
            <span>조회</span><span>&gt;</span>
            <span>계좌조회</span><span>&gt;</span>
            <span>AX풀뱅크 계좌조회</span><span>&gt;</span>
            <span className="font-semibold text-kb-text">{activeTab}</span>
            <span className="ml-2 text-kb-blue cursor-pointer">? 도움말</span>
          </div>

          {/* 제목 + 링크 */}
          <div className="flex items-start justify-between mb-4">
            <h1 className="text-[20px] font-bold text-kb-text">AX풀뱅크 계좌조회</h1>
            <div className="flex gap-3 text-[13px] text-kb-blue">
              <Link href="#" className="hover:underline">다른금융 계좌조회 &gt;</Link>
              <Link href="#" className="hover:underline">AXful금융그룹 통합조회 &gt;</Link>
            </div>
          </div>

          {/* 계좌 탭 */}
          <div className="flex border-b border-kb-border mb-4">
            {ACCOUNT_TABS.map((tab) => (
              <button key={tab} onClick={() => setActiveTab(tab)}
                className={`px-4 py-2 text-[13px] border-b-2 -mb-px transition-colors ${
                  activeTab === tab
                    ? 'border-kb-text font-bold text-kb-text'
                    : 'border-transparent text-kb-text-muted hover:text-kb-text'
                }`}>
                {tab}
              </button>
            ))}
          </div>

          {/* 조회기준일시 */}
          <p className="text-[12px] text-kb-text-muted mb-4">조회기준일시 : {datetime}</p>

          {/* 인사 + 잔액 토글 */}
          <div className="flex items-start justify-between mb-2">
            <div>
              <p className="text-[16px] font-bold text-kb-text">{userName ? `${userName} 고객님,` : '고객님,'}</p>
              <p className="text-[14px] text-kb-text-muted">바쁜 오후, 잠시 틈을 내어</p>
              <p className="text-[14px] text-kb-text-muted">차 한 잔의 여유를 즐겨보세요.</p>
            </div>
            <div className="text-right">
              <div className="flex items-center justify-end gap-2 mb-1">
                <span className="text-[12px] text-kb-text-muted">잔액보기</span>
                <button
                  onClick={() => setBalanceVisible(v => !v)}
                  className={`relative w-10 h-5 rounded-full transition-colors ${balanceVisible ? 'bg-kb-blue' : 'bg-gray-300'}`}
                >
                  <span className={`absolute top-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform ${balanceVisible ? 'translate-x-5' : 'translate-x-0.5'}`} />
                  <span className={`absolute text-[9px] font-bold text-white ${balanceVisible ? 'left-1.5 top-0.5' : 'right-1 top-0.5'}`}>
                    {balanceVisible ? 'ON' : 'OFF'}
                  </span>
                </button>
              </div>
              <p className="text-[12px] text-kb-text-muted">총 잔액(예금,펀드,신탁/ISA)</p>
              <p className="text-[22px] font-bold text-kb-text">{bal(totalBalance)}원</p>
              <button className="text-[12px] text-kb-text-muted underline">잔액에 포함되지 않는 계좌 안내</button>
            </div>
          </div>

          <p className="text-[12px] text-kb-text-muted mb-6">
            * 안전한 금융거래를 위해 사용자암호 변경을 권장드립니다.{' '}
            <Link href="#" className="text-kb-blue underline">바로가기 &gt;</Link>
          </p>

          {/* ============================== 예금 탭 ============================== */}
          {activeTab === '예금' && (
            <>
              {/* 총 예금 잔액 헤더 */}
              <div className="flex items-center justify-between mb-5">
                <p className="text-[14px] font-bold text-kb-text">
                  총 예금 잔액{' '}
                  <span className="text-[17px] font-bold" style={{ color: '#C23B00' }}>{bal(depositTabBalance)}</span>
                  {' '}원
                  <span className="text-kb-text-muted font-normal text-[13px] ml-1">({depositTabCount}계좌)</span>
                </p>
                <button className="flex items-center gap-1.5 border border-kb-border px-3 py-1.5 text-[12px] text-kb-text-muted hover:bg-kb-beige-light">
                  <svg viewBox="0 0 16 16" fill="none" className="w-3.5 h-3.5" stroke="currentColor" strokeWidth="1.5">
                    <rect x="1" y="1" width="6" height="6" rx="0.5"/><rect x="9" y="1" width="6" height="6" rx="0.5"/>
                    <rect x="1" y="9" width="6" height="6" rx="0.5"/><rect x="9" y="9" width="6" height="6" rx="0.5"/>
                  </svg>
                  카드뷰 보기
                </button>
              </div>

              {/* 입출금 섹션 */}
              <div className="mb-6">
                <SectionHeader
                  dotColor="bg-orange-400" label="입출금"
                  count={checkingAccounts.length}
                  balance={bal(checkingAccounts.reduce((s, a) => s + a.balance, 0))}
                  open={checkingOpen} onToggle={() => setCheckingOpen(v => !v)}
                />
                {checkingOpen && checkingAccounts.map((account) => (
                  <div key={account.id} className="border border-[#D5D5D5] rounded-lg p-5 mb-3 bg-white">
                    <div className="flex items-start justify-between gap-4">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1">
                          <Link href={`/accounts/${account.id}`}
                            className="text-[14px] font-bold text-kb-text hover:underline">
                            {account.number}
                          </Link>
                          {account.badge && (
                            <span className="text-[11px] border border-gray-400 text-gray-500 px-1.5 py-0.5 rounded-sm flex-shrink-0">
                              {account.badge}
                            </span>
                          )}
                        </div>
                        <p className="text-[12px] text-kb-text-muted mb-3">{account.name}</p>
                        <p className="text-[13px] text-kb-text">
                          잔액 <span className="font-bold text-[16px]">{bal(account.balance)}</span>원
                        </p>
                      </div>
                      <div className="flex-shrink-0">
                        <div className="grid grid-cols-2 gap-1 mb-1">
                          <Link href="/inquiry/transactions"
                            className="border border-kb-border px-5 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light text-center">
                            조회
                          </Link>
                          <Link href="/transfer/account"
                            className="border border-kb-border px-5 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light text-center">
                            이체
                          </Link>
                        </div>
                        <div className="relative mb-1">
                          <button
                            onClick={() => setMgmtOpen(mgmtOpen === account.id ? null : account.id)}
                            className="w-full border border-kb-border px-5 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light text-center"
                          >
                            계좌관리
                          </button>
                          {mgmtOpen === account.id && (
                            <div className="absolute right-0 top-full mt-1 bg-white border border-kb-border shadow-md z-50 w-[200px] py-2">
                              {MANAGEMENT_ITEMS.map((item) => (
                                <button key={item}
                                  className="block w-full text-left px-4 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light">
                                  · {item}
                                </button>
                              ))}
                            </div>
                          )}
                        </div>
                        <Link href="/products/deposit/inquiry/terminate"
                          className="block w-full border border-[#E05555] px-3 py-1.5 text-[12px] text-[#E05555] hover:bg-red-50 text-center">
                          해지
                        </Link>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              {/* 적금 섹션 */}
              <div className="mb-6">
                <SectionHeader
                  dotColor="bg-blue-400" label="적금"
                  count={savingsAccounts.length}
                  balance={bal(savingsAccounts.reduce((s, a) => s + a.balance, 0))}
                  open={savingsOpen} onToggle={() => setSavingsOpen(v => !v)}
                />
                {savingsOpen && (
                  savingsAccounts.length === 0 ? (
                    <EmptyState
                      message="조회 내용이 없습니다."
                      subMessage="아직 가입된 적금 계좌가 없습니다. 목돈 마련의 첫걸음, 적금을 시작해보세요."
                    />
                  ) : (
                    savingsAccounts.map((account) => (
                      <div key={account.id} className="border border-[#D5D5D5] rounded-lg p-5 mb-3 bg-white">
                        <div className="flex items-start justify-between gap-4">
                          <div className="flex-1 min-w-0">
                            <p className="text-[14px] font-bold text-kb-text mb-1">{account.number}</p>
                            <p className="text-[12px] text-kb-text-muted mb-1">{account.name}</p>
                            <div className="flex gap-4 text-[12px] text-kb-text-muted mb-2">
                              <span>신규일 {account.createdAt}</span>
                              {account.maturityDate && <span>납입월차 {account.maturityDate}</span>}
                            </div>
                            <p className="text-[13px] text-kb-text">
                              잔액 <span className="font-bold text-[16px]">{bal(account.balance)}</span>원
                            </p>
                          </div>
                          <div className="flex-shrink-0">
                            <div className="grid grid-cols-2 gap-1">
                              <button className="border border-kb-border px-3 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light text-center">조회</button>
                              <button className="border border-kb-border px-3 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light text-center">해지예상조회</button>
                              <button className="border border-kb-border px-3 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light text-center">입금</button>
                              <button className="border border-kb-border px-3 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light text-center">계좌관리</button>
                              <Link href="/products/deposit/inquiry/terminate"
                                className="col-span-2 border border-[#E05555] px-3 py-1.5 text-[12px] text-[#E05555] hover:bg-red-50 text-center">
                                해지
                              </Link>
                            </div>
                          </div>
                        </div>
                      </div>
                    ))
                  )
                )}
              </div>

              {/* 예금/시장성계좌 섹션 */}
              <div className="mb-8">
                <SectionHeader
                  dotColor="bg-orange-300" label="예금/시장성계좌"
                  count={pureDepositAccounts.length}
                  balance={bal(pureDepositAccounts.reduce((s, a) => s + a.balance, 0))}
                  open={depositOpen} onToggle={() => setDepositOpen(v => !v)}
                  showOrder={false}
                />
                {depositOpen && (
                  pureDepositAccounts.length === 0 ? (
                    <EmptyState
                      message="조회 내용이 없습니다."
                      subMessage="재테크 고수는 놀지 않는 예금이자, 단 한달이라도 여유자금을 예금에 맡겨보세요."
                      actionHref="/products/deposit"
                      actionLabel="가입하기"
                    />
                  ) : (
                    pureDepositAccounts.map((account) => (
                      <div key={account.id} className="border border-[#D5D5D5] rounded-lg p-5 mb-3 bg-white">
                        <div className="flex items-start justify-between gap-4">
                          <div className="flex-1 min-w-0">
                            <p className="text-[14px] font-bold text-kb-text mb-1">{account.number}</p>
                            <p className="text-[12px] text-kb-text-muted mb-1">{account.name}</p>
                            <div className="flex gap-4 text-[12px] text-kb-text-muted mb-2">
                              <span>신규일 {account.createdAt}</span>
                              {account.maturityDate && <span>만기일 {account.maturityDate}</span>}
                            </div>
                            <p className="text-[13px] text-kb-text">
                              잔액 <span className="font-bold text-[16px]">{bal(account.balance)}</span>원
                            </p>
                          </div>
                          <div className="flex-shrink-0">
                            <div className="grid grid-cols-2 gap-1">
                              <button className="border border-kb-border px-3 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light text-center">조회</button>
                              <button className="border border-kb-border px-3 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light text-center">해지예상조회</button>
                              <button className="border border-kb-border px-3 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light text-center">계좌관리</button>
                              <Link href="/products/deposit/inquiry/terminate"
                                className="border border-[#E05555] px-3 py-1.5 text-[12px] text-[#E05555] hover:bg-red-50 text-center">
                                해지
                              </Link>
                            </div>
                          </div>
                        </div>
                      </div>
                    ))
                  )
                )}
              </div>

              {/* 하단 버튼 */}
              <div className="flex justify-center gap-2 pt-5 border-t border-kb-border">
                <button className="border border-kb-border px-6 py-2 text-[13px] text-kb-text-body hover:bg-kb-beige-light">
                  카테고리 순서변경
                </button>
                <button className="px-6 py-2 text-[13px] text-white font-semibold hover:opacity-90 flex items-center gap-1.5"
                  style={{ backgroundColor: '#3D6B45' }}>
                  <svg viewBox="0 0 16 16" fill="none" className="w-3.5 h-3.5" stroke="currentColor" strokeWidth="1.5">
                    <rect x="2" y="2" width="12" height="12" rx="1"/>
                    <polyline points="5,8 7,10 11,6"/>
                  </svg>
                  저장
                </button>
                <button className="border border-kb-border px-6 py-2 text-[13px] text-kb-text-body hover:bg-kb-beige-light flex items-center gap-1">
                  인쇄
                  <svg viewBox="0 0 12 12" fill="none" className="w-3 h-3" stroke="currentColor" strokeWidth="1.5">
                    <path d="M2 10L10 2M10 2H5M10 2v5"/>
                  </svg>
                </button>
              </div>
            </>
          )}

          {/* ============================== 대출 탭 ============================== */}
          {activeTab === '대출' && (
            <div className="mb-8">
              <SectionHeader
                dotColor="bg-orange-400" label="대출"
                count={0} balance="0"
                open={loanOpen} onToggle={() => setLoanOpen(v => !v)}
                showOrder={false}
              />
              {loanOpen && (
                <EmptyState
                  message="조회 내역이 없습니다."
                  subMessage="긴급 자금이 필요한 모든 순간, 든든하게 힘이 되어 주는 AX풀뱅크에서 대출을 이용하세요."
                  actionHref="/loans/apply"
                  actionLabel="신청하기"
                />
              )}
              <div className="flex justify-end mt-3">
                <Link href="#" className="text-[13px] text-kb-text-body hover:underline">
                  해지계좌보기 &gt;
                </Link>
              </div>
            </div>
          )}

          {/* ============================== 전체계좌 탭 ============================== */}
          {activeTab === '전체계좌' && (
            <div className="mb-8 space-y-3">

              {/* ── 예금 ── */}
              <div className="border border-kb-border">
                <button onClick={() => setAllDepOpen(v => !v)}
                  className="w-full flex items-center justify-between px-4 py-3 bg-[#F5F3F0] text-left">
                  <div className="flex items-center gap-2">
                    <span className="w-2.5 h-2.5 rounded-full bg-orange-400 inline-block" />
                    <span className="text-[13px] font-bold text-kb-text">예금</span>
                    <span className="text-[13px] font-semibold" style={{ color: '#C23B00' }}>{bal(depositTabBalance)}원</span>
                  </div>
                  <span className="text-[12px] text-kb-text-muted">{allDepOpen ? '˄' : '˅'}</span>
                </button>
                {allDepOpen && (
                  <table className="w-full border-collapse">
                    <thead>
                      <tr className="bg-[#F0EDEA] border-b border-kb-border">
                        {['계좌번호', '계좌명', '신규일', '만기일', '잔액'].map(col => (
                          <th key={col} className="px-4 py-2.5 text-[12px] text-kb-text-body font-medium text-center border-r last:border-r-0 border-kb-border">{col}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {depositTabAccounts.map((acc) => (
                        <tr key={acc.id} className="border-b last:border-b-0 border-kb-border hover:bg-kb-beige-light">
                          <td className="px-4 py-3 text-[13px] text-center border-r border-kb-border">
                            <Link href="/inquiry/transactions" className="text-kb-blue hover:underline">{acc.number}</Link>
                            {acc.badge && <span className="ml-1 text-[11px] border border-gray-400 text-gray-500 px-1">{acc.badge}</span>}
                          </td>
                          <td className="px-4 py-3 text-[13px] text-center border-r border-kb-border">{acc.name}</td>
                          <td className="px-4 py-3 text-[13px] text-center border-r border-kb-border">{acc.createdAt}</td>
                          <td className="px-4 py-3 text-[13px] text-center border-r border-kb-border">{acc.maturityDate || ''}</td>
                          <td className="px-4 py-3 text-[13px] text-right pr-4">{bal(acc.balance)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>

              {/* ── 펀드 ── */}
              <div className="border border-kb-border">
                <button onClick={() => setAllFundOpen(v => !v)}
                  className="w-full flex items-center justify-between px-4 py-3 bg-[#F5F3F0] text-left">
                  <div className="flex items-center gap-2">
                    <span className="w-2.5 h-2.5 rounded-full bg-orange-400 inline-block" />
                    <span className="text-[13px] font-bold text-kb-text">펀드</span>
                    <span className="text-[11px] border border-gray-400 text-gray-500 px-1">?</span>
                    <span className="text-[13px] font-semibold" style={{ color: '#C23B00' }}>0원</span>
                  </div>
                  <span className="text-[12px] text-kb-text-muted">{allFundOpen ? '˄' : '˅'}</span>
                </button>
                {allFundOpen && (
                  <table className="w-full border-collapse">
                    <thead>
                      <tr className="bg-[#F0EDEA] border-b border-kb-border">
                        {['계좌번호', '펀드명', '신규일', '만기일', '수익률', '종가액'].map(col => (
                          <th key={col} className="px-4 py-2.5 text-[12px] text-kb-text-body font-medium text-center border-r last:border-r-0 border-kb-border">{col}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      <tr><td colSpan={6} className="text-center py-8 text-[13px] text-kb-text-muted">검색된 결과가 없습니다.</td></tr>
                    </tbody>
                  </table>
                )}
              </div>

              {/* ── 신탁 ── */}
              <div className="border border-kb-border">
                <button onClick={() => setAllTrustOpen(v => !v)}
                  className="w-full flex items-center justify-between px-4 py-3 bg-[#F5F3F0] text-left">
                  <div className="flex items-center gap-2">
                    <span className="w-2.5 h-2.5 rounded-full bg-orange-400 inline-block" />
                    <span className="text-[13px] font-bold text-kb-text">신탁</span>
                    <span className="text-[11px] border border-gray-400 text-gray-500 px-1">?</span>
                    <span className="text-[13px] font-semibold" style={{ color: '#C23B00' }}>0원</span>
                  </div>
                  <span className="text-[12px] text-kb-text-muted">{allTrustOpen ? '˄' : '˅'}</span>
                </button>
                {allTrustOpen && (
                  <table className="w-full border-collapse">
                    <thead>
                      <tr className="bg-[#F0EDEA] border-b border-kb-border">
                        {['계좌번호', '계좌명', '잔액', '평가액', '수익률'].map(col => (
                          <th key={col} className="px-4 py-2.5 text-[12px] text-kb-text-body font-medium text-center border-r last:border-r-0 border-kb-border">{col}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      <tr><td colSpan={5} className="text-center py-8 text-[13px] text-kb-text-muted">검색된 결과가 없습니다.</td></tr>
                    </tbody>
                  </table>
                )}
              </div>

              {/* ── 대출 ── */}
              <div className="border border-kb-border">
                <button onClick={() => setAllLoanAllOpen(v => !v)}
                  className="w-full flex items-center justify-between px-4 py-3 bg-[#F5F3F0] text-left">
                  <div className="flex items-center gap-2">
                    <span className="w-2.5 h-2.5 rounded-full bg-orange-400 inline-block" />
                    <span className="text-[13px] font-bold text-kb-text">대출</span>
                    <span className="text-[13px] font-semibold" style={{ color: '#C23B00' }}>0원</span>
                  </div>
                  <span className="text-[12px] text-kb-text-muted">{allLoanAllOpen ? '˄' : '˅'}</span>
                </button>
                {allLoanAllOpen && (
                  <table className="w-full border-collapse">
                    <thead>
                      <tr className="bg-[#F0EDEA] border-b border-kb-border">
                        {['계좌번호', '계좌명', '만기일', '대출잔액'].map(col => (
                          <th key={col} className="px-4 py-2.5 text-[12px] text-kb-text-body font-medium text-center border-r last:border-r-0 border-kb-border">{col}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      <tr><td colSpan={4} className="text-center py-8 text-[13px] text-kb-text-muted">검색된 결과가 없습니다.</td></tr>
                    </tbody>
                  </table>
                )}
              </div>

              {/* ── 외화/골드 ── */}
              <div className="border border-kb-border">
                <button onClick={() => setAllFxOpen(v => !v)}
                  className="w-full flex items-center justify-between px-4 py-3 bg-[#F5F3F0] text-left">
                  <div className="flex items-center gap-2">
                    <span className="w-2.5 h-2.5 rounded-full bg-orange-400 inline-block" />
                    <span className="text-[13px] font-bold text-kb-text">외화/골드</span>
                    <span className="text-[11px] border border-gray-400 text-gray-500 px-1">?</span>
                    <span className="text-[13px] font-semibold" style={{ color: '#C23B00' }}>0원</span>
                  </div>
                  <span className="text-[12px] text-kb-text-muted">{allFxOpen ? '˄' : '˅'}</span>
                </button>
                {allFxOpen && (
                  <table className="w-full border-collapse">
                    <thead>
                      <tr className="bg-[#F0EDEA] border-b border-kb-border">
                        {['계좌번호', '상품', '통화/잔액'].map(col => (
                          <th key={col} className="px-4 py-2.5 text-[12px] text-kb-text-body font-medium text-center border-r last:border-r-0 border-kb-border">{col}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      <tr><td colSpan={3} className="text-center py-8 text-[13px] text-kb-text-muted">검색된 결과가 없습니다.</td></tr>
                    </tbody>
                  </table>
                )}
              </div>

              {/* ── 보험/공제 ── */}
              <div className="border border-kb-border">
                <button onClick={() => setAllInsOpen(v => !v)}
                  className="w-full flex items-center justify-between px-4 py-3 bg-[#F5F3F0] text-left">
                  <div className="flex items-center gap-2">
                    <span className="w-2.5 h-2.5 rounded-full bg-orange-400 inline-block" />
                    <span className="text-[13px] font-bold text-kb-text">보험/공제</span>
                  </div>
                  <span className="text-[12px] text-kb-text-muted">{allInsOpen ? '˄' : '˅'}</span>
                </button>
                {allInsOpen && (
                  <>
                    <table className="w-full border-collapse">
                      <thead>
                        <tr className="bg-[#F0EDEA] border-b border-kb-border">
                          {['보험계좌번호', '보험사명', '상품명', '계약년월일'].map(col => (
                            <th key={col} className="px-4 py-2.5 text-[12px] text-kb-text-body font-medium text-center border-r last:border-r-0 border-kb-border">{col}</th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        <tr><td colSpan={4} className="text-center py-8 text-[13px] text-kb-text-muted">검색된 결과가 없습니다.</td></tr>
                      </tbody>
                    </table>
                    <p className="text-[11px] text-kb-text-muted px-4 py-2">* 해당 보험사에서 직접 거래하신 경우 조회된 정보가 실시간 업데이트 되지 않을 수 있습니다.</p>
                  </>
                )}
              </div>

              {/* ── 퇴직연금 ── */}
              <div className="border border-kb-border">
                <button onClick={() => setAllRetireOpen(v => !v)}
                  className="w-full flex items-center justify-between px-4 py-3 bg-[#F5F3F0] text-left">
                  <div className="flex items-center gap-2">
                    <span className="w-2.5 h-2.5 rounded-full bg-green-500 inline-block" />
                    <span className="text-[13px] font-bold text-kb-text">퇴직연금</span>
                  </div>
                  <span className="text-[12px] text-kb-text-muted">{allRetireOpen ? '˄' : '˅'}</span>
                </button>
                {allRetireOpen && (
                  <table className="w-full border-collapse">
                    <thead>
                      <tr className="bg-[#F0EDEA] border-b border-kb-border">
                        {['계좌번호', '고객명', '제도상세구분', '평가금액'].map(col => (
                          <th key={col} className="px-4 py-2.5 text-[12px] text-kb-text-body font-medium text-center border-r last:border-r-0 border-kb-border">{col}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      <tr><td colSpan={4} className="text-center py-8 text-[13px] text-kb-text-muted">검색된 결과가 없습니다.</td></tr>
                    </tbody>
                  </table>
                )}
              </div>

              {/* 하단 버튼 */}
              <div className="flex justify-center gap-2 pt-5 border-t border-kb-border">
                <button className="px-6 py-2 text-[13px] text-white font-semibold hover:opacity-90 flex items-center gap-1.5"
                  style={{ backgroundColor: '#3D6B45' }}>
                  <svg viewBox="0 0 16 16" fill="none" className="w-3.5 h-3.5" stroke="currentColor" strokeWidth="1.5">
                    <rect x="2" y="2" width="12" height="12" rx="1"/><polyline points="5,8 7,10 11,6"/>
                  </svg>
                  저장
                </button>
                <button className="border border-kb-border px-6 py-2 text-[13px] text-kb-text-body hover:bg-kb-beige-light flex items-center gap-1">
                  인쇄
                  <svg viewBox="0 0 12 12" fill="none" className="w-3 h-3" stroke="currentColor" strokeWidth="1.5">
                    <path d="M2 10L10 2M10 2H5M10 2v5"/>
                  </svg>
                </button>
              </div>
            </div>
          )}

        </main>
      </div>
    </div>
  )
}
