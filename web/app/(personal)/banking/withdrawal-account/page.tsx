'use client'
import { KB_MINT,KB_PRIMARY,KB_PRIMARY_BG } from '@/lib/theme'

import Link from 'next/link'
import { useState, useEffect, useCallback } from 'react'
import { api } from '@/lib/api'

// ── 은행 목록 (타행만) ───────────────────────────────────────
// 본행(AXful) 보유계좌는 등록 대상이 아니다. 이체 시 자동으로 출금계좌에 표시된다.

const BANKS = [
  { code: '004',   name: 'KB국민은행' },
  { code: '088',   name: '신한은행' },
  { code: '081',   name: '하나은행' },
  { code: '020',   name: '우리은행' },
  { code: '011',   name: 'NH농협은행' },
  { code: '003',   name: 'IBK기업은행' },
  { code: '090',   name: '카카오뱅크' },
  { code: '092',   name: '토스뱅크' },
  { code: '023',   name: 'SC제일은행' },
  { code: '039',   name: '경남은행' },
  { code: '034',   name: '광주은행' },
]

const NOTICES_LIST = [
  '본행(AXful) 보유계좌는 등록 없이 이체 시 자동으로 표시됩니다. 이 화면에서는 타행 출금계좌를 등록·관리합니다.',
  '등록한 타행 계좌는 목록에서 확인하고 삭제할 수 있습니다.',
]

const NOTICES_REG = [
  '본행(AXful) 보유계좌는 등록 대상이 아닙니다. 타행 계좌만 등록할 수 있습니다.',
  '등록할 계좌번호와 예금주명을 정확히 입력해 주세요.',
  '타행 계좌 등록 시 1원 검증이 진행됩니다. (MVP: 검증 모의 처리)',
  '1인당 최대 10개까지 타행 출금계좌를 등록할 수 있습니다.',
]

// ── 타입 ─────────────────────────────────────────────────────

interface WithdrawalAccount {
  withdrawalAccountId: number
  accountNumber: string
  bankCode: string
  bankName: string
  accountHolderName: string | null
  accountAlias: string | null
  registrationType: string
  priorityOrder: number
  registeredAt: string
}

// ── 공통 UI ──────────────────────────────────────────────────

function NoticeBox({ items }: { items: string[] }) {
  return (
    <div className="bg-kb-primary-bg border border-kb-border rounded-xl px-5 py-4 space-y-2">
      {items.map((text, i) => (
        <div key={i} className="flex items-start gap-2 text-[13px] text-kb-text-body">
          <span className="flex-shrink-0 mt-1.5 w-1.5 h-1.5 rounded-full flex-none" style={{ backgroundColor: KB_MINT }} />
          {text}
        </div>
      ))}
    </div>
  )
}

function getCustomerId() {
  return localStorage.getItem('customerId') ?? ''
}

// ─────────────────────────────────────────────────────────────

export default function WithdrawalAccountPage() {
  const [tab, setTab] = useState<'list' | 'register'>('list')

  // ── 목록 탭 상태 ──
  const [accounts, setAccounts]     = useState<WithdrawalAccount[]>([])
  const [loading, setLoading]       = useState(true)
  const [deleteId, setDeleteId]     = useState<number | null>(null)
  const [deleteLoading, setDeleteLoading] = useState(false)
  const [deleteError, setDeleteError]     = useState('')
  const [deleteDone, setDeleteDone]       = useState(false)

  // ── 등록 탭 상태 ──
  const [bankCode, setBankCode]     = useState(BANKS[0].code)
  const [accountNo, setAccountNo]   = useState('')
  const [holderName, setHolderName] = useState('')
  const [alias, setAlias]           = useState('')
  const [regStep, setRegStep]       = useState<'form' | 'verify' | 'done'>('form')
  const [regLoading, setRegLoading] = useState(false)
  const [regError, setRegError]     = useState('')
  const [regResult, setRegResult]   = useState<WithdrawalAccount | null>(null)

  const loadAccounts = useCallback(async () => {
    setLoading(true)
    try {
      const { data } = await api.get('/api/v1/banking/withdrawal-accounts', {
        headers: { 'X-Customer-Id': getCustomerId() },
      })
      setAccounts(data.data ?? [])
    } catch {
      setAccounts([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadAccounts() }, [loadAccounts])

  // ── 삭제 ──
  async function handleDelete() {
    if (!deleteId) return
    setDeleteLoading(true); setDeleteError('')
    try {
      await api.delete(`/api/v1/banking/withdrawal-accounts/${deleteId}`, {
        headers: { 'X-Customer-Id': getCustomerId() },
      })
      setDeleteDone(true)
      await loadAccounts()
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } }
      setDeleteError(e.response?.data?.message ?? '삭제 중 오류가 발생했습니다.')
    } finally {
      setDeleteLoading(false)
    }
  }

  // ── 등록 ──
  function goVerify() {
    if (!accountNo.match(/^\d{10,14}$/)) { setRegError('계좌번호는 10~14자리 숫자여야 합니다.'); return }
    if (!holderName.trim())              { setRegError('예금주명을 입력해 주세요.'); return }
    setRegError(''); setRegStep('verify')
  }

  async function handleRegister() {
    setRegLoading(true); setRegError('')
    try {
      const { data } = await api.post('/api/v1/banking/withdrawal-accounts', {
        accountNumber:     accountNo,
        bankCode,
        bankName:          BANKS.find(b => b.code === bankCode)?.name ?? bankCode,
        accountHolderName: holderName,
        accountAlias:      alias || null,
      }, { headers: { 'X-Customer-Id': getCustomerId() } })
      setRegResult(data.data)
      setRegStep('done')
      await loadAccounts()
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } }
      setRegError(e.response?.data?.message ?? '등록 중 오류가 발생했습니다.')
      setRegStep('form')
    } finally {
      setRegLoading(false)
    }
  }

  function resetRegForm() {
    setBankCode(BANKS[0].code); setAccountNo(''); setHolderName(''); setAlias('')
    setRegStep('form'); setRegError(''); setRegResult(null)
  }

  const selectedDeleteAccount = accounts.find(a => a.withdrawalAccountId === deleteId)

  return (
    <div className="max-w-kb-container mx-auto px-6 py-8">

      {/* 브레드크럼 */}
      <div className="flex items-center gap-1 text-[12px] text-kb-text-muted mb-6">
        <Link href="/" className="hover:underline">홈</Link>
        <span>›</span>
        <span>뱅킹관리</span>
        <span>›</span>
        <span>계좌관리</span>
        <span>›</span>
        <span className="font-semibold text-kb-text">타행 출금계좌 등록/삭제</span>
      </div>

      <h1 className="text-[24px] font-bold text-kb-text mb-8">타행 출금계좌 등록/삭제</h1>

      {/* 탭 */}
      <div className="flex border-b border-kb-border mb-6">
        {([['list', '타행 출금계좌 등록현황'], ['register', '타행 출금계좌 신규 등록']] as const).map(([key, label]) => (
          <button key={key} onClick={() => setTab(key)}
            className={`px-6 py-3 text-[14px] whitespace-nowrap transition-colors
              ${tab === key ? 'border-b-2 font-bold -mb-px' : 'text-kb-text-muted hover:text-kb-text'}`}
            style={tab === key ? { borderColor: KB_PRIMARY, color: KB_PRIMARY } : {}}>
            {label}
          </button>
        ))}
      </div>

      {/* ── 등록현황 탭 ── */}
      {tab === 'list' && (
        <div className="space-y-5">
          <NoticeBox items={NOTICES_LIST} />

          {loading ? (
            <div className="flex justify-center py-12">
              <div className="w-8 h-8 rounded-full border-[3px] border-t-transparent animate-spin" style={{ borderColor: KB_MINT, borderTopColor: 'transparent' }} />
            </div>
          ) : accounts.length === 0 ? (
            <div className="border border-kb-border rounded-xl px-6 py-12 text-center text-[14px] text-kb-text-muted">
              등록된 타행 출금계좌가 없습니다.
              <br />
              <button onClick={() => setTab('register')}
                className="mt-3 inline-block text-[13px] font-semibold hover:underline"
                style={{ color: KB_PRIMARY }}>
                타행 출금계좌 신규 등록 →
              </button>
            </div>
          ) : (
            <div className="border border-kb-border rounded-xl overflow-hidden">
              <table className="w-full text-[13px] border-collapse">
                <thead>
                  <tr style={{ backgroundColor: KB_PRIMARY }}>
                    {['No.', '은행', '계좌번호', '예금주', '별칭', '등록일', '삭제'].map(h => (
                      <th key={h} className="px-4 py-3 text-center font-semibold text-white border-l border-white/20 first:border-l-0 whitespace-nowrap">
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-kb-border">
                  {accounts.map((acc, idx) => (
                    <tr key={acc.withdrawalAccountId} className="hover:bg-kb-primary-surface transition-colors">
                      <td className="px-4 py-3 text-center text-kb-text-muted">{idx + 1}</td>
                      <td className="px-4 py-3 text-center whitespace-nowrap">{acc.bankName}</td>
                      <td className="px-4 py-3 text-center font-mono tracking-wider">{acc.accountNumber}</td>
                      <td className="px-4 py-3 text-center">{acc.accountHolderName ?? '-'}</td>
                      <td className="px-4 py-3 text-center text-kb-text-muted">{acc.accountAlias ?? '-'}</td>
                      <td className="px-4 py-3 text-center text-kb-text-muted whitespace-nowrap">{acc.registeredAt}</td>
                      <td className="px-4 py-3 text-center">
                        <button
                          onClick={() => { setDeleteId(acc.withdrawalAccountId); setDeleteDone(false); setDeleteError('') }}
                          className="px-3 py-1 text-[12px] font-semibold text-red-500 border border-red-200 rounded hover:bg-red-50 transition-colors">
                          삭제
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* ── 신규 등록 탭 ── */}
      {tab === 'register' && (
        <div className="space-y-5">
          <NoticeBox items={NOTICES_REG} />

          {/* 등록 폼 */}
          {regStep === 'form' && (
            <>
              <div className="border border-kb-border rounded-xl overflow-hidden">
                <table className="w-full text-[13px]">
                  <tbody>
                    {/* 은행 선택 */}
                    <tr className="border-b border-kb-border">
                      <td className="px-5 py-3.5 font-semibold text-kb-text w-44 whitespace-nowrap" style={{ backgroundColor: KB_PRIMARY_BG }}>
                        은행 선택 <span className="text-kb-primary ml-0.5">*</span>
                      </td>
                      <td className="border-l border-kb-border px-5 py-3">
                        <select value={bankCode} onChange={e => setBankCode(e.target.value)}
                          className="border border-kb-border rounded-lg px-3 py-2 text-[13px] bg-white outline-none focus:border-kb-primary w-52">
                          {BANKS.map(b => <option key={b.code} value={b.code}>{b.name}</option>)}
                        </select>
                      </td>
                    </tr>
                    {/* 계좌번호 */}
                    <tr className="border-b border-kb-border">
                      <td className="px-5 py-3.5 font-semibold text-kb-text whitespace-nowrap" style={{ backgroundColor: KB_PRIMARY_BG }}>
                        계좌번호 <span className="text-kb-primary ml-0.5">*</span>
                      </td>
                      <td className="border-l border-kb-border px-5 py-3">
                        <input type="text" value={accountNo}
                          onChange={e => setAccountNo(e.target.value.replace(/\D/g, '').slice(0, 14))}
                          placeholder="- 없이 숫자만 입력"
                          className="border border-kb-border rounded-lg px-3 py-2 text-[13px] w-52 outline-none focus:border-kb-primary font-mono tracking-wider" />
                      </td>
                    </tr>
                    {/* 예금주명 */}
                    <tr className="border-b border-kb-border">
                      <td className="px-5 py-3.5 font-semibold text-kb-text whitespace-nowrap" style={{ backgroundColor: KB_PRIMARY_BG }}>
                        예금주명 <span className="text-kb-primary ml-0.5">*</span>
                      </td>
                      <td className="border-l border-kb-border px-5 py-3">
                        <input type="text" value={holderName}
                          onChange={e => setHolderName(e.target.value)}
                          placeholder="예금주 성명"
                          className="border border-kb-border rounded-lg px-3 py-2 text-[13px] w-52 outline-none focus:border-kb-primary" />
                      </td>
                    </tr>
                    {/* 별칭 */}
                    <tr>
                      <td className="px-5 py-3.5 font-semibold text-kb-text whitespace-nowrap" style={{ backgroundColor: KB_PRIMARY_BG }}>
                        별칭 <span className="text-[12px] font-normal text-kb-text-muted">(선택)</span>
                      </td>
                      <td className="border-l border-kb-border px-5 py-3">
                        <input type="text" value={alias}
                          onChange={e => setAlias(e.target.value)}
                          placeholder="예: 월급통장, 비상금"
                          className="border border-kb-border rounded-lg px-3 py-2 text-[13px] w-52 outline-none focus:border-kb-primary" />
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              {regError && <p className="text-[12px] text-red-500">{regError}</p>}

              <div className="flex gap-3">
                <button onClick={() => setTab('list')}
                  className="px-8 py-2.5 text-[14px] border border-kb-border text-kb-text-body hover:bg-kb-primary-bg transition-colors">
                  취소
                </button>
                <button onClick={goVerify}
                  className="px-8 py-2.5 text-[14px] font-bold text-white rounded-lg hover:opacity-85 transition-opacity"
                  style={{ backgroundColor: KB_PRIMARY }}>
                  다음 (1원 검증)
                </button>
              </div>
            </>
          )}

          {/* 1원 검증 단계 (모의) */}
          {regStep === 'verify' && (
            <div className="space-y-5">
              <div className="border border-kb-border rounded-xl overflow-hidden">
                <div className="px-5 py-3 font-semibold text-[14px] text-white" style={{ backgroundColor: KB_PRIMARY }}>
                  1원 검증 (모의 처리)
                </div>
                <div className="px-6 py-5 space-y-3">
                  <div className="bg-[#FFF9E6] border border-[#E8D88A] rounded-xl px-4 py-3 text-[12px] text-[#7A6200]">
                    <p className="font-bold mb-1">등록 정보 확인</p>
                    <p>은행: <strong>{BANKS.find(b => b.code === bankCode)?.name}</strong></p>
                    <p>계좌번호: <strong className="font-mono">{accountNo}</strong></p>
                    <p>예금주: <strong>{holderName}</strong></p>
                    {alias && <p>별칭: <strong>{alias}</strong></p>}
                  </div>
                  <p className="text-[13px] text-kb-text-body">
                    입력하신 계좌로 <strong>1원</strong>을 송금하여 계좌 소유자를 확인합니다.<br />
                    <span className="text-kb-text-muted text-[12px]">(MVP 환경에서는 검증 없이 즉시 등록됩니다)</span>
                  </p>
                  {regError && <p className="text-[12px] text-red-500">{regError}</p>}
                </div>
              </div>
              <div className="flex gap-3">
                <button onClick={() => setRegStep('form')}
                  className="px-8 py-2.5 text-[14px] border border-kb-border text-kb-text-body hover:bg-kb-primary-bg transition-colors">
                  이전
                </button>
                <button onClick={handleRegister} disabled={regLoading}
                  className="px-8 py-2.5 text-[14px] font-bold text-white rounded-lg hover:opacity-85 disabled:opacity-50 transition-opacity"
                  style={{ backgroundColor: KB_PRIMARY }}>
                  {regLoading ? '등록 중...' : '등록 확인'}
                </button>
              </div>
            </div>
          )}

          {/* 등록 완료 */}
          {regStep === 'done' && regResult && (
            <div className="space-y-4">
              <div className="border border-kb-border bg-kb-primary-bg px-6 py-8 rounded-xl flex items-center gap-6">
                <div className="w-16 h-16 rounded-full flex items-center justify-center flex-shrink-0"
                  style={{ backgroundColor: KB_PRIMARY }}>
                  <svg viewBox="0 0 24 24" fill="none" className="w-8 h-8" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                    <polyline points="20,6 9,17 4,12"/>
                  </svg>
                </div>
                <div className="space-y-1">
                  <p className="text-[16px] font-bold" style={{ color: KB_PRIMARY }}>출금계좌가 등록되었습니다.</p>
                  <p className="text-[13px] text-kb-text-muted">
                    {regResult.bankName} · <span className="font-mono">{regResult.accountNumber}</span>
                    {regResult.accountAlias && <span> ({regResult.accountAlias})</span>}
                  </p>
                  <p className="text-[12px] text-kb-text-muted">등록일: {regResult.registeredAt}</p>
                </div>
              </div>
              <div className="flex gap-3">
                <button onClick={() => { setTab('list'); resetRegForm() }}
                  className="px-8 py-2.5 text-[14px] font-bold text-white rounded-lg hover:opacity-85 transition-opacity"
                  style={{ backgroundColor: KB_PRIMARY }}>
                  등록현황 확인
                </button>
                <button onClick={resetRegForm}
                  className="px-8 py-2.5 text-[14px] border border-kb-border text-kb-text-body hover:bg-kb-primary-bg transition-colors">
                  추가 등록
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ── 삭제 확인 모달 ── */}
      {deleteId !== null && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white w-full max-w-md rounded-xl shadow-2xl overflow-hidden">
            <div className="flex items-center justify-between px-6 py-4 border-b border-kb-border" style={{ backgroundColor: KB_PRIMARY }}>
              <span className="text-[15px] font-bold text-white">출금계좌 삭제</span>
              <button onClick={() => setDeleteId(null)} className="text-white/70 hover:text-white text-xl">✕</button>
            </div>
            <div className="px-6 py-5 space-y-4">
              {deleteDone ? (
                <div className="flex items-center gap-3 bg-kb-primary-bg border border-kb-border rounded-xl px-5 py-4">
                  <div className="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0" style={{ backgroundColor: KB_PRIMARY }}>
                    <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                      <polyline points="20,6 9,17 4,12"/>
                    </svg>
                  </div>
                  <div>
                    <p className="text-[14px] font-bold" style={{ color: KB_PRIMARY }}>출금계좌가 삭제되었습니다.</p>
                    <p className="text-[12px] text-kb-text-muted">해당 계좌는 등록현황에서 제거됩니다.</p>
                  </div>
                </div>
              ) : (
                <>
                  <div className="bg-[#FFF4F4] border border-red-200 rounded-xl px-4 py-3 space-y-1.5 text-[12px] text-kb-text-body">
                    <p className="font-bold text-red-600">⚠ 삭제 주의사항</p>
                    <p>삭제된 계좌는 즉시 출금계좌 목록에서 제거됩니다.</p>
                    <p>재등록 시 24시간 이체 제한이 다시 적용됩니다.</p>
                  </div>
                  {selectedDeleteAccount && (
                    <div className="border border-kb-border rounded-xl overflow-hidden">
                      <table className="w-full text-[13px]">
                        <tbody>
                          {[
                            ['은행', selectedDeleteAccount.bankName],
                            ['계좌번호', selectedDeleteAccount.accountNumber],
                            ['예금주', selectedDeleteAccount.accountHolderName ?? '-'],
                          ].map(([label, value]) => (
                            <tr key={label} className="border-b border-kb-border last:border-b-0">
                              <td className="px-4 py-2.5 font-semibold text-[12px] w-24" style={{ backgroundColor: KB_PRIMARY_BG }}>{label}</td>
                              <td className="border-l border-kb-border px-4 py-2.5 text-[12px] font-mono">{value}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                  {deleteError && <p className="text-[12px] text-red-500">{deleteError}</p>}
                </>
              )}
              <div className="flex justify-end gap-3">
                {deleteDone ? (
                  <button onClick={() => setDeleteId(null)}
                    className="px-8 py-2 text-[13px] font-bold text-white rounded-lg hover:opacity-85"
                    style={{ backgroundColor: KB_PRIMARY }}>확인</button>
                ) : (
                  <>
                    <button onClick={() => setDeleteId(null)}
                      className="px-6 py-2 border border-kb-border text-[13px] text-kb-text-body hover:bg-kb-primary-bg transition-colors">취소</button>
                    <button onClick={handleDelete} disabled={deleteLoading}
                      className="px-6 py-2 text-[13px] font-bold text-white rounded-lg bg-red-500 hover:bg-red-600 disabled:opacity-50 transition-colors">
                      {deleteLoading ? '삭제 중...' : '삭제 확인'}
                    </button>
                  </>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
