'use client'

import Link from 'next/link'
import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { MOCK_BANKS, formatNumber } from '@/lib/mock-data'
import TransferSidebar from '@/components/inquiry/TransferSidebar'
import { fetchDepositAccountViewModels, getCurrentDepositCustomerId, DepositViewAccount, fetchTransactions, DepositTransaction } from '@/lib/deposit-api'

const AMOUNT_SHORTCUTS = ['100만', '50만', '10만', '5만', '1만', '전액', '정결']

export default function TransferAccountPage() {
  const router = useRouter()
  const [activeRecipientTab, setActiveRecipientTab] = useState('최근입금계좌')
  const [fromAccount, setFromAccount] = useState('')
  const [toBank, setToBank] = useState('AXful')
  const [toAccount, setToAccount] = useState('')
  const [amount, setAmount] = useState('')
  const [password, setPassword] = useState('')
  const [showBankModal, setShowBankModal] = useState(false)
  const [bankTab, setBankTab] = useState('은행')
  const [mouseInput, setMouseInput] = useState(false)
  const [accounts, setAccounts] = useState<DepositViewAccount[]>([])
  const [recentAccounts, setRecentAccounts] = useState<{ bank: string; name: string; number: string }[]>([])

  useEffect(() => {
    async function loadAccounts() {
      try {
        const customerId = getCurrentDepositCustomerId()
        const accs = await fetchDepositAccountViewModels(customerId)
        setAccounts(accs)
        if (accs.length > 0 && !fromAccount) {
          setFromAccount(accs[0].id)
        }
        const txs = await fetchTransactions({ customerId })
        const seen = new Set<string>()
        const recent = txs
          .filter((t: DepositTransaction) => t.transactionType === 'TRANSFER' && t.directionType === 'OUT' && t.counterpartyAccountNo)
          .reduce((acc: { bank: string; name: string; number: string }[], t: DepositTransaction) => {
            if (!seen.has(t.counterpartyAccountNo!)) {
              seen.add(t.counterpartyAccountNo!)
              acc.push({ bank: t.counterpartyBankName || 'AXful', name: t.counterpartyName || '수취인', number: t.counterpartyAccountNo! })
            }
            return acc
          }, [])
          .slice(0, 5)
        setRecentAccounts(recent)
      } catch {
        setAccounts([])
      }
    }
    loadAccounts()
  }, [])

  const fromAcc = accounts.find(a => a.id === fromAccount) ?? accounts[0]

  function handleAmountShortcut(label: string) {
    const map: Record<string, number> = {
      '100만': 1_000_000, '50만': 500_000, '10만': 100_000,
      '5만': 50_000, '1만': 10_000,
    }
    if (label === '전액') {
      setAmount(String(fromAcc?.availableBalance ?? 0))
    } else if (label === '정결') {
      const n = parseInt(amount.replace(/,/g, '')) || 0
      const rounded = Math.floor(n / 10000) * 10000
      setAmount(String(rounded))
    } else if (map[label]) {
      const prev = parseInt(amount.replace(/,/g, '')) || 0
      setAmount(String(prev + map[label]))
    }
  }

  function handleSelectRecent(acc: { bank: string; name: string; number: string }) {
    setToBank(acc.bank)
    setToAccount(acc.number)
  }

  function handleConfirm() {
    const amountNum = parseInt(amount.replace(/,/g, '')) || 0
    if (!toAccount || amountNum === 0) return
    const receiverName = recentAccounts.find(r => r.number === toAccount)?.name ?? '수취인'
    const data = {
      fromNumber: fromAcc?.number ?? '',
      fromName: fromAcc?.name ?? '',
      toBank,
      toAccount,
      amount: amountNum,
      receiverName,
      fee: 0,
    }
    sessionStorage.setItem('pendingTransfer', JSON.stringify(data))
    router.push('/transfer/confirm')
  }

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
            <span className="font-semibold text-kb-text">계좌이체</span>
            <span className="ml-2 text-kb-blue cursor-pointer">? 도움말</span>
          </div>

          <h1 className="text-[20px] font-bold text-kb-text mb-5">계좌이체</h1>

          {/* STEP 표시 */}
          <div className="mb-4">
            <div className="flex items-center gap-2 mb-4">
              <span className="text-[14px] font-bold text-kb-text border-b-2 border-kb-text pb-1">STEP 1. 이체정보 입력</span>
            </div>
          </div>

          {/* 최근입금계좌 탭 영역 */}
          <div className="border border-kb-border-dark rounded-xl mb-5 overflow-hidden">
            <div className="flex border-b border-kb-border">
              {['최근입금계좌', '자주쓰는계좌', '내계좌', '단축이체'].map(tab => (
                <button
                  key={tab}
                  onClick={() => setActiveRecipientTab(tab)}
                  className={`px-5 py-2.5 text-[13px] ${
                    activeRecipientTab === tab
                      ? 'bg-white font-bold text-kb-text border-b-2 border-kb-text -mb-px'
                      : 'bg-kb-beige-light text-kb-text-muted hover:text-kb-text'
                  }`}
                >
                  {tab}
                </button>
              ))}
            </div>
            {/* 최근입금계좌 */}
            {activeRecipientTab === '최근입금계좌' && (
              <div className="p-4">
                <p className="text-[12px] text-kb-text-muted mb-3">* 최근 3개월 이내 이체된 입금계좌입니다.</p>
                <div className="flex items-center gap-2 mb-3">
                  <div className="flex-1 border border-kb-border flex items-center px-3 py-2">
                    <input
                      type="text"
                      placeholder="이름, 은행, 계좌번호 등으로 검색"
                      className="flex-1 text-[13px] outline-none text-kb-text-muted"
                    />
                    <span className="text-kb-text-muted cursor-pointer">✕</span>
                  </div>
                  <button className="border border-kb-border px-4 py-2 text-[13px] text-kb-text-body hover:bg-kb-beige-light">
                    전체보기
                  </button>
                </div>
                <div className="flex gap-3 flex-wrap">
                  {recentAccounts.length === 0 ? <p className="text-[12px] text-kb-text-muted py-4 text-center">최근 이체 내역이 없습니다.</p> : recentAccounts.map(acc => (
                    <button
                      key={acc.number}
                      onClick={() => handleSelectRecent(acc)}
                      className={`border px-4 py-3 text-left text-[12px] hover:border-kb-yellow transition-colors ${
                        toAccount === acc.number ? 'border-kb-yellow bg-yellow-50' : 'border-kb-border'
                      }`}
                      style={{ minWidth: 160 }}
                    >
                      <p className="font-semibold text-kb-text">{acc.name}</p>
                      <p className="text-kb-text-muted">{acc.bank}</p>
                      <p className="text-kb-text-muted">{acc.number}</p>
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* 자주쓰는계좌 */}
            {activeRecipientTab === '자주쓰는계좌' && (
              <div className="p-8 flex flex-col items-center gap-3">
                <p className="text-[13px] text-kb-text-muted">자주쓰는계좌가 등록되어 있지 않습니다.</p>
                <p className="text-[12px] text-kb-text-muted">(대출, 펀드, ISA 등의 계좌는 표시되지 않습니다.)</p>
                <button className="mt-2 border border-kb-border px-6 py-2 text-[12px] text-kb-text-body hover:bg-kb-beige-light">
                  자주쓰는계좌 등록/삭제
                </button>
              </div>
            )}

            {/* 내계좌 */}
            {activeRecipientTab === '내계좌' && (
              <div className="p-4">
                <p className="text-[12px] mb-4" style={{ color: '#2563EB' }}>
                  * AX풀뱅크 내 계좌로 이체 시 계좌비밀번호, 보안매체, 인증서, 추가인증 없이 이체 가능합니다.
                </p>
                <div className="flex gap-3 flex-wrap">
                  {accounts.map(acc => (
                    <button
                      key={acc.id}
                      onClick={() => { setToBank('AXful'); setToAccount(acc.number) }}
                      className={`border px-4 py-3 text-left text-[12px] hover:border-kb-yellow transition-colors ${
                        toAccount === acc.number ? 'border-kb-yellow bg-yellow-50' : 'border-kb-border'
                      }`}
                      style={{ minWidth: 160 }}
                    >
                      <p className="font-semibold text-[13px]" style={{ color: '#2563EB' }}>{acc.name}</p>
                      <p className="text-kb-text-muted mt-1">{acc.number}</p>
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* 단축이체 */}
            {activeRecipientTab === '단축이체' && (
              <div className="p-8 flex flex-col items-center gap-3">
                <p className="text-[13px] text-kb-text-muted">단축이체가 등록되어 있지 않습니다.</p>
                <button className="mt-2 border border-kb-border px-6 py-2 text-[12px] text-kb-text-body hover:bg-kb-beige-light">
                  단축이체 등록/삭제
                </button>
              </div>
            )}
          </div>

          {/* 이체 폼 */}
          <div className="border border-kb-border-dark rounded-xl mb-4 overflow-hidden">
            <table className="w-full text-[13px]">
              <tbody>
                <tr className="border-b border-kb-border">
                  <td className="bg-kb-beige-light px-4 py-3 font-semibold text-kb-text w-[140px] whitespace-nowrap">출금계좌번호</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <select
                        value={fromAccount}
                        onChange={e => setFromAccount(e.target.value)}
                        className="border border-kb-border px-3 py-1.5 text-[13px] w-[280px]"
                      >
                        {accounts.filter(a => a.availableBalance > 0 || a.balance > 0).map(a => (
                          <option key={a.id} value={a.id}>{a.number}</option>
                        ))}
                      </select>
                      <button className="border border-kb-border px-3 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light">
                        출금가능금액
                      </button>
                      <button className="border border-kb-border px-3 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light">
                        수수료조회
                      </button>
                    </div>
                  </td>
                </tr>
                <tr className="border-b border-kb-border">
                  <td className="bg-kb-beige-light px-4 py-3 font-semibold text-kb-text whitespace-nowrap">입금기관</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <input
                        type="text"
                        value={toBank}
                        readOnly
                        className="border border-kb-border px-3 py-1.5 text-[13px] w-28 bg-kb-beige-light"
                      />
                      <button
                        onClick={() => setShowBankModal(true)}
                        className="border border-kb-border px-3 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light"
                      >
                        기관선택
                      </button>
                    </div>
                  </td>
                </tr>
                <tr className="border-b border-kb-border">
                  <td className="bg-kb-beige-light px-4 py-3 font-semibold text-kb-text whitespace-nowrap">입금계좌번호</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <input
                        type="text"
                        value={toAccount}
                        onChange={e => setToAccount(e.target.value)}
                        placeholder="계좌번호 입력"
                        className="border border-kb-border px-3 py-1.5 text-[13px] w-52"
                      />
                      <button className="border border-kb-border px-3 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light">
                        사기의심계좌여부 조회
                      </button>
                    </div>
                  </td>
                </tr>
                <tr className="border-b border-kb-border">
                  <td className="bg-kb-beige-light px-4 py-3 font-semibold text-kb-text whitespace-nowrap align-top pt-4">이체금액</td>
                  <td className="px-4 py-3">
                    <div className="flex gap-1.5 mb-2 flex-wrap">
                      {AMOUNT_SHORTCUTS.map(s => (
                        <button key={s} onClick={() => handleAmountShortcut(s)}
                          className="border border-kb-border px-3 py-1 text-[12px] hover:bg-kb-beige-light">
                          {s}
                        </button>
                      ))}
                    </div>
                    <div className="flex items-center gap-2">
                      <input
                        type="text"
                        value={amount ? formatNumber(parseInt(amount.replace(/,/g,''))||0) : ''}
                        onChange={e => setAmount(e.target.value.replace(/,/g,''))}
                        placeholder=""
                        className="border border-kb-border px-3 py-1.5 text-[13px] w-52 text-right"
                      />
                      <span className="text-[13px]">원</span>
                      <button className="border border-kb-border px-3 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light">
                        📱 계산기
                      </button>
                    </div>
                  </td>
                </tr>
                <tr>
                  <td className="bg-kb-beige-light px-4 py-3 font-semibold text-kb-text whitespace-nowrap">계좌비밀번호</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-3">
                      <input
                        type="password"
                        value={password}
                        onChange={e => setPassword(e.target.value)}
                        maxLength={4}
                        className="border border-kb-border px-3 py-1.5 text-[13px] w-28"
                      />
                      <label className="flex items-center gap-1 text-[12px] text-kb-text-muted cursor-pointer">
                        <input type="checkbox" checked={mouseInput} onChange={e => setMouseInput(e.target.checked)} />
                        마우스로 입력
                      </label>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          {/* 선택/추가 정보 */}
          <div className="flex justify-between mb-6">
            <button className="flex items-center gap-1 text-[13px] text-kb-text-body hover:text-kb-text">
              선택정보입력 <span className="border border-kb-border px-1 text-[11px]">+</span>
            </button>
            <p className="text-[12px] text-kb-text-muted">* 내 통장 표시, 받는분 통장 표시, 받는분 예금주명</p>
          </div>
          <div className="flex justify-between mb-6">
            <button className="flex items-center gap-1 text-[13px] text-kb-text-body hover:text-kb-text">
              추가정보입력 <span className="border border-kb-border px-1 text-[11px]">+</span>
            </button>
            <p className="text-[12px] text-kb-text-muted">* 예약이체, 납부회사, CMS 증도금</p>
          </div>

          {/* 확인 버튼 */}
          <div className="flex justify-center mb-8">
            <button
              onClick={handleConfirm}
              className="bg-kb-yellow px-24 py-3 text-[15px] font-bold text-kb-text hover:brightness-95"
            >
              확인
            </button>
          </div>

          {/* 이체수수료 안내 */}
          <div className="border border-kb-border rounded-xl p-6 mb-4 text-[12px] text-kb-text-muted space-y-1">
            <p className="font-semibold text-kb-text-body mb-2">이체수수료 안내</p>
            <p>* AX풀뱅크 계좌로 이체할 경우에는 이체수수료가 부과되지 않습니다.</p>
            <p>* AXful클럽(VVIP,VIP,그랜드,베스트)고객은 타행이체 수수료가 면제됩니다.</p>
            <p>* 개인 및 개인사업자 고객님은 타행이체수수료가 면제됩니다.</p>
            <p>* 이체수수료 면제 대상 고객은 다음 화면에서 면제 여부를 확인하실 수 있습니다.</p>
            <p>* 출금계좌가 이체수수료 면제횟수가 있는 상품일 경우, 잔여 면제횟수 내에서 면제됩니다.</p>
            <p>* [수수료조회] 버튼을 누르면 수수료 면제 잔여횟수를 확인하실 수 있습니다.</p>
          </div>

          {/* 인터넷 사기피해 */}
          <div className="border border-kb-border p-4 flex items-start gap-4 text-[12px]">
            <div className="w-12 h-12 bg-orange-100 rounded-full flex items-center justify-center flex-shrink-0">
              <span className="text-2xl">📞</span>
            </div>
            <div>
              <p className="font-semibold text-kb-text-body mb-1">인터넷 사기피해 신고여부 확인</p>
              <p className="text-kb-text-muted">최근 3개월 간 경찰청에 3회 이상 신고접수된 인터넷 사기 계좌번호를 조회할 수 있습니다.</p>
              <Link href="#" className="text-kb-blue underline">바로가기</Link>
              <span className="text-kb-text-muted ml-4">제공 : 경찰청</span>
            </div>
          </div>
        </main>
      </div>

      {/* ===== 기관선택 모달 ===== */}
      {showBankModal && (
        <div className="fixed inset-0 bg-black/40 z-[200] flex items-center justify-center">
          <div className="bg-white w-[520px] shadow-xl">
            <div className="bg-[#3D4F47] text-white px-5 py-3 flex items-center justify-between">
              <span className="font-bold text-[15px]">기관선택</span>
              <button onClick={() => setShowBankModal(false)} className="text-white text-xl">✕</button>
            </div>
            <div className="p-5">
              <div className="flex border-b border-kb-border mb-4">
                {['은행', '증권사', '국세납부', '지방세입납부'].map(t => (
                  <button key={t} onClick={() => setBankTab(t)}
                    className={`px-5 py-2 text-[13px] border-b-2 -mb-px ${
                      bankTab === t ? 'border-kb-yellow font-bold text-kb-text' : 'border-transparent text-kb-text-muted'
                    }`}>
                    {t}
                  </button>
                ))}
              </div>
              <div className="grid grid-cols-3 gap-2">
                {MOCK_BANKS.map(bank => (
                  <button key={bank.code}
                    onClick={() => { setToBank(bank.name); setShowBankModal(false) }}
                    className="flex items-center gap-2 px-3 py-2 border border-kb-border text-[13px] text-kb-text-body hover:bg-kb-beige-light hover:border-kb-yellow text-left">
                    <span className="w-5 h-5 rounded-full bg-kb-beige-light flex-shrink-0" />
                    {bank.name}
                  </button>
                ))}
              </div>
              <div className="mt-5 flex justify-center">
                <button onClick={() => setShowBankModal(false)}
                  className="border border-kb-border px-12 py-2 text-[13px] text-kb-text-body hover:bg-kb-beige-light">
                  닫기
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
