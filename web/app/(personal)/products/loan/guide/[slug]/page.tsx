'use client'

import Link from 'next/link'
import { use } from 'react'
import LoanSidebar from '@/components/inquiry/LoanSidebar'

type PageMeta = { title: string; breadcrumb: string; content: React.ReactNode }

function RateTable() {
  const rows = [
    { product: '신용대출 (직장인)', min: '4.50', max: '8.90', avg: '6.10', date: '2026.05.25' },
    { product: '신용대출 (전문직)', min: '3.90', max: '6.50', avg: '5.10', date: '2026.05.25' },
    { product: '아파트담보대출',    min: '3.50', max: '5.90', avg: '4.60', date: '2026.05.25' },
    { product: '전세자금대출',      min: '3.50', max: '5.20', avg: '4.20', date: '2026.05.25' },
    { product: '자동차대출 (신차)', min: '4.50', max: '7.90', avg: '5.80', date: '2026.05.25' },
  ]
  return (
    <div>
      <p className="text-[13px] text-kb-text-muted mb-4">단위: 연%, 2026.05.25 기준 / 세전 / 우대금리 미포함</p>
      <table className="w-full text-[13px] border-t border-kb-text">
        <thead>
          <tr className="bg-[#F5F5F5]">
            <th className="px-4 py-3 text-left font-medium border-b border-kb-border">상품명</th>
            <th className="px-4 py-3 text-center font-medium border-b border-kb-border">최저금리</th>
            <th className="px-4 py-3 text-center font-medium border-b border-kb-border">최고금리</th>
            <th className="px-4 py-3 text-center font-medium border-b border-kb-border">평균금리</th>
            <th className="px-4 py-3 text-center font-medium border-b border-kb-border">기준일</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-kb-border">
          {rows.map(r => (
            <tr key={r.product} className="hover:bg-kb-beige-light">
              <td className="px-4 py-3">{r.product}</td>
              <td className="px-4 py-3 text-center">연 {r.min}%</td>
              <td className="px-4 py-3 text-center">연 {r.max}%</td>
              <td className="px-4 py-3 text-center font-bold" style={{ color: '#C09B3A' }}>연 {r.avg}%</td>
              <td className="px-4 py-3 text-center text-kb-text-muted">{r.date}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function FeeTable() {
  const rows = [
    { fee: '인지세', desc: '대출금액에 따라 부과 (5천만원 이하 면제)', amount: '최대 35만원' },
    { fee: '근저당권 설정비', desc: '담보대출 설정 시', amount: '실비 (등기소 수수료)' },
    { fee: '감정평가 수수료', desc: '담보 감정평가 시', amount: '실비 (감정평가사 수수료)' },
    { fee: '중도상환수수료', desc: '만기 전 상환 시 (3년 경과 후 면제)', amount: '대출금액의 0.5~1.5%' },
    { fee: '계좌 관리 수수료', desc: '한도형 대출 유지비', amount: '없음' },
  ]
  return (
    <table className="w-full text-[13px] border-t border-kb-text">
      <thead>
        <tr className="bg-[#F5F5F5]">
          <th className="px-4 py-3 text-left font-medium border-b border-kb-border">수수료 종류</th>
          <th className="px-4 py-3 text-left font-medium border-b border-kb-border">부과 사유</th>
          <th className="px-4 py-3 text-center font-medium border-b border-kb-border">금액</th>
        </tr>
      </thead>
      <tbody className="divide-y divide-kb-border">
        {rows.map(r => (
          <tr key={r.fee} className="hover:bg-kb-beige-light">
            <td className="px-4 py-3 font-medium">{r.fee}</td>
            <td className="px-4 py-3 text-kb-text-muted">{r.desc}</td>
            <td className="px-4 py-3 text-center">{r.amount}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

function TextContent({ sections }: { sections: { heading: string; body: string }[] }) {
  return (
    <div className="space-y-6 max-w-2xl text-[13px] text-kb-text-body leading-relaxed">
      {sections.map(s => (
        <div key={s.heading}>
          <h3 className="font-bold text-kb-text mb-2">{s.heading}</h3>
          <p>{s.body}</p>
        </div>
      ))}
    </div>
  )
}

function LateFeeTable() {
  const rows = [
    { period: '1일', amount100: '273원', amount1000: '2,740원', amount5000: '13,699원' },
    { period: '7일', amount100: '1,918원', amount1000: '19,178원', amount5000: '95,890원' },
    { period: '30일', amount100: '8,219원', amount1000: '82,192원', amount5000: '410,959원' },
    { period: '90일', amount100: '24,657원', amount1000: '246,575원', amount5000: '1,232,876원' },
  ]
  return (
    <div>
      <p className="text-[13px] text-kb-text-muted mb-3">연체이율 10% 기준 예시 (원금 기준)</p>
      <table className="w-full text-[13px] border-t border-kb-text">
        <thead>
          <tr className="bg-[#F5F5F5]">
            <th className="px-4 py-3 text-center font-medium border-b border-kb-border">연체기간</th>
            <th className="px-4 py-3 text-center font-medium border-b border-kb-border">100만원</th>
            <th className="px-4 py-3 text-center font-medium border-b border-kb-border">1,000만원</th>
            <th className="px-4 py-3 text-center font-medium border-b border-kb-border">5,000만원</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-kb-border">
          {rows.map(r => (
            <tr key={r.period} className="hover:bg-kb-beige-light">
              <td className="px-4 py-3 text-center">{r.period}</td>
              <td className="px-4 py-3 text-center">{r.amount100}</td>
              <td className="px-4 py-3 text-center">{r.amount1000}</td>
              <td className="px-4 py-3 text-center text-red-600 font-medium">{r.amount5000}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

const PAGE_MAP: Record<string, PageMeta> = {
  rate: {
    title: '가계대출금리', breadcrumb: '가계대출금리',
    content: <RateTable />,
  },
  fee: {
    title: '대출관련 수수료', breadcrumb: '대출관련 수수료',
    content: <FeeTable />,
  },
  'rate-cut': {
    title: '금리인하요구권', breadcrumb: '금리인하요구권',
    content: <TextContent sections={[
      { heading: '금리인하요구권이란?', body: '대출 실행 이후 신용상태나 상환능력이 현저히 개선된 경우, 금융소비자가 금융회사에 대출금리 인하를 요구할 수 있는 권리입니다.' },
      { heading: '요구 가능 사유', body: '① 취업 또는 이직으로 소득이 증가한 경우  ② 신용점수가 상승한 경우  ③ 재산이 증가하거나 부채가 감소한 경우  ④ 기타 신용상태 또는 상환능력이 현저히 개선된 경우' },
      { heading: '신청 방법', body: '스타뱅킹, 인터넷뱅킹, 영업점 방문을 통해 신청할 수 있으며, 신청 후 10영업일 이내에 결과를 통보받습니다.' },
    ]} />,
  },
  'late-fee': {
    title: '대출연체시 지연배상금액 예시', breadcrumb: '지연배상금액 예시',
    content: <LateFeeTable />,
  },
  benefits: {
    title: '부가서비스', breadcrumb: '부가서비스',
    content: <TextContent sections={[
      { heading: '대출고객 우대 서비스', body: '대출 고객은 이체 수수료 면제, 환전 우대, 제휴 보험 할인 등의 혜택을 받을 수 있습니다.' },
      { heading: '자동이체 신청', body: '대출이자 자동이체를 신청하면 이자납부일에 지정 계좌에서 자동으로 이체됩니다. 자동이체 등록 시 0.1%p 금리 우대를 제공합니다.' },
      { heading: '대출 알림 서비스', body: '이자납부일, 만기일 등 주요 일정을 SMS 또는 카카오알림톡으로 미리 안내해드립니다.' },
    ]} />,
  },
  notice: {
    title: '내용증명 우편미수신 주요정보 안내', breadcrumb: '내용증명 우편미수신 안내',
    content: <TextContent sections={[
      { heading: '안내 목적', body: '금융회사가 발송한 내용증명 우편을 수령하지 못한 경우, 인터넷뱅킹을 통해 해당 내용을 확인하실 수 있습니다.' },
      { heading: '확인 방법', body: '대출 가이드 > 내용증명 우편미수신 주요정보 안내 화면에서 발송된 내용증명 내역을 조회하고 내용을 확인할 수 있습니다.' },
      { heading: '유의사항', body: '내용증명 우편은 법적 효력이 있는 중요 문서입니다. 내용 확인 후 필요한 조치를 취하시기 바랍니다.' },
    ]} />,
  },
  rights: {
    title: '추심관련 권리의무 및 권리구제방법 안내', breadcrumb: '추심관련 권리구제방법',
    content: <TextContent sections={[
      { heading: '채무자의 권리', body: '① 추심 연락 횟수 제한 요청권  ② 야간(오후 9시~오전 8시) 추심 거부권  ③ 특정 연락 수단 거부권  ④ 추심 금지 요청권 (사망, 실종, 파산 등)' },
      { heading: '불법 추심 금지 사항', body: '폭행·협박, 허위 사실 고지, 개인정보 무단 제공, 반복적 연락으로 심한 불안감·공포심 유발 등은 불법 추심에 해당합니다.' },
      { heading: '권리구제 방법', body: '금융감독원(1332), 한국소비자원(1372), 경찰청(112)에 신고하거나, 금융감독원 금융분쟁조정위원회에 분쟁조정을 신청할 수 있습니다.' },
    ]} />,
  },
}

export default function GuidePage({ params }: { params: Promise<{ slug: string }> }) {
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
          <span className="text-kb-text font-medium">대출 가이드</span><span>›</span>
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
