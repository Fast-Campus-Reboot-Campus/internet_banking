'use client'

import Link from 'next/link'
import { use, useState } from 'react'
import LoanSidebar from '@/components/inquiry/LoanSidebar'

type PageMeta = { title: string; breadcrumb: string; content: React.ReactNode }

function BizPlanForm() {
  const [mode, setMode] = useState<'view' | 'write'>('view')
  const mockDocs = [
    { date: '2026.03.15', type: '업체현황서', status: '검토완료', reviewer: '여신심사팀' },
    { date: '2026.01.20', type: '사업계획서', status: '검토완료', reviewer: '여신심사팀' },
  ]
  return (
    <div>
      <div className="flex gap-3 mb-6">
        <button onClick={() => setMode('view')} className={`px-6 py-2 text-[13px] font-bold border transition-colors ${mode === 'view' ? 'bg-kb-text text-white border-kb-text' : 'border-kb-border text-kb-text-muted hover:bg-kb-beige-light'}`}>조회</button>
        <button onClick={() => setMode('write')} className={`px-6 py-2 text-[13px] font-bold border transition-colors ${mode === 'write' ? 'bg-kb-text text-white border-kb-text' : 'border-kb-border text-kb-text-muted hover:bg-kb-beige-light'}`}>작성</button>
      </div>
      {mode === 'view' ? (
        <table className="w-full text-[13px] border-t border-kb-text">
          <thead>
            <tr className="bg-[#F5F5F5]">
              <th className="px-4 py-3 text-center font-medium border-b border-kb-border">제출일</th>
              <th className="px-4 py-3 text-center font-medium border-b border-kb-border">서류 유형</th>
              <th className="px-4 py-3 text-center font-medium border-b border-kb-border">처리 상태</th>
              <th className="px-4 py-3 text-center font-medium border-b border-kb-border">검토 부서</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-kb-border">
            {mockDocs.map((d, i) => (
              <tr key={i} className="hover:bg-kb-beige-light">
                <td className="px-4 py-3 text-center">{d.date}</td>
                <td className="px-4 py-3 text-center">{d.type}</td>
                <td className="px-4 py-3 text-center">
                  <span className="text-[11px] font-bold px-2 py-0.5 bg-[#4A7C59] text-white">{d.status}</span>
                </td>
                <td className="px-4 py-3 text-center text-kb-text-muted">{d.reviewer}</td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        <div className="max-w-lg border border-kb-border p-5 space-y-4">
          <div className="flex items-center gap-4">
            <label className="w-28 text-[13px] font-medium text-kb-text flex-shrink-0">대출 계좌</label>
            <select className="flex-1 border border-kb-border px-3 py-2 text-[13px] focus:outline-none">
              <option>AXful 직장인 신용대출 (****-3456)</option>
            </select>
          </div>
          <div className="flex items-start gap-4">
            <label className="w-28 text-[13px] font-medium text-kb-text flex-shrink-0 pt-2">업체현황 내용</label>
            <textarea rows={5} placeholder="업체 현황을 입력하세요" className="flex-1 border border-kb-border px-3 py-2 text-[13px] focus:outline-none focus:border-kb-text resize-none" />
          </div>
          <div className="flex justify-center pt-2">
            <button className="px-12 py-2.5 text-[14px] font-bold text-white" style={{ backgroundColor: '#3D3D3D' }}>제출</button>
          </div>
        </div>
      )}
    </div>
  )
}

function FatiSubmitForm() {
  return (
    <div className="max-w-lg">
      <div className="bg-[#FFF9E6] border border-[#C09B3A] p-4 mb-5 text-[13px] text-kb-text-body">
        <p className="font-bold mb-1">FATI (Financial and Tax Information) 제출 안내</p>
        <p>여신심사를 위해 재무제표, 세무자료 등을 제출합니다. 파일은 PDF 또는 Excel 형식만 업로드 가능합니다.</p>
      </div>
      <div className="border border-kb-border p-5 space-y-4">
        <div className="flex items-center gap-4">
          <label className="w-28 text-[13px] font-medium text-kb-text flex-shrink-0">대출 계좌</label>
          <select className="flex-1 border border-kb-border px-3 py-2 text-[13px] focus:outline-none">
            <option>AXful 사업자 운전자금대출 (****-5678)</option>
          </select>
        </div>
        <div className="flex items-center gap-4">
          <label className="w-28 text-[13px] font-medium text-kb-text flex-shrink-0">자료 유형</label>
          <select className="flex-1 border border-kb-border px-3 py-2 text-[13px] focus:outline-none">
            <option>재무제표 (손익계산서)</option>
            <option>재무제표 (대차대조표)</option>
            <option>부가가치세 신고서</option>
            <option>종합소득세 신고서</option>
          </select>
        </div>
        <div className="flex items-center gap-4">
          <label className="w-28 text-[13px] font-medium text-kb-text flex-shrink-0">기준연도</label>
          <select className="flex-1 border border-kb-border px-3 py-2 text-[13px] focus:outline-none">
            <option>2025년</option><option>2024년</option><option>2023년</option>
          </select>
        </div>
        <div className="flex items-center gap-4">
          <label className="w-28 text-[13px] font-medium text-kb-text flex-shrink-0">파일 첨부</label>
          <input type="file" accept=".pdf,.xlsx,.xls" className="flex-1 text-[13px]" />
        </div>
      </div>
      <div className="flex justify-center mt-5">
        <button className="px-12 py-2.5 text-[14px] font-bold text-white" style={{ backgroundColor: '#3D3D3D' }}>제출</button>
      </div>
    </div>
  )
}

function FatiHistoryTable() {
  const rows = [
    { date: '2026.04.10', type: '재무제표 (손익계산서)', year: '2025년', status: '처리완료' },
    { date: '2026.04.10', type: '재무제표 (대차대조표)', year: '2025년', status: '처리완료' },
    { date: '2025.10.05', type: '부가가치세 신고서',     year: '2024년', status: '처리완료' },
  ]
  return (
    <table className="w-full text-[13px] border-t border-kb-text">
      <thead>
        <tr className="bg-[#F5F5F5]">
          <th className="px-4 py-3 text-center font-medium border-b border-kb-border">제출일</th>
          <th className="px-4 py-3 text-left font-medium border-b border-kb-border">자료 유형</th>
          <th className="px-4 py-3 text-center font-medium border-b border-kb-border">기준연도</th>
          <th className="px-4 py-3 text-center font-medium border-b border-kb-border">처리 상태</th>
        </tr>
      </thead>
      <tbody className="divide-y divide-kb-border">
        {rows.map((r, i) => (
          <tr key={i} className="hover:bg-kb-beige-light">
            <td className="px-4 py-3 text-center">{r.date}</td>
            <td className="px-4 py-3">{r.type}</td>
            <td className="px-4 py-3 text-center">{r.year}</td>
            <td className="px-4 py-3 text-center">
              <span className="text-[11px] font-bold px-2 py-0.5 bg-[#4A7C59] text-white">{r.status}</span>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

const PAGE_MAP: Record<string, PageMeta> = {
  'biz-plan': {
    title: '「업체현황 및 사업계획서」조회 및 작성',
    breadcrumb: '업체현황 및 사업계획서',
    content: <BizPlanForm />,
  },
  'fati-submit': {
    title: '「FATI (재무 및 세무자료)」제출',
    breadcrumb: 'FATI 제출',
    content: <FatiSubmitForm />,
  },
  'fati-history': {
    title: '「FATI (재무 및 세무자료)」제출내역조회',
    breadcrumb: 'FATI 제출내역조회',
    content: <FatiHistoryTable />,
  },
}

export default function CreditEvalPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = use(params)
  const meta = PAGE_MAP[slug]

  if (!meta) {
    return (
      <main className="pb-16">
        <div className="max-w-kb-container mx-auto px-6 pt-6">
          <div className="flex gap-8">
            <LoanSidebar />
            <div className="flex-1 flex items-center justify-center py-20">
              <p className="text-[15px] text-kb-text-muted">페이지를 찾을 수 없습니다.</p>
            </div>
          </div>
        </div>
      </main>
    )
  }

  return (
    <main className="pb-16">
      <div className="max-w-kb-container mx-auto px-6 pt-6">
        <nav className="text-[12px] text-kb-text-muted mb-4 flex items-center gap-1">
          <Link href="/personal" className="hover:underline">개인뱅킹</Link><span>›</span>
          <Link href="/products/deposit" className="hover:underline">금융상품</Link><span>›</span>
          <Link href="/products/loan" className="hover:underline">대출</Link><span>›</span>
          <span className="text-kb-text font-medium">신용평가 및 여신심사 자료제출</span><span>›</span>
          <span className="text-kb-text font-medium">{meta.breadcrumb}</span>
        </nav>
        <div className="flex gap-8">
          <LoanSidebar />
          <div className="flex-1 min-w-0">
            <h1 className="text-[26px] font-bold text-kb-text mb-6">{meta.title}</h1>
            <div className="border-t border-kb-text pt-6">
              {meta.content}
            </div>
          </div>
        </div>
      </div>
    </main>
  )
}
