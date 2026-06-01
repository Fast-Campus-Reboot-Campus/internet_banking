'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import DepositSidebar from '@/components/products/DepositSidebar'
import { formatNumber } from '@/lib/mock-data'
import {
  DepositViewAccount,
  fetchDepositAccountViewModels,
  getCurrentDepositCustomerId,
  terminateDepositContract,
} from '@/lib/deposit-api'

const STEP_LABELS = ['계좌조회/선택', '해지계좌 확인', '완료']
type Step = 1 | 2 | 3

export default function DepositTerminatePage() {
  const [step, setStep] = useState<Step>(1)
  const [selected, setSelected] = useState<DepositViewAccount | null>(null)
  const [joinedAccounts, setJoinedAccounts] = useState<DepositViewAccount[]>([])
  const [password, setPassword] = useState('')
  const [mouseInput, setMouseInput] = useState(false)
  const [depositNo, setDepositNo] = useState('')
  const [depositOpen,        setDepositOpen]        = useState(true)
  const [regularSavingsOpen, setRegularSavingsOpen] = useState(true)
  const [freeSavingsOpen,    setFreeSavingsOpen]    = useState(true)
  const [checkingOpen,       setCheckingOpen]       = useState(true)
  const [subscriptionOpen,   setSubscriptionOpen]   = useState(true)

  useEffect(() => {
    let fallbackAccounts: DepositViewAccount[] = []
    try {
      const raw = localStorage.getItem('joinedAccounts')
      if (raw) fallbackAccounts = JSON.parse(raw)
    } catch {}

    let cancelled = false
    async function loadAccounts() {
      try {
        const apiAccounts = await fetchDepositAccountViewModels(getCurrentDepositCustomerId())
        if (!cancelled) setJoinedAccounts(apiAccounts.length > 0 ? apiAccounts : fallbackAccounts)
      } catch {
        if (!cancelled) setJoinedAccounts(fallbackAccounts)
      }
    }
    loadAccounts()
    return () => { cancelled = true }
  }, [])

  const allAccounts: DepositViewAccount[] = joinedAccounts
  const pureDepositAccounts    = allAccounts.filter(a => a.type === '예금')
  const regularSavingsAccounts = allAccounts.filter(a => a.type === '적금' && a.savingType === 'REGULAR')
  const freeSavingsAccounts    = allAccounts.filter(a => a.type === '적금' && a.savingType === 'FREE')
  const checkingAccounts       = allAccounts.filter(a => a.type === '입출금')
  const subscriptionAccounts   = allAccounts.filter(a => a.type === '청약')

  const totalBalance = allAccounts.reduce((s, a) => s + a.balance, 0)
  const totalCount   = allAccounts.length

  function handleSelect(acc: DepositViewAccount) {
    setSelected(acc)
    setStep(2)
  }

  async function handleConfirm() {
    if (!password && !mouseInput) { alert('해지계좌 비밀번호를 입력해주세요.'); return }
    if (!depositNo) { alert('입금계좌를 선택해주세요.'); return }
    if (selected) {
      try {
        if (selected.contractId) await terminateDepositContract(selected.contractId)
        const terminatedBalance = selected.balance
        let updated = joinedAccounts.filter(a => a.id !== selected.id)
        const targetInJoined = updated.find(a => a.id === depositNo)
        if (targetInJoined) {
          updated = updated.map(a => a.id === depositNo
            ? { ...a, balance: a.balance + terminatedBalance, availableBalance: a.availableBalance + terminatedBalance }
            : a)
        } else {
          const overrides = JSON.parse(localStorage.getItem('accountOverrides') || '{}')
          overrides[depositNo] = (overrides[depositNo] || 0) + terminatedBalance
          localStorage.setItem('accountOverrides', JSON.stringify(overrides))
        }
        localStorage.setItem('joinedAccounts', JSON.stringify(updated))
        setJoinedAccounts(updated)
      } catch {}
    }
    setStep(3)
  }

  function AccountRow({ acc }: { acc: DepositViewAccount }) {
    return (
      <tr className="border-b hover:bg-[#F8FFFE] transition-colors" style={{ borderColor: '#E2F5EF' }}>
        <td className="px-4 py-3.5">
          <p className="font-medium" style={{ color: '#0D5C47' }}>{acc.number}</p>
          <p className="text-[11px] text-kb-text-muted mt-0.5">신규일 {acc.createdAt}</p>
        </td>
        <td className="px-4 py-3.5 text-kb-text">{acc.name}</td>
        <td className="px-4 py-3.5 text-right font-semibold" style={{ color: '#0D5C47' }}>{formatNumber(acc.balance)}원</td>
        <td className="px-4 py-3.5 text-right">
          <div className="flex gap-1.5 justify-end">
            <button
              onClick={() => handleSelect(acc)}
              className="px-4 py-1.5 text-[12px] font-bold text-white rounded-lg hover:opacity-85 transition-opacity"
              style={{ backgroundColor: '#0D5C47' }}>
              해지
            </button>
            <button className="px-4 py-1.5 text-[12px] font-medium rounded-lg border transition-colors hover:bg-[#F0FAF7]"
              style={{ borderColor: '#5BC9A8', color: '#0D5C47' }}>
              해지상세조회
            </button>
          </div>
        </td>
      </tr>
    )
  }

  const rowStyle = "px-5 py-3.5 text-[13px]"
  const labelStyle = "font-semibold text-kb-text w-[150px] whitespace-nowrap"

  return (
    <div className="max-w-kb-container mx-auto px-6">
      <div className="flex">
        <DepositSidebar />

        <main className="flex-1 pl-8 pt-6 pb-12">
          <h1 className="text-[22px] font-bold text-kb-text mb-5">예금/적금 해지</h1>

          {/* 단계 표시 */}
          <div className="flex items-center gap-2 mb-6">
            {STEP_LABELS.map((label, i) => (
              <div key={i} className="flex items-center gap-2">
                {i > 0 && <span className="text-kb-text-muted">›</span>}
                <span
                  className="text-[13px]"
                  style={i + 1 === step
                    ? { color: '#0D5C47', fontWeight: 700, borderBottom: '2px solid #0D5C47', paddingBottom: '2px' }
                    : { color: '#9CA3AF' }}>
                  STEP {i + 1}. {label}
                </span>
              </div>
            ))}
          </div>

          {/* STEP 1 */}
          {step === 1 && (
            <>
              <div className="rounded-xl px-5 py-4 mb-5 text-[12px] space-y-1.5"
                style={{ backgroundColor: '#F8FFFE', border: '1px solid #E2F5EF' }}>
                <p className="flex gap-1.5 text-kb-text-muted">
                  <span className="flex-shrink-0">·</span>
                  <span>인터넷으로 해지 가능한 예금은 우측 [도움말]을 참조하시기 바랍니다.</span>
                </p>
                <p className="flex gap-1.5 text-kb-text-muted">
                  <span className="flex-shrink-0">·</span>
                  <span>해지예상조회를 이용하여 해지 시 고객님의 해지계좌번호를 다시 한번 확인하여 선택하기 바랍니다.</span>
                </p>
                <p className="flex gap-1.5 font-medium" style={{ color: '#E05555' }}>
                  <span className="flex-shrink-0">·</span>
                  <span>청약관련예금과 장기주택마련저축은 추가사항이 필요한 상품으로 인터넷뱅킹을 통한 해지가 제한됩니다.</span>
                </p>
              </div>

              {/* 총 잔액 요약 */}
              <div className="flex items-center justify-between mb-5 rounded-xl px-6 py-4"
                style={{ backgroundColor: '#F8FFFE', border: '1px solid #E2F5EF' }}>
                <p className="text-[14px] font-bold text-kb-text">
                  총 예금 잔액{' '}
                  <span className="text-[18px]" style={{ color: '#0D5C47' }}>{formatNumber(totalBalance)}</span>원
                  <span className="text-kb-text-muted font-normal text-[13px] ml-1">({totalCount}계좌)</span>
                </p>
              </div>

              {/* 섹션 공통 렌더 헬퍼 */}
              {(
                [
                  { label: '예금',    accounts: pureDepositAccounts,    open: depositOpen,        toggle: () => setDepositOpen(v => !v) },
                  { label: '정기적금', accounts: regularSavingsAccounts, open: regularSavingsOpen, toggle: () => setRegularSavingsOpen(v => !v) },
                  { label: '자유적금', accounts: freeSavingsAccounts,    open: freeSavingsOpen,    toggle: () => setFreeSavingsOpen(v => !v) },
                  { label: '입출금',  accounts: checkingAccounts,        open: checkingOpen,       toggle: () => setCheckingOpen(v => !v) },
                  { label: '청약',    accounts: subscriptionAccounts,    open: subscriptionOpen,   toggle: () => setSubscriptionOpen(v => !v) },
                ] as const
              ).map(({ label, accounts, open, toggle }) => (
                <div key={label} className="rounded-xl overflow-hidden mb-4" style={{ border: '1px solid #E2F5EF' }}>
                  <button
                    onClick={toggle}
                    className="flex items-center justify-between w-full px-5 py-3.5"
                    style={{ backgroundColor: '#F0FAF7', borderBottom: open ? '1px solid #E2F5EF' : 'none' }}>
                    <div className="flex items-center gap-2">
                      <span className="w-2 h-2 rounded-full inline-block" style={{ backgroundColor: '#5BC9A8' }} />
                      <span className="text-[14px] font-bold text-kb-text">{label}</span>
                      <span className="text-[13px] text-kb-text-muted">({accounts.length}계좌)</span>
                      <span className="text-[13px] font-semibold ml-1" style={{ color: '#0D5C47' }}>
                        잔액 {formatNumber(accounts.reduce((s, a) => s + a.balance, 0))}원
                      </span>
                    </div>
                    <span className="text-[18px] text-kb-text-muted leading-none">{open ? '˄' : '˅'}</span>
                  </button>
                  {open && (
                    accounts.length === 0 ? (
                      <p className="px-5 py-5 text-[13px] text-kb-text-muted text-center">조회하실 내역이 없습니다.</p>
                    ) : (
                      <table className="w-full border-collapse text-[13px]">
                        <thead>
                          <tr style={{ backgroundColor: '#F8FFFE' }}>
                            {['계좌번호', '상품명', '잔액', ''].map((h, i) => (
                              <th key={i} className="px-4 py-2.5 text-[12px] font-semibold text-left"
                                style={{ borderBottom: '1px solid #E2F5EF', color: '#0D5C47' }}>{h}</th>
                            ))}
                          </tr>
                        </thead>
                        <tbody>
                          {accounts.map(acc => <AccountRow key={acc.id} acc={acc} />)}
                        </tbody>
                      </table>
                    )
                  )}
                </div>
              ))}
            </>
          )}

          {/* STEP 2 */}
          {step === 2 && selected && (
            <>
              <div className="rounded-xl px-5 py-4 mb-5 text-[12px]"
                style={{ backgroundColor: '#F8FFFE', border: '1px solid #E2F5EF' }}>
                <p className="flex gap-1.5 text-kb-text-muted">
                  <span className="flex-shrink-0">·</span>
                  <span>해지 시 잔액이 선택하신 입금계좌로 이체됩니다.</span>
                </p>
              </div>

              <div className="rounded-xl overflow-hidden mb-6" style={{ border: '1px solid #E2F5EF' }}>
                <table className="w-full border-collapse text-[13px]">
                  <tbody>
                    {[
                      { label: '해지계좌번호', value: <span className="font-medium" style={{ color: '#0D5C47' }}>{selected.number}</span> },
                      { label: '해지계좌명',   value: selected.name },
                      { label: '해지금액',      value: <span className="font-bold text-[15px]" style={{ color: '#0D5C47' }}>{formatNumber(selected.balance)}원</span> },
                    ].map(({ label, value }, i) => (
                      <tr key={i} style={{ borderBottom: '1px solid #E2F5EF' }}>
                        <td className={`${rowStyle} ${labelStyle}`} style={{ backgroundColor: '#F0FAF7', width: 160 }}>{label}</td>
                        <td className={rowStyle}>{value}</td>
                      </tr>
                    ))}
                    <tr style={{ borderBottom: '1px solid #E2F5EF' }}>
                      <td className={`${rowStyle} ${labelStyle}`} style={{ backgroundColor: '#F0FAF7' }}>해지계좌비밀번호</td>
                      <td className={rowStyle}>
                        <div className="flex items-center gap-3">
                          <input
                            type={mouseInput ? 'text' : 'password'}
                            value={password}
                            onChange={e => setPassword(e.target.value)}
                            maxLength={4}
                            placeholder="4자리 입력"
                            className="border rounded-lg px-3 py-1.5 text-[13px] w-28 outline-none focus:ring-1"
                            style={{ borderColor: '#D1D5DB' }}
                          />
                          <label className="flex items-center gap-1.5 text-[12px] text-kb-text-muted cursor-pointer">
                            <input type="checkbox" checked={mouseInput} onChange={e => setMouseInput(e.target.checked)} />
                            마우스로 입력
                          </label>
                        </div>
                      </td>
                    </tr>
                    <tr>
                      <td className={`${rowStyle} ${labelStyle}`} style={{ backgroundColor: '#F0FAF7' }}>입금계좌번호</td>
                      <td className={rowStyle}>
                        <select
                          value={depositNo}
                          onChange={e => setDepositNo(e.target.value)}
                          className="border rounded-lg px-3 py-1.5 text-[13px] outline-none bg-white"
                          style={{ borderColor: '#D1D5DB' }}>
                          <option value="">- 선택 -</option>
                          {checkingAccounts.map(a => (
                            <option key={a.id} value={a.id}>{a.number} ({a.name})</option>
                          ))}
                        </select>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div className="flex justify-center gap-3">
                <button
                  onClick={() => setStep(1)}
                  className="border rounded-xl px-12 py-3 text-[14px] font-medium transition-colors hover:bg-[#F0FAF7]"
                  style={{ borderColor: '#D1D5DB', color: '#6B7280' }}>
                  이전
                </button>
                <button
                  onClick={handleConfirm}
                  className="px-12 py-3 text-[14px] font-bold text-white rounded-xl hover:opacity-85 transition-opacity"
                  style={{ backgroundColor: '#0D5C47' }}>
                  해지
                </button>
              </div>
            </>
          )}

          {/* STEP 3 */}
          {step === 3 && (
            <>
              <div className="rounded-xl p-6 mb-6 flex items-center gap-5"
                style={{ backgroundColor: '#F0FAF7', border: '1px solid #E2F5EF' }}>
                <div className="w-12 h-12 rounded-full flex items-center justify-center flex-shrink-0"
                  style={{ backgroundColor: '#0D5C47' }}>
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                    <polyline points="20 6 9 17 4 12"/>
                  </svg>
                </div>
                <div>
                  <p className="text-[16px] font-bold mb-1" style={{ color: '#0D5C47' }}>해지가 완료되었습니다.</p>
                  <p className="text-[12px] text-kb-text-muted">해지 결과는 해지결과/내역 조회에서 확인하실 수 있습니다.</p>
                </div>
              </div>

              <div className="flex justify-center gap-3">
                <Link href="/products/deposit/inquiry/terminate-result"
                  className="px-8 py-2.5 text-[14px] font-bold text-white rounded-xl hover:opacity-85 transition-opacity"
                  style={{ backgroundColor: '#0D5C47' }}>
                  해지결과 조회
                </Link>
                <Link href="/inquiry/accounts"
                  className="border rounded-xl px-8 py-2.5 text-[14px] font-medium transition-colors hover:bg-[#F0FAF7]"
                  style={{ borderColor: '#5BC9A8', color: '#0D5C47' }}>
                  계좌 조회
                </Link>
              </div>
            </>
          )}
        </main>
      </div>
    </div>
  )
}
