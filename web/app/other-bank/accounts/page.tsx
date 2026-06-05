'use client'

import Link from 'next/link'
import { useState } from 'react'

/* ── 다온은행 데모 목데이터 (KB 사이트와 분리된 독립 데이터) ── */
const DAON_ACCOUNT = {
  number: '880-21-0457-118',
  name: '다온 기업자유예금',
  holder: '김민준',
  createdAt: '2025.11.03',
  balance: 8_120_000,
  availableBalance: 8_120_000,
}

type DaonTx = {
  id: string
  datetime: string
  description: string
  sender?: string
  amount: number
  balance: number
  branch?: string
  highlight?: boolean
}

const DAON_TX: DaonTx[] = [
  { id: 'd1', datetime: '2026.06.04 14:22', description: '타행이체 입금', sender: 'AX풀뱅크 홍길동', amount: 500_000, balance: 8_120_000, branch: '인터넷', highlight: true },
  { id: 'd2', datetime: '2026.06.03 10:11', description: '카드결제대금', amount: -1_240_000, balance: 7_620_000, branch: '인터넷' },
  { id: 'd3', datetime: '2026.06.02 09:30', description: '거래처 입금', sender: '(주)한빛상사', amount: 3_300_000, balance: 8_860_000, branch: '인터넷' },
  { id: 'd4', datetime: '2026.06.01 00:05', description: '자동이체-임대료', amount: -1_500_000, balance: 5_560_000, branch: '인터넷' },
  { id: 'd5', datetime: '2026.05.29 16:40', description: '급여이체', amount: -2_800_000, balance: 7_060_000, branch: '인터넷' },
]

const DATE_PRESETS = ['1개월', '3개월', '6개월', '1년', '직접입력']
const TX_TYPE_OPTS = ['전체', '입금', '출금']

function formatNumber(n: number) {
  return n.toLocaleString('ko-KR')
}

export default function DaonAccountsPage() {
  const [datePreset, setDatePreset] = useState('1개월')
  const [txType, setTxType] = useState('전체')
  const [balanceVisible, setBalanceVisible] = useState(true)
  const [keyword, setKeyword] = useState('')

  const latestDeposit = DAON_TX.find(t => t.highlight)

  const filteredTx = DAON_TX.filter(tx => {
    if (txType === '입금' && tx.amount <= 0) return false
    if (txType === '출금' && tx.amount >= 0) return false
    if (keyword && !tx.description.includes(keyword)) return false
    return true
  })

  return (
    <div className="max-w-kb-container mx-auto px-6 py-4 pb-16">
      {/* 브레드크럼 */}
      <div className="flex justify-end mb-4 text-[12px] text-kb-text-muted gap-1">
        <span>기업뱅킹</span><span>&gt;</span>
        <span>조회</span><span>&gt;</span>
        <span className="hover:underline">계좌조회</span>
        <span>&gt;</span>
        <span className="font-semibold text-kb-text">거래내역</span>
      </div>

      {/* 타행 입금 도착 배너 */}
      {latestDeposit && (
        <div className="rounded-xl px-6 py-4 mb-5 flex items-center justify-between"
          style={{ background: 'linear-gradient(135deg, #1B3A6B 0%, #384d84 100%)' }}>
          <div className="flex items-center gap-4">
            <div className="w-11 h-11 rounded-full bg-white/15 flex items-center justify-center text-2xl">💸</div>
            <div className="text-white">
              <p className="text-[13px] text-white/80">타행 입금이 도착했어요</p>
              <p className="text-[16px] font-bold">
                {latestDeposit.sender} → {DAON_ACCOUNT.holder}님
              </p>
            </div>
          </div>
          <div className="text-right text-white">
            <p className="text-[22px] font-extrabold">+{formatNumber(latestDeposit.amount)}원</p>
            <p className="text-[12px] text-white/70">{latestDeposit.datetime}</p>
          </div>
        </div>
      )}

      {/* 계좌 정보 헤더 */}
      <div className="border border-kb-border-dark rounded-xl p-6 mb-6 bg-kb-beige-light">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-[13px] text-kb-text-muted mb-1">{DAON_ACCOUNT.name} · 예금주 {DAON_ACCOUNT.holder}</p>
            <p className="text-[18px] font-bold text-kb-text mb-1">{DAON_ACCOUNT.number}</p>
            <div className="flex gap-4 text-[12px] text-kb-text-muted">
              <span>신규일 {DAON_ACCOUNT.createdAt}</span>
            </div>
          </div>
          <div className="text-right">
            <div className="flex items-center justify-end gap-2 mb-1">
              <span className="text-[12px] text-kb-text-muted">잔액보기</span>
              <button
                onClick={() => setBalanceVisible(v => !v)}
                className="relative w-10 h-5 rounded-full transition-colors"
                style={{ backgroundColor: balanceVisible ? '#1B3A6B' : '#cbd5e1' }}
              >
                <span className={`absolute top-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform ${balanceVisible ? 'translate-x-5' : 'translate-x-0.5'}`} />
                <span className={`absolute text-[9px] font-bold text-white ${balanceVisible ? 'left-1.5 top-0.5' : 'right-1 top-0.5'}`}>
                  {balanceVisible ? 'ON' : 'OFF'}
                </span>
              </button>
            </div>
            <p className="text-[12px] text-kb-text-muted">출금가능잔액</p>
            <p className="text-[22px] font-bold text-kb-text">
              {balanceVisible ? formatNumber(DAON_ACCOUNT.availableBalance) : '●●●●●●●'}원
            </p>
            <p className="text-[12px] text-kb-text-muted mt-1">
              잔액 {balanceVisible ? formatNumber(DAON_ACCOUNT.balance) : '●●●●●●●'}원
            </p>
          </div>
        </div>

        {/* 계좌 액션 버튼 */}
        <div className="flex gap-2 mt-4 pt-4 border-t border-kb-border">
          <button className="border border-kb-border px-5 py-1.5 text-[12px] text-kb-text-body hover:bg-white transition-colors">
            이체
          </button>
          <button className="border border-kb-border px-5 py-1.5 text-[12px] text-kb-text-body hover:bg-white transition-colors">
            계좌관리
          </button>
          <button className="border border-kb-border px-5 py-1.5 text-[12px] text-kb-text-body hover:bg-white transition-colors">
            입금통보 신청
          </button>
        </div>
      </div>

      <h2 className="text-[18px] font-bold text-kb-text mb-4 pb-2 border-b-2 border-kb-text">거래내역 조회</h2>

      {/* 조회 조건 */}
      <div className="border border-kb-border-dark rounded-xl p-6 mb-4 space-y-3">
        <div className="flex items-center gap-4">
          <span className="text-[13px] font-medium text-kb-text w-16 flex-shrink-0">조회기간</span>
          <div className="flex gap-1">
            {DATE_PRESETS.map(p => (
              <button
                key={p}
                onClick={() => setDatePreset(p)}
                className="px-4 py-1.5 text-[12px] border rounded-lg transition-colors"
                style={datePreset === p
                  ? { backgroundColor: '#1B3A6B', borderColor: '#1B3A6B', color: '#fff', fontWeight: 700 }
                  : {}}
              >
                {p}
              </button>
            ))}
          </div>
        </div>

        <div className="flex items-center gap-4">
          <span className="text-[13px] font-medium text-kb-text w-16 flex-shrink-0">거래종류</span>
          <div className="flex gap-1">
            {TX_TYPE_OPTS.map(t => (
              <button
                key={t}
                onClick={() => setTxType(t)}
                className="px-4 py-1.5 text-[12px] border rounded-lg transition-colors"
                style={txType === t
                  ? { backgroundColor: '#1B3A6B', borderColor: '#1B3A6B', color: '#fff', fontWeight: 700 }
                  : {}}
              >
                {t}
              </button>
            ))}
          </div>
        </div>

        <div className="flex items-center gap-4">
          <span className="text-[13px] font-medium text-kb-text w-16 flex-shrink-0">내용검색</span>
          <div className="flex gap-2">
            <input
              type="text"
              value={keyword}
              onChange={e => setKeyword(e.target.value)}
              placeholder="거래 내용을 입력하세요"
              className="border border-kb-border px-3 py-1.5 text-[13px] w-60 focus:outline-none"
            />
            <button className="px-5 py-1.5 text-[12px] font-bold text-white hover:brightness-110" style={{ backgroundColor: '#1B3A6B' }}>
              조회
            </button>
          </div>
        </div>
      </div>

      {/* 거래 건수 요약 */}
      <div className="flex items-center justify-between mb-2">
        <p className="text-[12px] text-kb-text-muted">
          총 <span className="font-bold text-kb-text">{filteredTx.length}</span>건
        </p>
        <div className="flex gap-3 text-[12px] text-kb-text-muted">
          <span>
            입금합계{' '}
            <span className="font-medium text-kb-blue">
              {formatNumber(filteredTx.filter(t => t.amount > 0).reduce((s, t) => s + t.amount, 0))}원
            </span>
          </span>
          <span>
            출금합계{' '}
            <span className="font-medium text-kb-red">
              {formatNumber(Math.abs(filteredTx.filter(t => t.amount < 0).reduce((s, t) => s + t.amount, 0)))}원
            </span>
          </span>
        </div>
      </div>

      {/* 거래내역 테이블 */}
      <table className="w-full border-collapse text-[13px]">
        <thead>
          <tr className="bg-kb-beige-light border-t-2 border-kb-text">
            <th className="border border-kb-border px-4 py-2 text-left font-semibold text-kb-text">날짜/시간</th>
            <th className="border border-kb-border px-4 py-2 text-left font-semibold text-kb-text">내용</th>
            <th className="border border-kb-border px-4 py-2 text-right font-semibold text-kb-text">출금금액</th>
            <th className="border border-kb-border px-4 py-2 text-right font-semibold text-kb-text">입금금액</th>
            <th className="border border-kb-border px-4 py-2 text-right font-semibold text-kb-text">잔액</th>
            <th className="border border-kb-border px-4 py-2 text-center font-semibold text-kb-text">거래점</th>
          </tr>
        </thead>
        <tbody>
          {filteredTx.length === 0 ? (
            <tr>
              <td colSpan={6} className="border border-kb-border px-4 py-8 text-center text-kb-text-muted">
                조회된 거래내역이 없습니다.
              </td>
            </tr>
          ) : (
            filteredTx.map(tx => (
              <tr key={tx.id} className="transition-colors" style={tx.highlight ? { backgroundColor: '#eef2fb' } : {}}>
                <td className="border border-kb-border px-4 py-2.5 text-kb-text-muted whitespace-nowrap">{tx.datetime}</td>
                <td className="border border-kb-border px-4 py-2.5 text-kb-text-body">
                  <p className={tx.highlight ? 'font-bold' : ''}>{tx.description}</p>
                  {tx.sender && <p className="text-[11px] text-kb-text-muted">{tx.sender}</p>}
                </td>
                <td className="border border-kb-border px-4 py-2.5 text-right text-kb-red font-medium">
                  {tx.amount < 0 ? formatNumber(Math.abs(tx.amount)) : '-'}
                </td>
                <td className="border border-kb-border px-4 py-2.5 text-right text-kb-blue font-medium">
                  {tx.amount > 0 ? formatNumber(tx.amount) : '-'}
                </td>
                <td className="border border-kb-border px-4 py-2.5 text-right text-kb-text">
                  {formatNumber(tx.balance)}
                </td>
                <td className="border border-kb-border px-4 py-2.5 text-center text-kb-text-muted">
                  {tx.branch ?? '-'}
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>

      <div className="mt-4 flex justify-center">
        <Link href="/other-bank" className="border border-kb-border px-8 py-2 text-[13px] text-kb-text-body hover:bg-kb-beige-light transition-colors">
          홈으로
        </Link>
      </div>
    </div>
  )
}
