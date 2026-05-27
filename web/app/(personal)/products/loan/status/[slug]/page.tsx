'use client'

import Link from 'next/link'
import { use } from 'react'
import LoanSidebar from '@/components/inquiry/LoanSidebar'

type PageMeta = { title: string; breadcrumb: string; content: React.ReactNode }

function ConsentForm({ title, fields }: { title: string; fields: string[] }) {
  return (
    <div className="max-w-lg">
      <div className="bg-[#F5F5F5] border border-kb-border p-4 mb-5 text-[13px] text-kb-text-body">
        <p className="font-bold mb-1">{title} 동의</p>
        <p>금융거래 목적으로 개인(신용)정보를 제3자에게 제공하는 것에 동의합니다.</p>
      </div>
      <div className="border border-kb-border p-5 space-y-4">
        {fields.map(field => (
          <div key={field} className="flex items-center gap-4">
            <label className="w-28 text-[13px] font-medium text-kb-text flex-shrink-0">{field}</label>
            <input type="text" placeholder="입력하세요" className="flex-1 border border-kb-border px-3 py-2 text-[13px] focus:outline-none focus:border-kb-text" />
          </div>
        ))}
      </div>
      <div className="flex justify-center mt-5">
        <button className="px-12 py-2.5 text-[14px] font-bold text-white" style={{ backgroundColor: '#3D3D3D' }}>동의 제출</button>
      </div>
    </div>
  )
}

function DocsUpload() {
  const docs = [
    { name: '재직증명서', required: true,  status: '미제출' },
    { name: '소득확인서류', required: true,  status: '미제출' },
    { name: '건강보험료 납부확인서', required: false, status: '미제출' },
  ]
  return (
    <div>
      <p className="text-[13px] text-kb-text-muted mb-4">대출 실행 후 요구된 사후 서류를 제출합니다.</p>
      <table className="w-full text-[13px] border-t border-kb-text mb-5">
        <thead>
          <tr className="bg-[#F5F5F5]">
            <th className="px-4 py-3 text-left font-medium border-b border-kb-border">서류명</th>
            <th className="px-4 py-3 text-center font-medium border-b border-kb-border">필수여부</th>
            <th className="px-4 py-3 text-center font-medium border-b border-kb-border">제출상태</th>
            <th className="px-4 py-3 text-center font-medium border-b border-kb-border">업로드</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-kb-border">
          {docs.map(d => (
            <tr key={d.name} className="hover:bg-kb-beige-light">
              <td className="px-4 py-3">{d.name}</td>
              <td className="px-4 py-3 text-center">
                <span className={`text-[11px] font-bold px-2 py-0.5 ${d.required ? 'bg-red-500 text-white' : 'bg-kb-border text-kb-text-muted'}`}>{d.required ? '필수' : '선택'}</span>
              </td>
              <td className="px-4 py-3 text-center text-kb-text-muted">{d.status}</td>
              <td className="px-4 py-3 text-center">
                <input type="file" className="text-[11px]" />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="flex justify-center">
        <button className="px-12 py-2.5 text-[14px] font-bold text-white" style={{ backgroundColor: '#3D3D3D' }}>제출</button>
      </div>
    </div>
  )
}

function ESignForm() {
  return (
    <div className="max-w-lg">
      <div className="bg-[#FFF9E6] border border-[#C09B3A] p-4 mb-5 text-[13px] text-kb-text-body">
        <p className="font-bold mb-1">부동산담보대출 전자서명 안내</p>
        <p>근저당권 설정 등기를 위한 전자서명을 진행합니다. 공동인증서 또는 금융인증서가 필요합니다.</p>
      </div>
      <div className="border border-kb-border p-5 space-y-4">
        <div className="flex items-center gap-4">
          <label className="w-28 text-[13px] font-medium text-kb-text flex-shrink-0">대출 계좌</label>
          <select className="flex-1 border border-kb-border px-3 py-2 text-[13px] focus:outline-none">
            <option>AXful 아파트담보대출 (****-7890)</option>
          </select>
        </div>
        <div className="flex items-center gap-4">
          <label className="w-28 text-[13px] font-medium text-kb-text flex-shrink-0">담보물건</label>
          <input type="text" placeholder="담보 부동산 주소 입력" className="flex-1 border border-kb-border px-3 py-2 text-[13px] focus:outline-none focus:border-kb-text" />
        </div>
        <div className="flex items-center gap-4">
          <label className="w-28 text-[13px] font-medium text-kb-text flex-shrink-0">인증 수단</label>
          <select className="flex-1 border border-kb-border px-3 py-2 text-[13px] focus:outline-none">
            <option>금융인증서</option>
            <option>공동인증서 (구 공인인증서)</option>
          </select>
        </div>
      </div>
      <div className="flex justify-center mt-5">
        <button className="px-12 py-2.5 text-[14px] font-bold text-white" style={{ backgroundColor: '#3D3D3D' }}>전자서명 진행</button>
      </div>
    </div>
  )
}

const PAGE_MAP: Record<string, PageMeta> = {
  docs: {
    title: '사후서류제출', breadcrumb: '사후서류제출',
    content: <DocsUpload />,
  },
  spouse: {
    title: '배우자정보제공동의', breadcrumb: '배우자정보제공동의',
    content: <ConsentForm title="배우자정보제공" fields={['배우자 성명', '배우자 주민번호', '배우자 연락처']} />,
  },
  household: {
    title: '세대원정보제공동의', breadcrumb: '세대원정보제공동의',
    content: <ConsentForm title="세대원정보제공" fields={['세대원 성명', '세대원 주민번호', '관계']} />,
  },
  collateral: {
    title: '제3자담보정보제공동의', breadcrumb: '제3자담보정보제공동의',
    content: <ConsentForm title="제3자담보정보제공" fields={['담보 제공자 성명', '담보 제공자 주민번호', '담보 제공자 연락처']} />,
  },
  sign: {
    title: '부동산담보대출 전자서명', breadcrumb: '부동산담보대출 전자서명',
    content: <ESignForm />,
  },
}

export default function StatusSlugPage({ params }: { params: Promise<{ slug: string }> }) {
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
          <Link href="/products/loan/status" className="hover:underline">대출진행현황</Link><span>›</span>
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
