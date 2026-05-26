'use client'

import Link from 'next/link'
import { useState } from 'react'
import DepositSidebar from '@/components/products/DepositSidebar'

const STEPS = ['1. 계좌조회/선택', '2. 해지계좌확인/정보입력', '3', '4', '+']

const INSTALLMENT_ACCOUNTS = [
  { no: '467307-04-023562', name: '주택청약종합저축', amount: '4,810,000', date: '2014.12.18', unit: '월/일' },
]

const FREE_ACCOUNTS = [
  { no: '531089-04-274618', name: 'AX풀뱅크ONE통장-보통예금', amount: '17,807', date: '2019.10.28' },
]

type Step = 1 | 2 | 3
type SelectedAccount = { no: string; name: string }

export default function DepositTerminatePage() {
  const [step, setStep] = useState<Step>(1)
  const [selected, setSelected] = useState<SelectedAccount | null>(null)
  const [password, setPassword] = useState('')
  const [mouseInput, setMouseInput] = useState(false)
  const [depositNo, setDepositNo] = useState('선택')
  const [freeOpen, setFreeOpen] = useState(true)
  const [installOpen, setInstallOpen] = useState(true)

  function handleTerminate(acc: SelectedAccount) {
    setSelected(acc)
    setStep(2)
  }

  function handleConfirm() {
    if (!password && !mouseInput) { alert('해지계좌 비밀번호를 입력해주세요.'); return }
    setStep(3)
  }

  const stepBtnCls = (i: number) =>
    i + 1 === step
      ? 'px-4 py-1.5 text-[12px] font-bold text-white'
      : 'px-4 py-1.5 text-[12px] text-kb-text-body border border-kb-border bg-white hover:bg-kb-beige-light'

  return (
    <div className="max-w-kb-container mx-auto px-6 py-6">
      <div className="flex justify-end mb-3 text-[12px] text-kb-text-muted gap-1 items-center">
        <span>개인뱅킹</span><span>›</span>
        <span>금융상품</span><span>›</span>
        <span>예금</span><span>›</span>
        <Link href="/products/deposit/inquiry/terminate" className="hover:underline">예금해지</Link>
        <span>›</span>
        <Link href="#" className="text-kb-blue hover:underline">도움말</Link>
      </div>

      <div className="flex gap-6">
        <DepositSidebar />

        <main className="flex-1 min-w-0">
          <div className="flex items-center justify-between mb-5">
            <h1 className="text-[20px] font-bold text-kb-text">예금해지</h1>
            <div className="flex gap-1">
              {STEPS.map((s, i) => (
                <button key={s} className={stepBtnCls(i)}
                  style={i + 1 === step ? { backgroundColor: '#5BC9A8' } : {}}>
                  {s}
                </button>
              ))}
            </div>
          </div>

          {/* STEP 1: 계좌조회/선택 */}
          {step === 1 && (
            <>
              <div className="border border-kb-border bg-[#FAFAFA] px-4 py-3 mb-5 text-[12px] text-kb-text-body space-y-1">
                {[
                  '인터넷으로 해지 가능한 예금은 오른쪽 상단의 [도움말]을 참조하시기 바랍니다.',
                  '해지예상조회를 이용하여 해지면 고객님의 해지계좌번호를 다시 한번 확인하여 선택하기 바랍니다.',
                  <span key="r" className="text-[#E05555]">정약관련예금(분탁정약접저예금·정약부금·정약금·정약)과 장기주택마련저축은 추가사항이 필요한 상품으로 인터넷뱅킹을 통한 해지가 제한됩니다. (창구를 통한 해지는 가능합니다.)</span>,
                ].map((n, i) => (
                  <p key={i} className="flex gap-1.5"><span className="flex-shrink-0">-</span><span>{n}</span></p>
                ))}
              </div>

              {/* 거치식 */}
              <div className="border border-kb-border mb-3">
                <button onClick={() => {}}
                  className="flex items-center justify-between w-full px-4 py-3 bg-[#FAFAFA] border-b border-kb-border">
                  <span className="text-[13px] font-bold text-kb-text">거치식 예금계좌</span>
                  <span className="text-[11px] border border-kb-border px-3 py-1 bg-white hover:bg-kb-beige-light text-kb-text-muted">－</span>
                </button>
                <div className="px-4 py-3 text-[13px] text-kb-text-muted">조회하실 내역이 없습니다</div>
              </div>

              {/* 적립식 */}
              <div className="border border-kb-border mb-3">
                <button onClick={() => setInstallOpen(v => !v)}
                  className="flex items-center justify-between w-full px-4 py-3 bg-[#FAFAFA] border-b border-kb-border">
                  <span className="text-[13px] font-bold text-kb-text">적립식 예금계좌</span>
                  <span className="text-[11px] border border-kb-border px-3 py-1 bg-white hover:bg-kb-beige-light text-kb-text-muted">{installOpen ? '－' : '＋'}</span>
                </button>
                {installOpen && (
                  <table className="w-full border-collapse text-[13px]">
                    <tbody>
                      {INSTALLMENT_ACCOUNTS.map(acc => (
                        <tr key={acc.no} className="border-t border-kb-border">
                          <td className="px-4 py-3 text-kb-text-body">
                            <p>{acc.no}</p>
                            <p className="text-[11px] text-kb-text-muted mt-0.5">신규일 {acc.date} | {acc.unit}</p>
                          </td>
                          <td className="px-4 py-3 text-kb-text-body">{acc.name}</td>
                          <td className="px-4 py-3 text-right text-kb-text-body font-medium">{acc.amount}원</td>
                          <td className="px-4 py-3 text-right">
                            <button className="border border-kb-border px-3 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light">
                              해지상세조회
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>

              {/* 입출금이자유로운 */}
              <div className="border border-kb-border mb-3">
                <button onClick={() => setFreeOpen(v => !v)}
                  className="flex items-center justify-between w-full px-4 py-3 bg-[#FAFAFA] border-b border-kb-border">
                  <span className="text-[13px] font-bold text-kb-text">입출금이자유로운 예금계좌</span>
                  <span className="text-[11px] border border-kb-border px-3 py-1 bg-white hover:bg-kb-beige-light text-kb-text-muted">{freeOpen ? '－' : '＋'}</span>
                </button>
                {freeOpen && (
                  <table className="w-full border-collapse text-[13px]">
                    <tbody>
                      {FREE_ACCOUNTS.map(acc => (
                        <tr key={acc.no} className="border-t border-kb-border">
                          <td className="px-4 py-3 text-kb-text-body">
                            <p>{acc.no}</p>
                            <p className="text-[11px] text-kb-text-muted mt-0.5">신규일 {acc.date}</p>
                          </td>
                          <td className="px-4 py-3 text-kb-text-body">{acc.name}</td>
                          <td className="px-4 py-3 text-right text-kb-text-body font-medium">{acc.amount}원</td>
                          <td className="px-4 py-3 text-right">
                            <div className="flex gap-1 justify-end">
                              <button
                                onClick={() => handleTerminate(acc)}
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
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </>
          )}

          {/* STEP 2: 해지계좌확인/정보입력 */}
          {step === 2 && selected && (
            <>
              <div className="border border-kb-border bg-[#FAFAFA] px-4 py-3 mb-5 text-[12px] text-kb-text-body">
                <p className="flex gap-1.5"><span>-</span><span>입출금이자유로운예금 해지 시 당 본인계좌로 입금되며 입금될 계좌가 없거나 각종 자동이체 및 카드 결제과 등 연 결계좌로 사용되고 있을 경우 해지가 제한됩니다.(창구를 통한 해지는 가능합니다.)</span></p>
              </div>

              <table className="w-full border-collapse text-[13px] border-t-2 border-kb-text">
                <tbody>
                  <tr>
                    <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text w-[140px] whitespace-nowrap">해지계좌번호</td>
                    <td className="border border-kb-border px-4 py-3 text-kb-text-body">{selected.no}</td>
                  </tr>
                  <tr>
                    <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text">해지계좌명</td>
                    <td className="border border-kb-border px-4 py-3 text-kb-text-body">{selected.name}</td>
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
                        <option>-선택-</option>
                        <option>531089-04-274618 (AX풀뱅크ONE통장)</option>
                      </select>
                    </td>
                  </tr>
                </tbody>
              </table>

              <div className="flex justify-center mt-6">
                <button
                  onClick={handleConfirm}
                  className="px-14 py-2.5 text-[14px] font-bold text-white hover:opacity-90"
                  style={{ backgroundColor: '#5BC9A8' }}>
                  해지
                </button>
              </div>
            </>
          )}

          {/* STEP 3: 완료 */}
          {step === 3 && (
            <div className="border border-kb-border py-16 text-center">
              <p className="text-[16px] font-bold text-kb-text mb-3">예금 해지가 완료되었습니다.</p>
              <p className="text-[13px] text-kb-text-muted mb-6">해지 결과는 해지결과/내역 조회에서 확인하실 수 있습니다.</p>
              <Link href="/products/deposit/inquiry/terminate-result"
                className="inline-block border border-kb-border px-8 py-2.5 text-[13px] text-kb-text-body hover:bg-kb-beige-light mr-2">
                해지결과 조회
              </Link>
              <Link href="/products/deposit"
                className="inline-block bg-kb-yellow px-8 py-2.5 text-[13px] font-bold text-kb-text hover:bg-kb-yellow-dark">
                예금 상품 목록
              </Link>
            </div>
          )}
        </main>
      </div>
    </div>
  )
}
