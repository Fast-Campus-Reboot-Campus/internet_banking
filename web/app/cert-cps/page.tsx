'use client'

import { useState } from 'react'

const TOC = [
  { num: '1', label: '소 개' },
  { num: '2', label: '전자서명인증업무 관련 정보의 공고' },
  { num: '3', label: '신원확인' },
  { num: '4', label: '인증서 관리' },
  { num: '5', label: '시설 및 운영관리' },
  { num: '6', label: '기술적 보호조치' },
  { num: '7', label: '인증서 형식' },
  { num: '8', label: '감사 및 평가' },
  { num: '9', label: '전자서명인증업무 보증 등 기타사항' },
  { num: '부록 A', label: '프로파일' },
  { num: '부록 B', label: '개정이력' },
]

const SECTIONS = [
  {
    num: '1',
    title: '소개',
    content: (
      <div className="space-y-6">
        <section>
          <h3 className="text-[19px] font-bold mb-3">1.1 개요</h3>
          <h4 className="text-[19px] font-bold mb-2">1.1.1 배경 및 목적</h4>
          <p className="text-[19px] text-gray-700 leading-relaxed mb-4">
            AXful인증서 인증업무준칙(CPS: Certification Practice Statement)은 전자서명법 및 동법 시행령, 동법 시행규칙에 의하여 주식회사 AX풀뱅크(이하 &ldquo;AX풀뱅크&rdquo;)이 개인을 대상으로 하는 AXful인증서 및 사업자(개인사업자 및 법인사업자)를 대상으로 하는 AXful인증서(기업)(이하 &ldquo;인증서 등&rdquo;)의 발급, 관리 및 인증시스템을 운영함에 있어서 필요한 사항을 정하고, AX풀뱅크과 가입자 등 인증 관련 당사자의 책임과 의무사항을 규정함을 목적으로 합니다.
          </p>
          <h4 className="text-[19px] font-bold mb-2">1.1.2 전자서명인증체계</h4>
          <p className="text-[19px] text-gray-700 leading-relaxed mb-3">
            AX풀뱅크은 전자서명인증체계를 안전하고 신뢰성 있게 운영하기 위한 정책을 수립하고 시행하는 기관으로서 최상위인증기관(ROOT CA), 인증기관(CA)으로 전자서명인증체계를 구성하여 관리합니다.
          </p>
          <div className="space-y-2 text-[19px] text-gray-700 leading-relaxed pl-4">
            <p className="font-medium">AX풀뱅크 최상위 인증기관(AXful ROOT CA)</p>
            <ul className="list-disc list-inside space-y-1 pl-2 text-gray-600">
              <li>안전한 전자서명 인증관리체계 구축 및 운영</li>
              <li>전자서명 인증기술의 개발 및 보급</li>
              <li>인증기관(CA) 검사 및 안전한 운영지원</li>
              <li>인증기관 전자서명생성정보에 대한 인증 등 인증업무 수행</li>
              <li>오프라인으로 관리 운영</li>
            </ul>
            <p className="font-medium mt-2">AX풀뱅크 인증기관(AXful CA)</p>
            <ul className="list-disc list-inside space-y-1 pl-2 text-gray-600">
              <li>개인 및 사업자의 신원 확인</li>
              <li>개인 및 사업자 인증서 발급, 재발급, 갱신, 폐지 업무</li>
              <li>인증서 유효성 확인 업무</li>
              <li>기타 인증기관으로서 수행해야 할 업무</li>
            </ul>
          </div>
        </section>

        <section>
          <h3 className="text-[19px] font-bold mb-2">1.2 문서의 명칭</h3>
          <p className="text-[19px] text-gray-700 leading-relaxed">
            본 문서의 명칭은 「AXful인증서 인증업무준칙」(이하 &ldquo;인증업무준칙&rdquo;이라 한다)으로 전자서명법, 동법 시행령, 동법 시행규칙을 준수합니다.
          </p>
        </section>

        <section>
          <h3 className="text-[19px] font-bold mb-2">1.4 인증서 종류</h3>
          <h4 className="text-[19px] font-bold mb-3">1.4.1 인증서 이용범위 및 용도</h4>
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-[16px]">
              <thead>
                <tr className="bg-gray-100">
                  <th className="border border-gray-300 px-4 py-2 text-left font-semibold">구분</th>
                  <th className="border border-gray-300 px-4 py-2 text-left font-semibold">용도</th>
                  <th className="border border-gray-300 px-4 py-2 text-left font-semibold">유효기간</th>
                </tr>
              </thead>
              <tbody className="text-gray-700">
                <tr>
                  <td className="border border-gray-300 px-4 py-2">AXful인증서</td>
                  <td className="border border-gray-300 px-4 py-2">전자서명인증이 필요한 모든 전자거래 업무에 이용</td>
                  <td className="border border-gray-300 px-4 py-2">발급일로부터 2년</td>
                </tr>
                <tr className="bg-gray-50">
                  <td className="border border-gray-300 px-4 py-2">AXful인증서(대면용)</td>
                  <td className="border border-gray-300 px-4 py-2">대면 업무용 서비스(마이데이터통합인증)를 위한 전자서명 업무</td>
                  <td className="border border-gray-300 px-4 py-2">발급 후 3시간</td>
                </tr>
                <tr>
                  <td className="border border-gray-300 px-4 py-2">AXful인증서(기업)</td>
                  <td className="border border-gray-300 px-4 py-2">전자서명인증이 필요한 모든 전자거래 업무에 이용</td>
                  <td className="border border-gray-300 px-4 py-2">발급일로부터 3년</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section>
          <h3 className="text-[19px] font-bold mb-2">1.5 준칙의 관리</h3>
          <div className="space-y-1 text-[19px] text-gray-700">
            <p><span className="font-medium">부서:</span> 인증사업부(P)</p>
            <p><span className="font-medium">이메일:</span> admin@axful.com</p>
            <p><span className="font-medium">주소:</span> 서울특별시 중구 태평로1길 1(AXful동) AX풀뱅크</p>
          </div>
        </section>
      </div>
    ),
  },
  { num: '2', title: '전자서명인증업무 관련 정보의 공고', content: null },
  { num: '3', title: '신원확인', content: null },
  { num: '4', title: '인증서 관리', content: null },
  { num: '5', title: '시설 및 운영관리', content: null },
  { num: '6', title: '기술적 보호조치', content: null },
  { num: '7', title: '인증서 형식', content: null },
  { num: '8', title: '감사 및 평가', content: null },
  { num: '9', title: '전자서명인증업무 보증 등 기타사항', content: null },
  { num: '부록 A', title: '프로파일', content: null },
  { num: '부록 B', title: '개정이력', content: null },
]

export default function CertCpsPage() {
  const [openSections, setOpenSections] = useState<Set<string>>(new Set(['1']))

  function toggle(num: string) {
    setOpenSections((prev) => {
      const next = new Set(prev)
      if (next.has(num)) { next.delete(num) } else { next.add(num) }
      return next
    })
  }

  return (
    <div>
      {/* 버전 드롭다운 */}
      <div className="flex justify-end mb-6">
        <button className="flex items-center gap-2 border border-gray-300 px-4 py-2 text-[19px] text-gray-700 hover:bg-gray-50 transition-colors">
          인증업무준칙 (Ver.1.0.14)
          <span className="text-gray-400">▼</span>
        </button>
      </div>

      {/* 제목 */}
      <h1 className="text-[34px] font-bold text-gray-900 mb-8">AXful인증서 인증업무준칙(CPS)</h1>

      {/* 목차 */}
      <div className="mb-10 space-y-1">
        {TOC.map((item) => (
          <p key={item.num} className="text-[19px] text-gray-500">
            {item.num}. {item.label}
          </p>
        ))}
      </div>

      <hr className="border-gray-200 mb-8" />

      {/* 섹션 목록 */}
      <div className="space-y-2">
        {SECTIONS.map((section) => (
          <div key={section.num} className="border border-gray-200">
            <button
              onClick={() => toggle(section.num)}
              className="w-full flex items-center justify-between px-5 py-4 text-left hover:bg-gray-50 transition-colors"
            >
              <span className="text-[19px] font-bold text-gray-800">
                {section.num}. {section.title}
              </span>
              <span className="text-gray-400 text-sm">{openSections.has(section.num) ? '▲' : '▼'}</span>
            </button>

            {openSections.has(section.num) && (
              <div className="px-6 py-5 border-t border-gray-200 bg-white">
                {section.content ?? (
                  <p className="text-[19px] text-gray-400 italic">해당 섹션의 내용은 준비 중입니다.</p>
                )}
              </div>
            )}
          </div>
        ))}
      </div>

      {/* 부칙 */}
      <div className="mt-8 border border-gray-200 px-5 py-4 space-y-1">
        <p className="text-[19px] font-bold text-gray-800">부칙</p>
        <p className="text-[19px] text-red-600">이 준칙은 2026년 02월 24일부터 시행합니다.</p>
      </div>
    </div>
  )
}
