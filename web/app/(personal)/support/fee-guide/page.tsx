'use client'
import { KB_MINT,KB_PRIMARY,KB_PRIMARY_BG } from '@/lib/theme'

import Link from 'next/link'

const FEE_TABLES = [
  {
    title: '계좌이체 수수료',
    headers: ['구분', '이체 금액', '수수료'],
    rows: [
      ['AXful → AXful (자행)', '전 금액', '무료'],
      ['AXful → 타행 (인터넷뱅킹)', '5만원 미만', '500원'],
      ['AXful → 타행 (인터넷뱅킹)', '5만원 이상', '무료'],
      ['AXful → 타행 (모바일뱅킹)', '전 금액', '무료'],
    ],
  },
  {
    title: '수수료 면제 대상',
    headers: ['대상', '면제 조건', '비고'],
    rows: [
      ['AXful클럽 VVIP·VIP', '타행이체 수수료 전액 면제', '등급 유지 기간 적용'],
      ['AXful클럽 그랜드·베스트', '타행이체 수수료 전액 면제', '등급 유지 기간 적용'],
      ['개인 및 개인사업자', '타행이체 수수료 전액 면제', '상시 적용'],
      ['이체한도 면제횟수 보유 계좌', '잔여 면제횟수 내 면제', '상품별 상이'],
    ],
  },
  {
    title: '기타 서비스 수수료',
    headers: ['서비스', '수수료', '비고'],
    rows: [
      ['증명서 발급 (인터넷)', '무료', '-'],
      ['잔액증명서 발급', '무료', '-'],
      ['거래내역 조회', '무료', '-'],
      ['자동이체 등록/변경', '무료', '-'],
    ],
  },
]

const NOTES = [
  '수수료는 출금 계좌 기준으로 부과됩니다.',
  '수수료 면제 대상 여부는 이체 확인 화면에서 확인하실 수 있습니다.',
  '공과금·카드대금 등 지로이체의 경우 별도 수수료가 적용될 수 있습니다.',
  '수수료는 금융환경 변화에 따라 변경될 수 있으며, 변경 시 사전 공지합니다.',
  '보다 자세한 수수료 안내는 고객센터(1588-0000)로 문의하시기 바랍니다.',
]

export default function FeeGuidePage() {
  return (
    <div className="max-w-kb-container mx-auto px-6 py-8">
      {/* 브레드크럼 */}
      <div className="flex items-center gap-1 text-[12px] text-kb-text-muted mb-6">
        <Link href="/" className="hover:underline">홈</Link>
        <span>›</span>
        <span>고객지원</span>
        <span>›</span>
        <span className="font-semibold text-kb-text">이용수수료 안내</span>
      </div>

      <h1 className="text-[24px] font-bold text-kb-text mb-2">이용수수료 안내</h1>
      <p className="text-[14px] text-kb-text-muted mb-8">AXful Bank 인터넷뱅킹 서비스 이용 시 적용되는 수수료를 안내해 드립니다.</p>

      <div className="space-y-6">
        {FEE_TABLES.map(table => (
          <div key={table.title} className="rounded-xl overflow-hidden border border-kb-border">
            <div className="px-6 py-3 font-semibold text-[15px] text-white" style={{ backgroundColor: KB_PRIMARY }}>
              {table.title}
            </div>
            <table className="w-full text-[14px]">
              <thead>
                <tr style={{ backgroundColor: KB_PRIMARY_BG }}>
                  {table.headers.map(h => (
                    <th key={h} className="px-6 py-3 text-left font-semibold text-kb-text border-b border-kb-border">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {table.rows.map((row, i) => (
                  <tr key={i} className="border-b border-kb-border last:border-b-0 hover:bg-kb-primary-surface transition-colors">
                    {row.map((cell, j) => (
                      <td key={j} className={`px-6 py-3.5 text-kb-text-body ${cell === '무료' ? 'font-semibold' : ''}`}
                        style={cell === '무료' ? { color: KB_PRIMARY } : {}}>
                        {cell}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))}

        {/* 유의사항 */}
        <div className="rounded-xl border border-kb-border overflow-hidden">
          <div className="px-6 py-3 font-semibold text-[15px] text-white" style={{ backgroundColor: KB_PRIMARY }}>
            유의사항
          </div>
          <ul className="px-6 py-4 space-y-2">
            {NOTES.map((note, i) => (
              <li key={i} className="flex items-start gap-2 text-[14px] text-kb-text-body">
                <span className="mt-1.5 w-1.5 h-1.5 rounded-full flex-shrink-0" style={{ backgroundColor: KB_MINT }} />
                {note}
              </li>
            ))}
          </ul>
        </div>
      </div>

      {/* 하단 바로가기 */}
      <div className="mt-8 flex gap-3">
        <Link href="/transfer/account"
          className="px-5 py-2.5 text-[14px] font-semibold rounded-lg border-2 transition-colors hover:bg-kb-primary-bg"
          style={{ borderColor: KB_PRIMARY, color: KB_PRIMARY }}>
          계좌이체 바로가기
        </Link>
        <Link href="/support/internet-banking-guide"
          className="px-5 py-2.5 text-[14px] font-semibold rounded-lg border-2 transition-colors hover:bg-kb-primary-bg"
          style={{ borderColor: KB_PRIMARY, color: KB_PRIMARY }}>
          인터넷뱅킹 이용안내
        </Link>
        <Link href="tel:15880000"
          className="px-5 py-2.5 text-[14px] font-bold text-white rounded-lg hover:opacity-85 transition-opacity"
          style={{ backgroundColor: KB_PRIMARY }}>
          고객센터 1588-0000
        </Link>
      </div>
    </div>
  )
}
