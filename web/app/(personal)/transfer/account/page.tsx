'use client'

import { useState, useEffect } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { MOCK_BANKS, formatNumber } from '@/lib/mock-data'
import TransferSidebar from '@/components/inquiry/TransferSidebar'
import { fetchDepositAccountViewModels, getCurrentDepositCustomerId, DepositViewAccount, fetchTransactions, DepositTransaction } from '@/lib/deposit-api'

const AMOUNT_SHORTCUTS = ['100만', '50만', '10만', '5만', '1만', '전액', '정결']

const labelCell = "px-4 py-3 text-[13px] font-semibold text-kb-text whitespace-nowrap w-[130px]"
const valueCell = "px-4 py-3"
const inputCls  = "border rounded-lg px-3 py-1.5 text-[13px] outline-none focus:ring-1 transition-all"
const inputStyle = { borderColor: '#D1D5DB' }

export default function TransferAccountPage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [activeRecipientTab, setActiveRecipientTab] = useState('최근입금계좌')
  const [fromAccount, setFromAccount] = useState('')
  const [toBank, setToBank]     = useState('AXful')
  const [toAccount, setToAccount] = useState('')
  const [amount, setAmount]     = useState('')
  const [password, setPassword] = useState('')
  const [showBankModal, setShowBankModal] = useState(false)
  const [bankTab, setBankTab]   = useState('은행')
  const [accounts, setAccounts] = useState<DepositViewAccount[]>([])
  const [recentAccounts, setRecentAccounts] = useState<{ bank: string; name: string; number: string }[]>([])

  useEffect(() => {
    const to   = searchParams.get('to')
    const bank = searchParams.get('bank')
    if (to)   setToAccount(to)
    if (bank) setToBank(bank)
  }, [searchParams])

  useEffect(() => {
    async function load() {
      try {
        const customerId = getCurrentDepositCustomerId()
        const accs = await fetchDepositAccountViewModels(customerId)
        setAccounts(accs)
        if (accs.length > 0) setFromAccount(accs[0].id)
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
      } catch { setAccounts([]) }
    }
    load()
  }, [])

  const fromAcc = accounts.find(a => a.id === fromAccount) ?? accounts[0]

  function handleAmountShortcut(label: string) {
    const map: Record<string, number> = { '100만': 1_000_000, '50만': 500_000, '10만': 100_000, '5만': 50_000, '1만': 10_000 }
    if (label === '전액') {
      setAmount(String(fromAcc?.availableBalance ?? 0))
    } else if (label === '정결') {
      const n = parseInt(amount.replace(/,/g, '')) || 0
      setAmount(String(Math.floor(n / 10000) * 10000))
    } else if (map[label]) {
      setAmount(String((parseInt(amount.replace(/,/g, '')) || 0) + map[label]))
    }
  }

  function handleConfirm() {
    const amountNum = parseInt(amount.replace(/,/g, '')) || 0
    if (!toAccount || amountNum === 0) return
    sessionStorage.setItem('pendingTransfer', JSON.stringify({
      fromNumber: fromAcc?.number ?? '',
      fromName: fromAcc?.name ?? '',
      toBank,
      toBankCode: MOCK_BANKS.find(b => b.name === toBank)?.code ?? 'KB',
      toAccount,
      amount: amountNum,
      receiverName: recentAccounts.find(r => r.number === toAccount)?.name ?? '수취인',
      fee: 0,
    }))
    router.push('/transfer/confirm')
  }

  const recipientCard = (acc: { bank: string; name: string; number: string }) => {
    const selected = toAccount === acc.number
    return (
      <button key={acc.number} onClick={() => { setToBank(acc.bank); setToAccount(acc.number) }}
        className="border rounded-xl px-4 py-3 text-left text-[12px] transition-colors"
        style={{
          minWidth: 160,
          borderColor: selected ? '#0D5C47' : '#E2F5EF',
          backgroundColor: selected ? '#F0FAF7' : 'white',
        }}>
        <p className="font-semibold text-kb-text">{acc.name}</p>
        <p className="text-kb-text-muted mt-0.5">{acc.bank}</p>
        <p className="text-kb-text-muted">{acc.number}</p>
      </button>
    )
  }

  return (
    <div className="max-w-kb-container mx-auto px-6">
      <div className="flex">
        <TransferSidebar />

        <main className="flex-1 pl-8 pt-6 pb-12">
          <h1 className="text-[22px] font-bold text-kb-text mb-5">계좌이체</h1>

          {/* 수취인 탭 */}
          <div className="rounded-xl overflow-hidden mb-5" style={{ border: '1px solid #E2F5EF' }}>
            <div className="flex border-b" style={{ borderColor: '#E2F5EF' }}>
              {['최근입금계좌', '자주쓰는계좌', '내계좌', '단축이체'].map(tab => (
                <button key={tab} onClick={() => setActiveRecipientTab(tab)}
                  className="px-5 py-2.5 text-[13px] font-medium transition-colors"
                  style={activeRecipientTab === tab
                    ? { backgroundColor: 'white', color: '#0D5C47', fontWeight: 700, borderBottom: '2px solid #0D5C47' }
                    : { backgroundColor: '#F8FFFE', color: '#9CA3AF' }}>
                  {tab}
                </button>
              ))}
            </div>

            {activeRecipientTab === '최근입금계좌' && (
              <div className="p-4">
                <p className="text-[12px] text-kb-text-muted mb-3">최근 3개월 이내 이체된 입금계좌입니다.</p>
                <div className="flex gap-3 flex-wrap">
                  {recentAccounts.length === 0
                    ? <p className="text-[13px] text-kb-text-muted py-4">최근 이체 내역이 없습니다.</p>
                    : recentAccounts.map(acc => recipientCard(acc))
                  }
                </div>
              </div>
            )}

            {activeRecipientTab === '자주쓰는계좌' && (
              <div className="p-8 flex flex-col items-center gap-3">
                <p className="text-[13px] text-kb-text-muted">자주쓰는계좌가 등록되어 있지 않습니다.</p>
                <button className="mt-2 border rounded-lg px-6 py-2 text-[12px] transition-colors hover:bg-[#F0FAF7]"
                  style={{ borderColor: '#5BC9A8', color: '#0D5C47' }}>
                  자주쓰는계좌 등록/삭제
                </button>
              </div>
            )}

            {activeRecipientTab === '내계좌' && (
              <div className="p-4">
                <p className="text-[12px] mb-3" style={{ color: '#0D5C47' }}>
                  AXful Bank 내 계좌로 이체 시 추가 인증 없이 이체 가능합니다.
                </p>
                <div className="flex gap-3 flex-wrap">
                  {accounts.map(acc => recipientCard({ bank: 'AXful', name: acc.name, number: acc.number }))}
                </div>
              </div>
            )}

            {activeRecipientTab === '단축이체' && (
              <div className="p-8 flex flex-col items-center gap-3">
                <p className="text-[13px] text-kb-text-muted">단축이체가 등록되어 있지 않습니다.</p>
                <button className="mt-2 border rounded-lg px-6 py-2 text-[12px] transition-colors hover:bg-[#F0FAF7]"
                  style={{ borderColor: '#5BC9A8', color: '#0D5C47' }}>
                  단축이체 등록/삭제
                </button>
              </div>
            )}
          </div>

          {/* 이체 폼 */}
          <div className="rounded-xl overflow-hidden mb-6" style={{ border: '1px solid #E2F5EF' }}>
            <table className="w-full text-[13px]">
              <tbody>
                <tr style={{ borderBottom: '1px solid #E2F5EF' }}>
                  <td className={labelCell} style={{ backgroundColor: '#F0FAF7' }}>출금계좌번호</td>
                  <td className={valueCell}>
                    <select value={fromAccount} onChange={e => setFromAccount(e.target.value)}
                      className={inputCls + " w-[280px]"} style={inputStyle}>
                      {accounts.map(a => <option key={a.id} value={a.id}>{a.number} ({a.name})</option>)}
                    </select>
                    {fromAcc && (
                      <span className="ml-3 text-[12px] text-kb-text-muted">출금가능 {formatNumber(fromAcc.availableBalance)}원</span>
                    )}
                  </td>
                </tr>
                <tr style={{ borderBottom: '1px solid #E2F5EF' }}>
                  <td className={labelCell} style={{ backgroundColor: '#F0FAF7' }}>입금기관</td>
                  <td className={valueCell}>
                    <div className="flex items-center gap-2">
                      <input type="text" value={toBank} readOnly
                        className={inputCls + " w-28"} style={{ ...inputStyle, backgroundColor: '#F8FFFE' }} />
                      <button onClick={() => setShowBankModal(true)}
                        className="border rounded-lg px-3 py-1.5 text-[12px] font-medium transition-colors hover:bg-[#F0FAF7]"
                        style={{ borderColor: '#5BC9A8', color: '#0D5C47' }}>
                        기관선택
                      </button>
                    </div>
                  </td>
                </tr>
                <tr style={{ borderBottom: '1px solid #E2F5EF' }}>
                  <td className={labelCell} style={{ backgroundColor: '#F0FAF7' }}>입금계좌번호</td>
                  <td className={valueCell}>
                    <input type="text" value={toAccount} onChange={e => setToAccount(e.target.value)}
                      placeholder="계좌번호 입력" className={inputCls + " w-52"} style={inputStyle} />
                  </td>
                </tr>
                <tr style={{ borderBottom: '1px solid #E2F5EF' }}>
                  <td className={labelCell + " align-top pt-4"} style={{ backgroundColor: '#F0FAF7' }}>이체금액</td>
                  <td className={valueCell}>
                    <div className="flex gap-1.5 mb-2 flex-wrap">
                      {AMOUNT_SHORTCUTS.map(s => (
                        <button key={s} onClick={() => handleAmountShortcut(s)}
                          className="border rounded-lg px-3 py-1 text-[12px] font-medium transition-colors hover:bg-[#F0FAF7]"
                          style={{ borderColor: '#5BC9A8', color: '#0D5C47' }}>
                          {s}
                        </button>
                      ))}
                    </div>
                    <div className="flex items-center gap-2">
                      <input type="text"
                        value={amount ? formatNumber(parseInt(amount.replace(/,/g,''))||0) : ''}
                        onChange={e => setAmount(e.target.value.replace(/,/g,''))}
                        placeholder="0"
                        className={inputCls + " w-52 text-right"}
                        style={inputStyle} />
                      <span className="text-[13px] text-kb-text-muted">원</span>
                    </div>
                  </td>
                </tr>
                <tr>
                  <td className={labelCell} style={{ backgroundColor: '#F0FAF7' }}>계좌비밀번호</td>
                  <td className={valueCell}>
                    <input type="password" value={password} onChange={e => setPassword(e.target.value)}
                      maxLength={4} placeholder="4자리 입력"
                      className={inputCls + " w-28"} style={inputStyle} />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          {/* 확인 버튼 */}
          <div className="flex justify-center">
            <button onClick={handleConfirm}
              className="px-24 py-3 text-[15px] font-bold text-white rounded-xl hover:opacity-85 transition-opacity"
              style={{ backgroundColor: '#0D5C47' }}>
              확인
            </button>
          </div>
        </main>
      </div>

      {/* 기관선택 모달 */}
      {showBankModal && (
        <div className="fixed inset-0 bg-black/40 z-[200] flex items-center justify-center">
          <div className="bg-white rounded-2xl w-[520px] shadow-2xl overflow-hidden">
            <div className="flex items-center justify-between px-5 py-4" style={{ backgroundColor: '#0D5C47' }}>
              <span className="font-bold text-[15px] text-white">기관선택</span>
              <button onClick={() => setShowBankModal(false)} className="text-white text-xl leading-none hover:opacity-75">✕</button>
            </div>
            <div className="p-5">
              <div className="flex border-b mb-4" style={{ borderColor: '#E2F5EF' }}>
                {['은행', '증권사', '국세납부', '지방세입납부'].map(t => (
                  <button key={t} onClick={() => setBankTab(t)}
                    className="px-5 py-2 text-[13px] border-b-2 -mb-px transition-colors"
                    style={bankTab === t
                      ? { borderColor: '#0D5C47', color: '#0D5C47', fontWeight: 700 }
                      : { borderColor: 'transparent', color: '#9CA3AF' }}>
                    {t}
                  </button>
                ))}
              </div>
              <div className="grid grid-cols-3 gap-2">
                {MOCK_BANKS.map(bank => (
                  <button key={bank.code}
                    onClick={() => { setToBank(bank.name); setShowBankModal(false) }}
                    className="flex items-center gap-2 px-3 py-2 border rounded-lg text-[13px] text-kb-text-body transition-colors hover:bg-[#F0FAF7] text-left"
                    style={{ borderColor: '#E2F5EF' }}>
                    <span className="w-5 h-5 rounded-full flex-shrink-0" style={{ backgroundColor: '#E2F5EF' }} />
                    {bank.name}
                  </button>
                ))}
              </div>
              <div className="mt-5 flex justify-center">
                <button onClick={() => setShowBankModal(false)}
                  className="border rounded-lg px-12 py-2 text-[13px] font-medium transition-colors hover:bg-[#F0FAF7]"
                  style={{ borderColor: '#5BC9A8', color: '#0D5C47' }}>
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
