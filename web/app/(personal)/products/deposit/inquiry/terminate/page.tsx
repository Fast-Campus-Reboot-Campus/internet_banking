'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import { useSearchParams } from 'next/navigation'
import { Suspense } from 'react'
import DepositSidebar from '@/components/products/DepositSidebar'
import { formatNumber } from '@/lib/mock-data'
import {
  DepositViewAccount,
  fetchDepositAccountViewModels,
  getCurrentDepositCustomerId,
  terminateDepositContract,
} from '@/lib/deposit-api'
import { executeChatbotFeature } from '@/lib/consultation-api'

const STEPS = ['1. 계좌조회/선택', '2. 해지계좌확인/정보입력', '3', '4', '+']

type Step = 1 | 2 | 3

const PIN_PAD = [
  [5, 2, 7],
  [9, 8, 0],
  [6, 1, 4],
  ['↺', 3, '✕'],
]

function DepositTerminateContent() {
  const searchParams = useSearchParams()
  const preselectedId = searchParams.get('accountId')
  const [step, setStep] = useState<Step>(1)
  const [selected, setSelected] = useState<DepositViewAccount | null>(null)
  const [joinedAccounts, setJoinedAccounts] = useState<DepositViewAccount[]>([])
  const [password, setPassword] = useState('')
  const [mouseInput, setMouseInput] = useState(false)
  const [depositNo, setDepositNo] = useState('')
  const [receiveMethod, setReceiveMethod] = useState<'own' | 'other' | 'cash'>('own')
  const [otherBank, setOtherBank] = useState('')
  const [otherAccount, setOtherAccount] = useState('')
  const [showCertModal, setShowCertModal] = useState(false)
  const [certStep, setCertStep] = useState<'info' | 'pin'>('info')
  const [pin, setPin] = useState<number[]>([])
  const [cardInput1, setCardInput1] = useState('')
  const [cardInput2, setCardInput2] = useState('')
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
      // deposit API 우선, 실패 시 consultation service fallback
      try {
        const apiAccounts = await fetchDepositAccountViewModels(getCurrentDepositCustomerId())
        if (!cancelled && apiAccounts.length > 0) { setJoinedAccounts(apiAccounts); return }
      } catch {}
      try {
        const customerId = localStorage.getItem('customerId') || getCurrentDepositCustomerId()
        const result = await executeChatbotFeature('MY_ACCOUNTS', { customer_no: customerId })
        if (!cancelled && result.data?.length > 0) {
          const rows = result.data.map((r: Record<string, unknown>) => ({
            id: String(r.account_id ?? r.id ?? ''),
            apiAccountId: Number(r.account_id ?? 0),
            number: String(r.account_number ?? ''),
            name: String(r.product_name ?? ''),
            type: String(r.product_type ?? ''),
            balance: Number(r.balance ?? 0),
            availableBalance: Number(r.balance ?? 0),
            accountStatus: String(r.account_status ?? 'ACTIVE'),
            maturityDate: r.maturity_at ? String(r.maturity_at) : undefined,
            createdAt: r.started_at ? String(r.started_at) : undefined,
          } as DepositViewAccount))
          setJoinedAccounts(rows)
          return
        }
      } catch {}
      if (!cancelled) setJoinedAccounts(fallbackAccounts)
    }

    async function init() {
      await loadAccounts()
    }
    init()

    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (!preselectedId || joinedAccounts.length === 0) return
    const acc = joinedAccounts.find(a => String(a.apiAccountId) === preselectedId || a.id === preselectedId)
    if (acc) { setSelected(acc); setStep(2) }
  }, [joinedAccounts, preselectedId])

  const allAccounts: DepositViewAccount[] = joinedAccounts
  const installmentAccounts = allAccounts.filter(a => a.type === '적금' || a.type === '청약')
  const pureDepositAccounts = allAccounts.filter(a => a.type === '예금')
  const checkingAccounts    = allAccounts.filter(a => a.type === '입출금')
  const receivableCheckingAccounts = checkingAccounts.filter(a => a.id !== selected?.id)

  function handleSelect(acc: DepositViewAccount) {
    setSelected(acc)
    setStep(2)
  }

  async function executeTermination() {
    if (selected) {
      try {
        const targetAccount = receiveMethod === 'own'
          ? joinedAccounts.find(a => a.id === depositNo)
          : undefined
        if (selected.contractId) {
          await terminateDepositContract(
            selected.contractId,
            'ONLINE_TERMINATION',
            targetAccount?.apiAccountId
          )
        }
        const terminatedBalance = selected.balance
        let updated = joinedAccounts.filter(a => a.id !== selected.id)

        if (receiveMethod === 'own' && targetAccount) {
          updated = updated.map(a => a.id === depositNo
            ? { ...a, balance: a.balance + terminatedBalance, availableBalance: a.availableBalance + terminatedBalance }
            : a
          )
        }

        localStorage.setItem('joinedAccounts', JSON.stringify(updated))
        setJoinedAccounts(updated)
      } catch {}
    }
    setStep(3)
  }

  function handleConfirm() {
    if (!password && !mouseInput) { alert('해지계좌 비밀번호를 입력해주세요.'); return }
    if (receiveMethod === 'own' && !depositNo) { alert('입금계좌를 선택해주세요.'); return }
    if (receiveMethod === 'other' && (!otherBank || !otherAccount)) { alert('은행명과 계좌번호를 입력해주세요.'); return }
    if (!cardInput1 || !cardInput2) { alert('보안카드 번호를 입력해주세요.'); return }
    setShowCertModal(true)
    setCertStep('info')
    setPin([])
  }

  function handlePinKey(key: number | string) {
    if (key === '↺') { setPin([]); return }
    if (key === '✕') { setPin(p => p.slice(0, -1)); return }
    if (typeof key === 'number' && pin.length < 6) {
      const next = [...pin, key]
      setPin(next)
      if (next.length === 6) {
        setTimeout(async () => {
          setShowCertModal(false)
          await executeTermination()
        }, 400)
      }
    }
  }

  const stepBtnCls = (i: number) =>
    i + 1 === step
      ? 'px-4 py-1.5 text-[12px] font-bold text-white'
      : 'px-4 py-1.5 text-[12px] text-kb-text-body border border-kb-border bg-white hover:bg-kb-beige-light'

  const targetAccount = receiveMethod === 'own'
    ? joinedAccounts.find(a => a.id === depositNo)
    : undefined
  const receiveMethodText =
    receiveMethod === 'cash'
      ? '현금 수령'
      : receiveMethod === 'own'
        ? '당행 계좌 입금'
        : '타행 계좌 입금'
  const receiveTargetText =
    receiveMethod === 'cash'
      ? '영업점 현금 수령'
      : receiveMethod === 'own'
        ? targetAccount?.number || '-'
        : `${otherBank || '-'} ${otherAccount || ''}`.trim()

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
              선택
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
                    <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text">입금방식</td>
                    <td className="border border-kb-border px-4 py-3">
                      <div className="flex gap-4 text-[13px]">
                        <label className="flex items-center gap-1.5 cursor-pointer">
                          <input type="radio" name="receiveMethod" value="own"
                            checked={receiveMethod === 'own'} onChange={() => { setReceiveMethod('own'); setOtherBank(''); setOtherAccount('') }} />
                          당행 계좌 입금
                        </label>
                        <label className="flex items-center gap-1.5 cursor-pointer">
                          <input type="radio" name="receiveMethod" value="other"
                            checked={receiveMethod === 'other'} onChange={() => { setReceiveMethod('other'); setDepositNo('') }} />
                          타행 계좌 입금
                        </label>
                        <label className="flex items-center gap-1.5 cursor-pointer">
                          <input type="radio" name="receiveMethod" value="cash"
                            checked={receiveMethod === 'cash'} onChange={() => { setReceiveMethod('cash'); setDepositNo(''); setOtherBank(''); setOtherAccount('') }} />
                          현금 수령
                        </label>
                      </div>
                    </td>
                  </tr>
                  {receiveMethod === 'own' && (
                  <tr>
                    <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text">입금계좌번호</td>
                    <td className="border border-kb-border px-4 py-3">
                      <select value={depositNo} onChange={e => setDepositNo(e.target.value)}
                        className="border border-kb-border px-3 py-1.5 text-[13px] outline-none bg-white">
                        <option value="">-선택-</option>
                        {receivableCheckingAccounts.map(a => (
                          <option key={a.id} value={a.id}>{a.number} ({a.name})</option>
                        ))}
                      </select>
                    </td>
                  </tr>
                  )}
                  {receiveMethod === 'other' && (
                  <tr>
                    <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text">타행 입금계좌</td>
                    <td className="border border-kb-border px-4 py-3">
                      <div className="flex gap-2">
                        <select
                          value={otherBank}
                          onChange={e => setOtherBank(e.target.value)}
                          className="border border-kb-border px-3 py-1.5 text-[13px] w-36 outline-none bg-white"
                        >
                          <option value="">은행 선택</option>
                          {['국민은행','신한은행','우리은행','하나은행','농협은행','기업은행','SC제일은행','한국씨티은행','카카오뱅크','케이뱅크','토스뱅크','우체국','새마을금고','신협','수협은행','대구은행','부산은행','광주은행','전북은행','경남은행','제주은행'].map(b => (
                            <option key={b} value={b}>{b}</option>
                          ))}
                        </select>
                        <input
                          type="text"
                          placeholder="계좌번호 입력"
                          value={otherAccount}
                          onChange={e => setOtherAccount(e.target.value)}
                          className="border border-kb-border px-3 py-1.5 text-[13px] flex-1 outline-none"
                        />
                      </div>
                    </td>
                  </tr>
                  )}
                  {receiveMethod === 'cash' && (
                  <tr>
                    <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text">수령방법</td>
                    <td className="border border-kb-border px-4 py-3 text-kb-text-body">
                      현금으로 수령합니다.
                    </td>
                  </tr>
                  )}
                </tbody>
              </table>

              {/* 보안카드 입력 */}
              <div className="border border-kb-border-dark rounded-xl p-6 mt-5">
                <p className="text-[14px] font-bold text-kb-text mb-4">보안매체 비밀번호 입력</p>
                <div className="flex items-start gap-8">
                  <div className="space-y-3">
                    <div className="flex items-center gap-3 text-[13px]">
                      <span className="text-gray-400">●●</span>
                      <input type="text" maxLength={2} value={cardInput1}
                        onChange={e => setCardInput1(e.target.value.replace(/\D/g, ''))}
                        className="border border-kb-border w-16 px-2 py-1 text-center text-[13px]" />
                      <span className="text-kb-text-muted">[33] 앞의 두자리</span>
                    </div>
                    <div className="flex items-center gap-3 text-[13px]">
                      <span className="text-gray-400">●●</span>
                      <input type="text" maxLength={2} value={cardInput2}
                        onChange={e => setCardInput2(e.target.value.replace(/\D/g, ''))}
                        className="border border-kb-border w-16 px-2 py-1 text-center text-[13px]" />
                      <span className="text-kb-text-muted">[10] 뒤의 두자리</span>
                    </div>
                  </div>
                  <div className="border border-gray-300 p-3 text-[11px]" style={{ minWidth: 260 }}>
                    <div className="flex justify-between items-center mb-2 pb-1 border-b border-gray-200">
                      <span className="font-bold text-kb-text">✱ AX풀뱅크</span>
                      <span className="text-kb-text-muted">Number. 0123456789</span>
                    </div>
                    <div className="grid grid-cols-5 gap-1 text-center text-[10px] text-gray-500">
                      {Array.from({length:35},(_,i)=>(
                        <div key={i} className="py-0.5">
                          <span className="mr-0.5 text-gray-400">{i+1}</span><span>•••••</span>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </div>

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

      {showCertModal && selected && (
        <div className="fixed inset-0 bg-black/50 z-[300] flex items-center justify-center">
          <div className="bg-white shadow-xl flex" style={{ width: 680, minHeight: 420 }}>
            <div className="w-[220px] bg-gray-50 border-r border-gray-200 flex flex-col items-center justify-center p-6">
              <div className="text-center mb-4">
                <div className="inline-block border-2 border-gray-300 rounded px-3 py-1 mb-2">
                  <span className="text-[15px] font-black tracking-wider text-gray-600">YESKEY</span>
                  <span className="text-[9px] text-gray-400 block">금융인증원</span>
                </div>
                <div className="border border-gray-300 rounded-full w-10 h-10 flex items-center justify-center mx-auto">
                  <span className="text-[10px] text-gray-500">TRUST<br/>CA</span>
                </div>
              </div>
            </div>

            <div className="flex-1 flex flex-col">
              <div className="flex items-center justify-between px-5 py-3 border-b border-gray-200 bg-gray-50">
                <span className="text-[13px] font-bold text-kb-text-muted">금융인증서비스</span>
                <button onClick={() => setShowCertModal(false)} className="text-gray-400 hover:text-gray-600">✕</button>
              </div>

              <div className="flex-1 p-6">
                {certStep === 'info' ? (
                  <div>
                    <p className="text-[13px] font-bold text-kb-text mb-4">전자서명 원문</p>
                    <div className="bg-gray-50 p-4 text-[13px] text-kb-text-body space-y-1 border border-gray-200 mb-6">
                      <p>거래종류 : 예금/적금 해지</p>
                      <p>해지계좌번호 : {selected.number}</p>
                      <p>해지계좌명 : {selected.name}</p>
                      <p>해지금액 : {formatNumber(selected.balance)}원</p>
                      <p>입금방식 : {receiveMethodText}</p>
                      <p>입금/수령정보 : {receiveTargetText}</p>
                    </div>
                    <div className="flex justify-center">
                      <button
                        onClick={() => setCertStep('pin')}
                        className="bg-kb-yellow px-16 py-3 text-[14px] font-bold text-kb-text hover:brightness-95"
                      >
                        확인
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="flex flex-col items-center">
                    <p className="text-[13px] text-kb-blue mb-1">홍길동님의 금융인증서</p>
                    <p className="text-[16px] font-bold text-kb-text mb-5">비밀번호를 입력해주세요</p>
                    <div className="flex gap-2 mb-6">
                      {Array.from({length:6}).map((_, i) => (
                        <div key={i}
                          className={`w-8 h-8 rounded-full border-2 flex items-center justify-center ${
                            i < pin.length ? 'bg-kb-text border-kb-text' : 'border-gray-300'
                          }`}>
                          {i < pin.length && <span className="w-2 h-2 bg-white rounded-full" />}
                        </div>
                      ))}
                    </div>
                    <Link href="#" className="text-[12px] text-kb-blue underline mb-6">비밀번호를 잊으셨나요?</Link>
                    <div className="grid grid-cols-3 gap-3 w-48">
                      {PIN_PAD.map((row, ri) =>
                        row.map((key, ci) => (
                          <button
                            key={`${ri}-${ci}`}
                            onClick={() => handlePinKey(key)}
                            className={`h-10 text-[16px] font-semibold rounded hover:bg-kb-beige transition-colors ${
                              typeof key === 'string' ? 'text-gray-400' : 'text-kb-text'
                            }`}
                          >
                            {key}
                          </button>
                        ))
                      )}
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default function DepositTerminatePage() {
  return (
    <Suspense>
      <DepositTerminateContent />
    </Suspense>
  )
}
