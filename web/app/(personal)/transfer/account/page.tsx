'use client'

import Link from 'next/link'
import { useState, useEffect } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { MOCK_BANKS, formatNumber } from '@/lib/mock-data'
import TransferSidebar from '@/components/inquiry/TransferSidebar'
import { fetchDepositAccountViewModels, getCurrentDepositCustomerId, DepositViewAccount, fetchTransactions, DepositTransaction } from '@/lib/deposit-api'

const AMOUNT_SHORTCUTS = ['100만', '50만', '10만', '5만', '1만', '전액', '정결']

export default function TransferAccountPage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const requestedFromAccount = searchParams.get('from') || ''
  const [activeRecipientTab, setActiveRecipientTab] = useState('최근입금계좌')
  const [fromAccount, setFromAccount] = useState('')
  const [toBank, setToBank] = useState('AXful')
  const [toBankCode, setToBankCode] = useState('KB')
  const [toAccount, setToAccount] = useState('')
  const [amount, setAmount] = useState('')
  const [password, setPassword] = useState('')
  const [showBankModal, setShowBankModal] = useState(false)
  const [bankTab, setBankTab] = useState('은행')
  const [mouseInput, setMouseInput] = useState(false)
  const [accounts, setAccounts] = useState<DepositViewAccount[]>([])
  const [recentAccounts, setRecentAccounts] = useState<{ bank: string; name: string; number: string }[]>([])
  const [validationMessage, setValidationMessage] = useState('')
  const [innerBankTab, setInnerBankTab] = useState<'own' | 'other'>('own')

  // 계좌 목록 로딩 — 마운트 시 1회 또는 requestedFromAccount 변경 시만 실행
  useEffect(() => {
    async function loadAccounts() {
      let loadedAccounts: DepositViewAccount[] = []
      let fallbackAccounts: DepositViewAccount[] = []
      try {
        const raw = localStorage.getItem('joinedAccounts')
        if (raw) fallbackAccounts = JSON.parse(raw) as DepositViewAccount[]
      } catch {}

      try {
        const customerId = getCurrentDepositCustomerId()
        const accs = await fetchDepositAccountViewModels(customerId)
        loadedAccounts = accs.length > 0 ? accs : fallbackAccounts
      } catch {
        loadedAccounts = fallbackAccounts
      }

      setAccounts(loadedAccounts)

      // 출금 계좌 초기 선택 — 이미 선택된 경우 변경하지 않음
      setFromAccount(prev => {
        if (prev) return prev
        if (requestedFromAccount) {
          const requested = loadedAccounts.find(a =>
            a.id === requestedFromAccount ||
            a.number === requestedFromAccount ||
            String(a.apiAccountId) === requestedFromAccount
          )
          if (requested && requested.isWithdrawable !== false) return requested.id
        }
        const first = loadedAccounts.find(a =>
          a.type !== '적금' &&
          a.type !== '청약' &&
          a.isWithdrawable === true &&
          a.accountStatus !== 'CLOSED'
        ) ?? loadedAccounts[0]
        return first?.id ?? ''
      })
    }
    loadAccounts()
  }, [requestedFromAccount])

  // 최근 이체 내역 로딩 — 출금 계좌 변경 시 실행
  useEffect(() => {
    if (!fromAccount || accounts.length === 0) return
    async function loadRecent() {
      try {
        const sourceAcc = accounts.find(a => a.id === fromAccount) ?? accounts[0]
        const accId = sourceAcc?.apiAccountId
        if (!accId) return
        const txs = await fetchTransactions({ accountId: accId })
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
      } catch {}
    }
    loadRecent()
  }, [fromAccount, accounts])

  const transferableAccounts = accounts.filter(a =>
    a.type === '입출금' &&
    a.isWithdrawable !== false &&
    a.accountStatus !== 'CLOSED'
  )
  const fromAcc = transferableAccounts.find(a => a.id === fromAccount)
    ?? accounts.find(a => a.number === requestedFromAccount || a.id === requestedFromAccount || String(a.apiAccountId) === requestedFromAccount)
    ?? (requestedFromAccount ? undefined : transferableAccounts[0])
  const isFromAccountLocked = Boolean(requestedFromAccount)
  const withdrawalAccounts =
    isFromAccountLocked && fromAcc
      ? [fromAcc]
      : transferableAccounts

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
    setToBankCode(MOCK_BANKS.find(bank => bank.name === acc.bank)?.code ?? 'KB')
    setToAccount(acc.number)
  }

  function handleConfirm() {
    const amountNum = parseInt(amount.replace(/,/g, '')) || 0
    setValidationMessage('')
    if (!fromAcc) {
      setValidationMessage('출금계좌를 선택해 주세요.')
      return
    }
    if (!toAccount) {
      setValidationMessage('입금계좌번호를 입력해 주세요.')
      return
    }
    if (amountNum === 0) {
      setValidationMessage('이체금액을 입력해 주세요.')
      return
    }
    if (amountNum > fromAcc.availableBalance) {
      setValidationMessage('이체금액이 출금가능금액을 초과했습니다.')
      return
    }
    const targetAccount = innerBankTab === 'own'
      ? accounts.find(acc => acc.number === toAccount)
      : undefined
    const receiverName = targetAccount?.name ?? recentAccounts.find(r => r.number === toAccount)?.name ?? '수취인'
    const data = {
      fromAccountId: fromAcc?.apiAccountId,
      fromAccountViewId: fromAcc?.id ?? '',
      fromNumber: fromAcc?.number ?? '',
      fromName: fromAcc?.name ?? '',
      toAccountId: targetAccount?.apiAccountId,
      toBank,
      toBankCode,
      toAccount,
      amount: amountNum,
      receiverName,
      fee: 0,
      transferType: targetAccount ? 'INTERNAL' : 'EXTERNAL',
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
              {['최근입금계좌', '자주쓰는계좌', '단축이체'].map(tab => (
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
                      {isFromAccountLocked && fromAcc ? (
                        <input
                          type="text"
                          readOnly
                          value={`${fromAcc.number} (${fromAcc.name}) - ${fromAcc.balance.toLocaleString()}원`}
                          className="border border-kb-border px-3 py-1.5 text-[13px] w-[280px] bg-kb-beige-light"
                        />
                      ) : (
                        <select
                          value={fromAccount}
                          onChange={e => setFromAccount(e.target.value)}
                          className="border border-kb-border px-3 py-1.5 text-[13px] w-[280px]"
                        >
                          {withdrawalAccounts.map(a => (
                            <option key={a.id} value={a.id}>{a.number} ({a.name}) - {a.balance.toLocaleString()}원</option>
                          ))}
                        </select>
                      )}
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
                  <td className="bg-kb-beige-light px-4 py-3 font-semibold text-kb-text whitespace-nowrap align-top pt-4">입금계좌</td>
                  <td className="px-4 py-3">
                    {/* 당행 / 타행 토글 */}
                    <div className="flex border border-kb-border rounded overflow-hidden w-fit mb-3">
                      <button
                        type="button"
                        onClick={() => { setInnerBankTab('own'); setToBank('AXful'); setToBankCode('KB'); setToAccount('') }}
                        className={`px-5 py-1.5 text-[13px] font-bold transition ${innerBankTab === 'own' ? 'bg-kb-text text-white' : 'bg-white text-kb-text-muted hover:bg-kb-beige-light'}`}
                      >
                        당행
                      </button>
                      <button
                        type="button"
                        onClick={() => { setInnerBankTab('other'); setToBank(''); setToBankCode(''); setToAccount('') }}
                        className={`px-5 py-1.5 text-[13px] font-bold transition ${innerBankTab === 'other' ? 'bg-kb-text text-white' : 'bg-white text-kb-text-muted hover:bg-kb-beige-light'}`}
                      >
                        타행
                      </button>
                    </div>

                    {/* 당행: 내 계좌 드롭다운 */}
                    {innerBankTab === 'own' && (
                      <div className="flex items-center gap-2">
                        <select
                          value={toAccount}
                          onChange={e => { setToBank('AXful'); setToBankCode('KB'); setToAccount(e.target.value) }}
                          className="border border-kb-border px-3 py-1.5 text-[13px] w-[300px] outline-none bg-white"
                        >
                          <option value="">계좌 선택</option>
                          {accounts.filter(acc => acc.id !== fromAcc?.id).map(acc => (
                            <option key={acc.id} value={acc.number}>
                              {acc.name} — {acc.number} ({acc.balance.toLocaleString()}원)
                            </option>
                          ))}
                        </select>
                      </div>
                    )}

                    {/* 타행: 은행 선택 + 계좌번호 입력 */}
                    {innerBankTab === 'other' && (
                      <div className="flex items-center gap-2">
                        <button
                          type="button"
                          onClick={() => setShowBankModal(true)}
                          className="border border-kb-border px-3 py-1.5 text-[13px] w-28 text-left hover:bg-kb-beige-light"
                        >
                          {toBank || '은행 선택'}
                        </button>
                        <input
                          type="text"
                          value={toAccount}
                          onChange={e => setToAccount(e.target.value)}
                          placeholder="계좌번호 입력"
                          className="border border-kb-border px-3 py-1.5 text-[13px] w-52 outline-none"
                        />
                        <button className="border border-kb-border px-3 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light">
                          사기의심계좌여부 조회
                        </button>
                      </div>
                    )}
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
            <div className="flex flex-col items-center gap-3">
              {validationMessage && (
                <p className="text-[13px] font-medium text-kb-red">{validationMessage}</p>
              )}
              <button
                onClick={handleConfirm}
                className="bg-kb-yellow px-24 py-3 text-[15px] font-bold text-kb-text hover:brightness-95"
              >
                확인
              </button>
            </div>
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
                    onClick={() => { setToBank(bank.name); setToBankCode(bank.code); setShowBankModal(false) }}
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
