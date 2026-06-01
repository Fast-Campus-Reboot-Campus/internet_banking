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

const STEPS = ['1. 계좌조회/선택', '2. 해지계좌확인/정보입력', '3', '4', '+']

type Step = 1 | 2 | 3

export default function DepositTerminatePage() {
  const [step, setStep] = useState<Step>(1)
  const [selected, setSelected] = useState<DepositViewAccount | null>(null)
  const [joinedAccounts, setJoinedAccounts] = useState<DepositViewAccount[]>([])
  const [password, setPassword] = useState('')
  const [mouseInput, setMouseInput] = useState(false)
  const [depositNo, setDepositNo] = useState('')
  const [installOpen, setInstallOpen] = useState(true)
  const [depositOpen, setDepositOpen] = useState(true)

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
    return () => {
      cancelled = true
    }
  }, [])

  const allAccounts: DepositViewAccount[] = joinedAccounts
  const installmentAccounts = allAccounts.filter(a => a.type === '적금' || a.type === '청약')
  const pureDepositAccounts = allAccounts.filter(a => a.type === '예금')
  const checkingAccounts    = allAccounts.filter(a => a.type === '입출금')

  function handleSelect(acc: DepositViewAccount) {
    setSelected(acc)
    setStep(2)
  }

  async function handleConfirm() {
    if (!password && !mouseInput) { alert('해지계좌 비밀번호를 입력해주세요.'); return }
    if (!depositNo) { alert('입금계좌를 선택해주세요.'); return }
    if (selected) {
      try {
        if (selected.contractId) {
          await terminateDepositContract(selected.contractId)
        }
        const terminatedBalance = selected.balance
        let updated = joinedAccounts.filter(a => a.id !== selected.id)

        // 해지금액을 입금 계좌에 합산
        const targetInJoined = updated.find(a => a.id === depositNo)
        if (targetInJoined) {
          updated = updated.map(a => a.id === depositNo
            ? { ...a, balance: a.balance + terminatedBalance, availableBalance: a.availableBalance + terminatedBalance }
            : a
          )
        } else {
          // MOCK_ACCOUNT인 경우 별도 override 저장
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

  const stepBtnCls = (i: number) =>
    i + 1 === step
      ? 'px-4 py-1.5 text-[12px] font-bold text-white'
      : 'px-4 py-1.5 text-[12px] text-kb-text-body border border-kb-border bg-white hover:bg-kb-beige-light'

  function AccountRow({ acc }: { acc: DepositViewAccount }) {
    return (
      <tr className="border-t border-kb-border">
        <td className="px-4 py-3 text-kb-text-body">
          <p>{acc.number}</p>
          <p className="text-[11px] text-kb-text-muted mt-0.5">신규일 {acc.createdAt}</p>
        </td>
        <td className="px-4 py-3 text-kb-text-body">{acc.name}</td>
        <td className="px-4 py-3 text-right text-kb-text-body font-medium">{formatNumber(acc.balance)}원</td>
        <td className="px-4 py-3 text-right">
          <div className="flex gap-1 justify-end">
            <button
              onClick={() => handleSelect(acc)}
              className="px-3 py-1.5 text-[12px] font-bold text-white hover:opacity-90"
              style={{ backgroundColor: '#5BC9A8' }}>
              해지
            </button>
            <button className="border border-kb-border px-3 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light">
              해지상세조회
            </button>
          </div>
        </td>
      </tr>
    )
  }

  return (
    <div className="max-w-kb-container mx-auto px-6 py-6">
      <div className="flex justify-end mb-3 text-[12px] text-kb-text-muted gap-1 items-center">
        <span>개인뱅킹</span><span>›</span>
        <span>금융상품</span><span>›</span>
        <span>예금</span><span>›</span>
        <Link href="/products/deposit/inquiry/terminate" className="hover:underline">예금/적금 해지</Link>
        <span>›</span>
        <Link href="#" className="text-kb-blue hover:underline">도움말</Link>
      </div>

      <div className="flex gap-6">
        <DepositSidebar />

        <main className="flex-1 min-w-0">
          <div className="flex items-center justify-between mb-5">
            <h1 className="text-[20px] font-bold text-kb-text">예금/적금 해지</h1>
            <div className="flex gap-1">
              {STEPS.map((s, i) => (
                <button key={s} className={stepBtnCls(i)}
                  style={i + 1 === step ? { backgroundColor: '#5BC9A8' } : {}}>
                  {s}
                </button>
              ))}
            </div>
          </div>

          {/* STEP 1 */}
          {step === 1 && (
            <>
              <div className="border border-kb-border bg-[#FAFAFA] px-4 py-3 mb-5 text-[12px] text-kb-text-body space-y-1">
                {[
                  '인터넷으로 해지 가능한 예금은 오른쪽 상단의 [도움말]을 참조하시기 바랍니다.',
                  '해지예상조회를 이용하여 해지면 고객님의 해지계좌번호를 다시 한번 확인하여 선택하기 바랍니다.',
                  <span key="r" className="text-[#E05555]">청약관련예금과 장기주택마련저축은 추가사항이 필요한 상품으로 인터넷뱅킹을 통한 해지가 제한됩니다.</span>,
                ].map((n, i) => (
                  <p key={i} className="flex gap-1.5"><span className="flex-shrink-0">-</span><span>{n}</span></p>
                ))}
              </div>

              {/* 거치식(예금) */}
              <div className="border border-kb-border mb-3">
                <button onClick={() => setDepositOpen(v => !v)}
                  className="flex items-center justify-between w-full px-4 py-3 bg-[#FAFAFA] border-b border-kb-border">
                  <span className="text-[13px] font-bold text-kb-text">거치식 예금계좌</span>
                  <span className="text-[11px] border border-kb-border px-3 py-1 bg-white hover:bg-kb-beige-light text-kb-text-muted">{depositOpen ? '－' : '＋'}</span>
                </button>
                {depositOpen && (
                  pureDepositAccounts.length === 0 ? (
                    <div className="px-4 py-3 text-[13px] text-kb-text-muted">조회하실 내역이 없습니다</div>
                  ) : (
                    <table className="w-full border-collapse text-[13px]">
                      <tbody>{pureDepositAccounts.map(acc => <AccountRow key={acc.id} acc={acc} />)}</tbody>
                    </table>
                  )
                )}
              </div>

              {/* 적립식(적금/청약) */}
              <div className="border border-kb-border mb-3">
                <button onClick={() => setInstallOpen(v => !v)}
                  className="flex items-center justify-between w-full px-4 py-3 bg-[#FAFAFA] border-b border-kb-border">
                  <span className="text-[13px] font-bold text-kb-text">적립식 예금계좌</span>
                  <span className="text-[11px] border border-kb-border px-3 py-1 bg-white hover:bg-kb-beige-light text-kb-text-muted">{installOpen ? '－' : '＋'}</span>
                </button>
                {installOpen && (
                  installmentAccounts.length === 0 ? (
                    <div className="px-4 py-3 text-[13px] text-kb-text-muted">조회하실 내역이 없습니다</div>
                  ) : (
                    <table className="w-full border-collapse text-[13px]">
                      <tbody>{installmentAccounts.map(acc => <AccountRow key={acc.id} acc={acc} />)}</tbody>
                    </table>
                  )
                )}
              </div>
            </>
          )}

          {/* STEP 2 */}
          {step === 2 && selected && (
            <>
              <div className="border border-kb-border bg-[#FAFAFA] px-4 py-3 mb-5 text-[12px] text-kb-text-body">
                <p className="flex gap-1.5"><span>-</span><span>해지 시 잔액이 선택하신 입금계좌로 이체됩니다.</span></p>
              </div>

              <table className="w-full border-collapse text-[13px] border-t-2 border-kb-text">
                <tbody>
                  <tr>
                    <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text w-[140px] whitespace-nowrap">해지계좌번호</td>
                    <td className="border border-kb-border px-4 py-3 text-kb-text-body">{selected.number}</td>
                  </tr>
                  <tr>
                    <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text">해지계좌명</td>
                    <td className="border border-kb-border px-4 py-3 text-kb-text-body">{selected.name}</td>
                  </tr>
                  <tr>
                    <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text">해지금액</td>
                    <td className="border border-kb-border px-4 py-3 text-kb-text-body font-semibold">{formatNumber(selected.balance)}원</td>
                  </tr>
                  <tr>
                    <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text">해지계좌비밀번호</td>
                    <td className="border border-kb-border px-4 py-3">
                      <div className="flex items-center gap-3">
                        <input
                          type={mouseInput ? 'text' : 'password'}
                          value={password}
                          onChange={e => setPassword(e.target.value)}
                          maxLength={4}
                          className="border border-kb-border px-3 py-1.5 text-[13px] w-28 outline-none"
                        />
                        <label className="flex items-center gap-1.5 text-[12px] text-kb-text-body cursor-pointer">
                          <input type="checkbox" checked={mouseInput} onChange={e => setMouseInput(e.target.checked)} />
                          마우스로 입력
                        </label>
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text">입금계좌번호</td>
                    <td className="border border-kb-border px-4 py-3">
                      <select value={depositNo} onChange={e => setDepositNo(e.target.value)}
                        className="border border-kb-border px-3 py-1.5 text-[13px] outline-none bg-white">
                        <option value="">-선택-</option>
                        {checkingAccounts.map(a => (
                          <option key={a.id} value={a.id}>{a.number} ({a.name})</option>
                        ))}
                      </select>
                    </td>
                  </tr>
                </tbody>
              </table>

              <div className="flex justify-center gap-2 mt-6">
                <button onClick={() => setStep(1)}
                  className="border border-kb-border px-10 py-2.5 text-[13px] text-kb-text-body hover:bg-kb-beige-light">
                  이전
                </button>
                <button
                  onClick={handleConfirm}
                  className="px-14 py-2.5 text-[14px] font-bold text-white hover:opacity-90"
                  style={{ backgroundColor: '#5BC9A8' }}>
                  해지
                </button>
              </div>
            </>
          )}

          {/* STEP 3 */}
          {step === 3 && (
            <div className="border border-kb-border py-16 text-center">
              <p className="text-[16px] font-bold text-kb-text mb-3">해지가 완료되었습니다.</p>
              <p className="text-[13px] text-kb-text-muted mb-6">해지 결과는 해지결과/내역 조회에서 확인하실 수 있습니다.</p>
              <Link href="/products/deposit/inquiry/terminate-result"
                className="inline-block border border-kb-border px-8 py-2.5 text-[13px] text-kb-text-body hover:bg-kb-beige-light mr-2">
                해지결과 조회
              </Link>
              <Link href="/inquiry/accounts"
                className="inline-block bg-kb-yellow px-8 py-2.5 text-[13px] font-bold text-kb-text hover:bg-kb-yellow-dark">
                계좌 조회
              </Link>
            </div>
          )}
        </main>
      </div>
    </div>
  )
}
