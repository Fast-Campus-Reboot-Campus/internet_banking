'use client'

import Link from 'next/link'
import { useState } from 'react'
import { useParams, useSearchParams } from 'next/navigation'
import LoanSidebar from '@/components/inquiry/LoanSidebar'
import { api } from '@/lib/api'

const inputCls  = "border rounded-lg px-3 py-2 text-[13px] outline-none focus:ring-1 transition-all"
const inputStyle = { borderColor: '#D1D5DB' }

function PrimaryBtn({ onClick, disabled, children }: { onClick: () => void; disabled?: boolean; children: React.ReactNode }) {
  return (
    <button onClick={onClick} disabled={disabled}
      className="px-12 py-2.5 text-[14px] font-bold text-white rounded-xl hover:opacity-85 transition-opacity disabled:opacity-50"
      style={{ backgroundColor: '#0D5C47' }}>
      {children}
    </button>
  )
}

function InfoNotice({ lines }: { lines: string[] }) {
  return (
    <div className="rounded-xl px-5 py-4 mb-5 text-[12px] space-y-1.5"
      style={{ backgroundColor: '#F8FFFE', border: '1px solid #E2F5EF' }}>
      {lines.map((l, i) => <p key={i} className="text-kb-text-muted">· {l}</p>)}
    </div>
  )
}

/* ── 적용금리조회 ── */
function RatePage({ cntrId }: { cntrId: string }) {
  const [rows, setRows] = useState<{ accrualDate: string; dailyInterest: number; cumulativeInterest: number }[]>([])
  const [searched, setSearched] = useState(false)
  const [from, setFrom] = useState('')
  const [to, setTo]     = useState('')

  async function handleSearch() {
    try {
      const res = await api.get(`/api/loan-contracts/${cntrId}/interest-accruals`, {
        params: { from: from.replace(/\./g, ''), to: to.replace(/\./g, '') },
      })
      setRows(res.data.data?.items ?? [])
    } catch { setRows([]) }
    setSearched(true)
  }

  return (
    <div>
      <InfoNotice lines={['계약별 일별 이자 누적 내역을 조회합니다.', '조회 기간을 입력하지 않으면 전체 이력이 조회됩니다.']} />
      <div className="rounded-xl overflow-hidden mb-5" style={{ border: '1px solid #E2F5EF' }}>
        {[
          { label: '계약번호',   content: <span className="text-[13px] font-medium" style={{ color: '#0D5C47' }}>{cntrId || '-'}</span> },
          { label: '조회기간',   content: (
            <div className="flex items-center gap-2">
              <input type="text" value={from} onChange={e => setFrom(e.target.value)} placeholder="YYYY.MM.DD" className={inputCls + ' w-32'} style={inputStyle} />
              <span className="text-kb-text-muted">~</span>
              <input type="text" value={to}   onChange={e => setTo(e.target.value)}   placeholder="YYYY.MM.DD" className={inputCls + ' w-32'} style={inputStyle} />
            </div>
          )},
        ].map(({ label, content }, i, arr) => (
          <div key={label} className="flex items-center"
            style={{ borderBottom: i < arr.length - 1 ? '1px solid #E2F5EF' : 'none' }}>
            <div className="w-[160px] px-5 py-3 text-[13px] font-semibold flex-shrink-0"
              style={{ backgroundColor: '#F0FAF7', color: '#0D5C47', alignSelf: 'stretch', display: 'flex', alignItems: 'center' }}>
              {label}
            </div>
            <div className="flex-1 px-5 py-2.5">{content}</div>
          </div>
        ))}
      </div>
      <div className="flex justify-center mb-6"><PrimaryBtn onClick={handleSearch}>조회</PrimaryBtn></div>
      {searched && (
        <div className="rounded-xl overflow-hidden" style={{ border: '1px solid #E2F5EF' }}>
          <table className="w-full text-[13px]">
            <thead>
              <tr style={{ backgroundColor: '#F0FAF7', borderBottom: '2px solid #E2F5EF' }}>
                {['일자', '일별 이자', '누적 이자'].map(h => (
                  <th key={h} className="px-4 py-3 text-center font-semibold text-[12px]" style={{ color: '#0D5C47' }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 ? (
                <tr><td colSpan={3} className="px-4 py-8 text-center text-[13px] text-kb-text-muted">조회된 내역이 없습니다.</td></tr>
              ) : rows.map((r, i, arr) => (
                <tr key={r.accrualDate} className="hover:bg-[#F8FFFE]"
                  style={{ borderBottom: i < arr.length - 1 ? '1px solid #E2F5EF' : 'none' }}>
                  <td className="px-4 py-3 text-center">{r.accrualDate}</td>
                  <td className="px-4 py-3 text-right pr-5">{r.dailyInterest.toLocaleString('ko-KR')}원</td>
                  <td className="px-4 py-3 text-right pr-5 font-medium" style={{ color: '#0D5C47' }}>{r.cumulativeInterest.toLocaleString('ko-KR')}원</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

/* ── 이자/월부금입금 ── */
function PaymentPage({ cntrId }: { cntrId: string }) {
  const [amount, setAmount]   = useState('')
  const [account, setAccount] = useState('')
  const [done, setDone]       = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState('')

  async function handleSubmit() {
    if (!amount || !account) { setError('모든 항목을 입력해주세요.'); return }
    setError(''); setLoading(true)
    try {
      await api.post(`/api/loan-contracts/${cntrId}/repayments/online`, {
        paymentAmount: Number(amount.replace(/,/g, '')), accountNo: account,
      })
      setDone(true)
    } catch { setError('납입 처리에 실패했습니다. 잔액을 확인해주세요.') }
    finally { setLoading(false) }
  }

  if (done) return (
    <div className="rounded-xl p-6 flex items-center gap-5" style={{ backgroundColor: '#F0FAF7', border: '1px solid #E2F5EF' }}>
      <div className="w-12 h-12 rounded-full flex items-center justify-center flex-shrink-0" style={{ backgroundColor: '#0D5C47' }}>
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
      </div>
      <div>
        <p className="text-[16px] font-bold mb-1" style={{ color: '#0D5C47' }}>납입이 완료되었습니다.</p>
        <p className="text-[12px] text-kb-text-muted">{Number(amount.replace(/,/g, '')).toLocaleString('ko-KR')}원이 납입 처리되었습니다.</p>
      </div>
    </div>
  )

  return (
    <div>
      <InfoNotice lines={['이자 및 월부금을 납입합니다.', '납입 후 취소가 불가하오니 금액을 정확히 확인하세요.']} />
      {SimpleForm([
        { label: '계약번호', value: cntrId, readOnly: true },
        { label: '납입금액', value: amount, setter: setAmount, placeholder: '금액 입력 (원)', type: 'number' },
        { label: '출금계좌', value: account, setter: setAccount, placeholder: '출금 계좌번호 입력' },
      ])}
      {error && <p className="text-[13px] mb-4" style={{ color: '#E05555' }}>{error}</p>}
      <PrimaryBtn onClick={handleSubmit} disabled={loading}>{loading ? '처리 중...' : '납입'}</PrimaryBtn>
    </div>
  )
}

/* ── 대출금상환 ── */
function RepayPage({ cntrId }: { cntrId: string }) {
  const [amount, setAmount]   = useState('')
  const [account, setAccount] = useState('')
  const [type, setType]       = useState<'PARTIAL' | 'FULL'>('PARTIAL')
  const [done, setDone]       = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState('')

  async function handleSubmit() {
    if (!account) { setError('출금 계좌를 입력해주세요.'); return }
    if (type === 'PARTIAL' && !amount) { setError('상환 금액을 입력해주세요.'); return }
    setError(''); setLoading(true)
    try {
      await api.post(`/api/loan-contracts/${cntrId}/repayments/partial`, {
        repaymentAmount: type === 'FULL' ? undefined : Number(amount.replace(/,/g, '')),
        repaymentType: type, accountNo: account,
      })
      setDone(true)
    } catch { setError('상환 처리에 실패했습니다.') }
    finally { setLoading(false) }
  }

  if (done) return (
    <div className="rounded-xl p-6 flex items-center gap-5" style={{ backgroundColor: '#F0FAF7', border: '1px solid #E2F5EF' }}>
      <div className="w-12 h-12 rounded-full flex items-center justify-center flex-shrink-0" style={{ backgroundColor: '#0D5C47' }}>
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
      </div>
      <div>
        <p className="text-[16px] font-bold mb-1" style={{ color: '#0D5C47' }}>상환이 완료되었습니다.</p>
        <p className="text-[12px] text-kb-text-muted">내 대출 현황에서 잔액을 확인하세요.</p>
      </div>
    </div>
  )

  return (
    <div>
      <InfoNotice lines={['대출금을 일부 또는 전액 상환합니다.', '1일 일부상환은 20회까지 가능합니다.']} />
      <div className="rounded-xl overflow-hidden mb-5" style={{ border: '1px solid #E2F5EF' }}>
        <div className="flex items-center" style={{ borderBottom: '1px solid #E2F5EF' }}>
          <div className="w-[160px] px-5 py-3 text-[13px] font-semibold flex-shrink-0"
            style={{ backgroundColor: '#F0FAF7', color: '#0D5C47', alignSelf: 'stretch', display: 'flex', alignItems: 'center' }}>
            상환구분
          </div>
          <div className="flex-1 px-5 py-3 flex gap-6">
            {([['PARTIAL', '일부상환'], ['FULL', '완제']] as const).map(([code, label]) => (
              <label key={code} className="flex items-center gap-1.5 cursor-pointer text-[13px]">
                <input type="radio" checked={type === code} onChange={() => setType(code)} style={{ accentColor: '#0D5C47' }} />
                {label}
              </label>
            ))}
          </div>
        </div>
        {type === 'PARTIAL' && (
          <div className="flex items-center" style={{ borderBottom: '1px solid #E2F5EF' }}>
            <div className="w-[160px] px-5 py-3 text-[13px] font-semibold flex-shrink-0"
              style={{ backgroundColor: '#F0FAF7', color: '#0D5C47', alignSelf: 'stretch', display: 'flex', alignItems: 'center' }}>
              상환금액
            </div>
            <div className="flex-1 px-5 py-2.5 flex items-center gap-2">
              <input type="text" value={amount} onChange={e => setAmount(e.target.value)} placeholder="금액 입력"
                className={inputCls + ' max-w-xs'} style={inputStyle} />
              <span className="text-[13px] text-kb-text-muted">원</span>
            </div>
          </div>
        )}
        <div className="flex items-center">
          <div className="w-[160px] px-5 py-3 text-[13px] font-semibold flex-shrink-0"
            style={{ backgroundColor: '#F0FAF7', color: '#0D5C47', alignSelf: 'stretch', display: 'flex', alignItems: 'center' }}>
            출금계좌
          </div>
          <div className="flex-1 px-5 py-2.5">
            <input type="text" value={account} onChange={e => setAccount(e.target.value)} placeholder="출금 계좌번호 입력"
              className={inputCls + ' max-w-xs'} style={inputStyle} />
          </div>
        </div>
      </div>
      {error && <p className="text-[13px] mb-4" style={{ color: '#E05555' }}>{error}</p>}
      <PrimaryBtn onClick={handleSubmit} disabled={loading}>{loading ? '처리 중...' : '상환'}</PrimaryBtn>
    </div>
  )
}

/* ── 공통 폼 헬퍼 ── */
function SimpleForm(rows: { label: string; value: string; readOnly?: boolean; setter?: (v: string) => void; placeholder?: string; type?: string }[]) {
  return (
    <div className="rounded-xl overflow-hidden mb-5" style={{ border: '1px solid #E2F5EF' }}>
      {rows.map(({ label, value, readOnly, setter, placeholder, type }, i, arr) => (
        <div key={label} className="flex items-center"
          style={{ borderBottom: i < arr.length - 1 ? '1px solid #E2F5EF' : 'none' }}>
          <div className="w-[180px] px-5 py-3 text-[13px] font-semibold flex-shrink-0"
            style={{ backgroundColor: '#F0FAF7', color: '#0D5C47', alignSelf: 'stretch', display: 'flex', alignItems: 'center' }}>
            {label}
          </div>
          <div className="flex-1 px-5 py-2.5">
            {readOnly
              ? <span className="text-[13px] font-medium" style={{ color: '#0D5C47' }}>{value || '-'}</span>
              : <input type={type ?? 'text'} value={value} onChange={e => setter?.(e.target.value)}
                  placeholder={placeholder} className={inputCls + ' max-w-xs'} style={inputStyle} />
            }
          </div>
        </div>
      ))}
    </div>
  )
}

/* ── 대출계약철회/완제 ── */
function WithdrawPage({ cntrId }: { cntrId: string }) {
  const [reason, setReason] = useState('NORMAL')
  const [done, setDone]     = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError]   = useState('')

  async function handleSubmit() {
    setError(''); setLoading(true)
    try {
      await api.post(`/api/loan-contracts/${cntrId}/closure`, { closureReasonCd: reason })
      setDone(true)
    } catch { setError('처리에 실패했습니다. 잔액을 확인해주세요.') }
    finally { setLoading(false) }
  }

  if (done) return (
    <div className="rounded-xl p-6 flex items-center gap-5" style={{ backgroundColor: '#F0FAF7', border: '1px solid #E2F5EF' }}>
      <div className="w-12 h-12 rounded-full flex items-center justify-center flex-shrink-0" style={{ backgroundColor: '#0D5C47' }}>
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
      </div>
      <div>
        <p className="text-[16px] font-bold mb-1" style={{ color: '#0D5C47' }}>대출 계약이 종결되었습니다.</p>
        <p className="text-[12px] text-kb-text-muted">계약 종결 처리가 완료되었습니다.</p>
      </div>
    </div>
  )

  return (
    <div>
      <InfoNotice lines={['대출 계약을 완제(조기상환) 처리합니다.', '잔여 원금 + 이자가 모두 상환된 후 계약이 종결됩니다.']} />
      <div className="rounded-xl overflow-hidden mb-5" style={{ border: '1px solid #E2F5EF' }}>
        <div className="flex items-center">
          <div className="w-[180px] px-5 py-3 text-[13px] font-semibold flex-shrink-0"
            style={{ backgroundColor: '#F0FAF7', color: '#0D5C47', alignSelf: 'stretch', display: 'flex', alignItems: 'center' }}>
            종결 유형
          </div>
          <div className="flex-1 px-5 py-3 flex gap-6">
            {([['NORMAL', '정상종결'], ['EARLY', '조기상환']] as const).map(([code, label]) => (
              <label key={code} className="flex items-center gap-1.5 cursor-pointer text-[13px]">
                <input type="radio" checked={reason === code} onChange={() => setReason(code)} style={{ accentColor: '#0D5C47' }} />
                {label}
              </label>
            ))}
          </div>
        </div>
      </div>
      {error && <p className="text-[13px] mb-4" style={{ color: '#E05555' }}>{error}</p>}
      <PrimaryBtn onClick={handleSubmit} disabled={loading}>{loading ? '처리 중...' : '완제 신청'}</PrimaryBtn>
    </div>
  )
}

/* ── 기한연장 ── */
function ExtendPage({ cntrId }: { cntrId: string }) {
  const [months, setMonths]   = useState('12')
  const [done, setDone]       = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState('')

  async function handleSubmit() {
    setError(''); setLoading(true)
    try {
      await api.post(`/api/loan-contracts/${cntrId}/maturity/extend`, { extensionMonths: Number(months) })
      setDone(true)
    } catch { setError('기한연장 처리에 실패했습니다.') }
    finally { setLoading(false) }
  }

  if (done) return (
    <div className="rounded-xl p-6 flex items-center gap-5" style={{ backgroundColor: '#F0FAF7', border: '1px solid #E2F5EF' }}>
      <div className="w-12 h-12 rounded-full flex items-center justify-center flex-shrink-0" style={{ backgroundColor: '#0D5C47' }}>
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
      </div>
      <div>
        <p className="text-[16px] font-bold mb-1" style={{ color: '#0D5C47' }}>기한연장이 완료되었습니다.</p>
        <p className="text-[12px] text-kb-text-muted">{months}개월 연장 처리되었습니다.</p>
      </div>
    </div>
  )

  return (
    <div>
      <InfoNotice lines={['대출 만기일을 연장합니다.', '연장 수수료가 발생할 수 있습니다. 사전에 영업점으로 문의하세요.']} />
      {SimpleForm([
        { label: '계약번호',   value: cntrId, readOnly: true },
        { label: '연장 기간',  value: months, setter: setMonths, placeholder: '개월 수 입력', type: 'number' },
      ])}
      {error && <p className="text-[13px] mb-4" style={{ color: '#E05555' }}>{error}</p>}
      <PrimaryBtn onClick={handleSubmit} disabled={loading}>{loading ? '처리 중...' : '기한연장 신청'}</PrimaryBtn>
    </div>
  )
}

/* ── 금리인하요구권 ── */
function RateCutPage({ cntrId }: { cntrId: string }) {
  const [reason, setReason] = useState('')
  const [done, setDone]     = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError]   = useState('')

  async function handleSubmit() {
    if (!reason) { setError('인하 요구 사유를 입력해주세요.'); return }
    setError(''); setLoading(true)
    try {
      await api.post(`/api/loan-contracts/${cntrId}/rate-changes`, { changeReason: reason })
      setDone(true)
    } catch { setError('신청 처리에 실패했습니다.') }
    finally { setLoading(false) }
  }

  if (done) return (
    <div className="rounded-xl p-6 flex items-center gap-5" style={{ backgroundColor: '#F0FAF7', border: '1px solid #E2F5EF' }}>
      <div className="w-12 h-12 rounded-full flex items-center justify-center flex-shrink-0" style={{ backgroundColor: '#0D5C47' }}>
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
      </div>
      <div>
        <p className="text-[16px] font-bold mb-1" style={{ color: '#0D5C47' }}>금리인하요구권 신청이 완료되었습니다.</p>
        <p className="text-[12px] text-kb-text-muted">신청 후 10영업일 이내에 결과를 통보받습니다.</p>
      </div>
    </div>
  )

  return (
    <div>
      <InfoNotice lines={[
        '신용 상태 개선(신용점수 상승, 소득 증가 등) 시 금리 인하를 요구할 수 있습니다.',
        '신청 후 10영업일 이내에 결과를 통보받습니다.',
      ]} />
      <div className="rounded-xl overflow-hidden mb-5" style={{ border: '1px solid #E2F5EF' }}>
        <div className="flex items-start">
          <div className="w-[180px] px-5 py-3 text-[13px] font-semibold flex-shrink-0"
            style={{ backgroundColor: '#F0FAF7', color: '#0D5C47', paddingTop: '14px' }}>
            인하 요구 사유
          </div>
          <div className="flex-1 px-5 py-2.5">
            <textarea value={reason} onChange={e => setReason(e.target.value)}
              placeholder="예: 신용점수 상승, 소득 증가, 부채 감소 등"
              rows={4} className="border rounded-lg px-3 py-2 text-[13px] outline-none focus:ring-1 resize-none w-full"
              style={{ borderColor: '#D1D5DB' }} />
          </div>
        </div>
      </div>
      {error && <p className="text-[13px] mb-4" style={{ color: '#E05555' }}>{error}</p>}
      <PrimaryBtn onClick={handleSubmit} disabled={loading}>{loading ? '신청 중...' : '신청'}</PrimaryBtn>
    </div>
  )
}

/* ── 연체정보조회 ── */
function DelinquencyPage({ cntrId }: { cntrId: string }) {
  const [rows, setRows]       = useState<{ overdueDays: number; overdueAmount: number; penaltyRate: number }[]>([])
  const [searched, setSearched] = useState(false)

  async function handleSearch() {
    try {
      const res = await api.get(`/api/loan-contracts/${cntrId}/delinquency`)
      setRows(res.data.data?.items ?? [])
    } catch { setRows([]) }
    setSearched(true)
  }

  return (
    <div>
      <InfoNotice lines={['대출 연체 현황을 조회합니다.', '연체 발생 시 즉시 납입하여 추가 불이익을 방지하세요.']} />
      {SimpleForm([{ label: '계약번호', value: cntrId, readOnly: true }])}
      <div className="flex justify-center mb-6"><PrimaryBtn onClick={handleSearch}>조회</PrimaryBtn></div>
      {searched && (
        <div className="rounded-xl overflow-hidden" style={{ border: '1px solid #E2F5EF' }}>
          <table className="w-full text-[13px]">
            <thead>
              <tr style={{ backgroundColor: '#F0FAF7', borderBottom: '2px solid #E2F5EF' }}>
                {['연체일수', '연체금액', '연체이율'].map(h => (
                  <th key={h} className="px-4 py-3 text-center font-semibold text-[12px]" style={{ color: '#0D5C47' }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 ? (
                <tr><td colSpan={3} className="px-4 py-8 text-center text-[13px] text-kb-text-muted">연체 내역이 없습니다.</td></tr>
              ) : rows.map((r, i, arr) => (
                <tr key={i} className="hover:bg-[#F8FFFE]" style={{ borderBottom: i < arr.length - 1 ? '1px solid #E2F5EF' : 'none' }}>
                  <td className="px-4 py-3 text-center">{r.overdueDays}일</td>
                  <td className="px-4 py-3 text-right pr-5 font-medium" style={{ color: '#E05555' }}>{r.overdueAmount.toLocaleString('ko-KR')}원</td>
                  <td className="px-4 py-3 text-center">연 {(r.penaltyRate / 100).toFixed(2)}%</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

/* ── 증명서 발급 ── */
function CertificatePage({ cntrId }: { cntrId: string }) {
  const CERT_TYPES = [
    { code: 'BALANCE',   label: '대출잔액증명서' },
    { code: 'DEBT',      label: '채무확인서' },
    { code: 'REPAYMENT', label: '상환내역증명서' },
  ]
  const [certType, setCertType] = useState('BALANCE')
  const [done, setDone]         = useState(false)
  const [loading, setLoading]   = useState(false)
  const [error, setError]       = useState('')

  async function handleIssue() {
    setError(''); setLoading(true)
    try {
      await api.post(`/api/loan-contracts/${cntrId}/certificates`, { certificateTypeCd: certType })
      setDone(true)
    } catch { setError('증명서 발급에 실패했습니다.') }
    finally { setLoading(false) }
  }

  if (done) return (
    <div className="rounded-xl p-6 flex items-center gap-5" style={{ backgroundColor: '#F0FAF7', border: '1px solid #E2F5EF' }}>
      <div className="w-12 h-12 rounded-full flex items-center justify-center flex-shrink-0" style={{ backgroundColor: '#0D5C47' }}>
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
      </div>
      <div>
        <p className="text-[16px] font-bold mb-1" style={{ color: '#0D5C47' }}>증명서가 발급되었습니다.</p>
        <p className="text-[12px] text-kb-text-muted">{CERT_TYPES.find(c => c.code === certType)?.label} 발급이 완료되었습니다.</p>
      </div>
    </div>
  )

  return (
    <div>
      <InfoNotice lines={['대출 관련 증명서를 발급합니다.', '발급된 증명서는 마이페이지에서 다운로드할 수 있습니다.']} />
      <div className="rounded-xl overflow-hidden mb-5" style={{ border: '1px solid #E2F5EF' }}>
        <div className="flex items-center" style={{ borderBottom: '1px solid #E2F5EF' }}>
          <div className="w-[180px] px-5 py-3 text-[13px] font-semibold flex-shrink-0"
            style={{ backgroundColor: '#F0FAF7', color: '#0D5C47', alignSelf: 'stretch', display: 'flex', alignItems: 'center' }}>
            계약번호
          </div>
          <div className="flex-1 px-5 py-3">
            <span className="text-[13px] font-medium" style={{ color: '#0D5C47' }}>{cntrId || '-'}</span>
          </div>
        </div>
        <div className="flex items-center">
          <div className="w-[180px] px-5 py-3 text-[13px] font-semibold flex-shrink-0"
            style={{ backgroundColor: '#F0FAF7', color: '#0D5C47', alignSelf: 'stretch', display: 'flex', alignItems: 'center' }}>
            증명서 종류
          </div>
          <div className="flex-1 px-5 py-3 flex gap-6">
            {CERT_TYPES.map(({ code, label }) => (
              <label key={code} className="flex items-center gap-1.5 cursor-pointer text-[13px]">
                <input type="radio" checked={certType === code} onChange={() => setCertType(code)} style={{ accentColor: '#0D5C47' }} />
                {label}
              </label>
            ))}
          </div>
        </div>
      </div>
      {error && <p className="text-[13px] mb-4" style={{ color: '#E05555' }}>{error}</p>}
      <PrimaryBtn onClick={handleIssue} disabled={loading}>{loading ? '발급 중...' : '발급'}</PrimaryBtn>
    </div>
  )
}

/* ── PAGE_MAP ── */
const PAGE_MAP: Record<string, { title: string; breadcrumb: string; Component: React.FC<{ cntrId: string }> }> = {
  rate:        { title: '적용금리조회',         breadcrumb: '적용금리조회',         Component: RatePage },
  payment:     { title: '이자/월부금입금',       breadcrumb: '이자/월부금입금',       Component: PaymentPage },
  repay:       { title: '대출금상환',           breadcrumb: '대출금상환',           Component: RepayPage },
  withdraw:    { title: '대출계약철회/완제',     breadcrumb: '대출계약철회/완제',     Component: WithdrawPage },
  extend:      { title: '기한연장',             breadcrumb: '기한연장',             Component: ExtendPage },
  'rate-cut':  { title: '금리인하요구권',        breadcrumb: '금리인하요구권',        Component: RateCutPage },
  delinquency: { title: '연체정보조회',          breadcrumb: '연체정보조회',          Component: DelinquencyPage },
  certificate: { title: '증명서 발급',           breadcrumb: '증명서 발급',           Component: CertificatePage },
}

export default function ManagePage() {
  const params       = useParams()
  const searchParams = useSearchParams()
  const slug   = params.slug as string
  const cntrId = searchParams.get('cntrId') ?? ''
  const meta   = PAGE_MAP[slug]

  if (!meta) return (
    <main className="pb-16">
      <div className="max-w-kb-container mx-auto px-6 pt-6 flex gap-8">
        <LoanSidebar />
        <div className="flex-1 flex items-center justify-center py-20">
          <p className="text-[15px] text-kb-text-muted">페이지를 찾을 수 없습니다.</p>
        </div>
      </div>
    </main>
  )

  const { title, breadcrumb, Component } = meta

  return (
    <main className="pb-16">
      <div className="max-w-kb-container mx-auto px-6 pt-6">
        <nav className="text-[12px] text-kb-text-muted mb-4 flex items-center gap-1">
          <Link href="/" className="hover:underline">개인뱅킹</Link><span>›</span>
          <Link href="/products/loan/my" className="hover:underline">대출관리</Link><span>›</span>
          <span className="text-kb-text font-medium">{breadcrumb}</span>
        </nav>
        <div className="flex gap-8">
          <LoanSidebar />
          <div className="flex-1 min-w-0">
            <h1 className="text-[22px] font-bold text-kb-text mb-6">{title}</h1>
            <div className="border-t-2 pt-6" style={{ borderColor: '#0D5C47' }}>
              <Component cntrId={cntrId} />
            </div>
          </div>
        </div>
      </div>
    </main>
  )
}
