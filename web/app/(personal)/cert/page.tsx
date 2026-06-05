'use client'
import { KB_PRIMARY } from '@/lib/theme'

import Link from 'next/link'

export default function CertPage() {
  return (
    <div className="max-w-kb-container mx-auto px-6 py-8">

      {/* 브레드크럼 */}
      <div className="flex items-center gap-1 text-[12px] text-kb-text-muted mb-6">
        <span>인증센터(개인)</span>
        <span>›</span>
        <span className="font-semibold text-kb-text">인증서 발급 안내</span>
      </div>

      {/* 페이지 제목 */}
      <h1 className="text-[24px] font-bold text-kb-text mb-2">인증서 발급 안내</h1>
      <p className="text-[14px] text-kb-text-muted mb-8">AXful Bank 인터넷뱅킹 서비스에서 이용 가능한 인증서를 안내해 드립니다.</p>

      {/* 안내 문구 */}
      <div className="border border-kb-border bg-kb-beige-light px-5 py-4 space-y-1.5 text-[13px] text-kb-text-body mb-6">
        <p>· 인증서는 온라인 상에서 모든 전자거래를 안전하고 편리하게 이용할 수 있도록 하는 온라인 인감증명서입니다.</p>
        <p>· 발급당일 및 용도에 맞는 인증서를 발급하여 AXful 인터넷뱅킹 서비스를 이용하실 수 있습니다.</p>
        <p>· 인증서 신규발급·재발급·갱신 시 발급증을 포함한 4일 동안 AXful뱅킹·인터넷뱅킹에서 이체신청을 할 수 없습니다.</p>
      </div>

      {/* 발급 안내 테이블 */}
      <div className="overflow-x-auto border border-kb-border rounded-xl">
        <table className="w-full border-collapse text-[13px]">
          <thead>
            <tr className="bg-kb-beige-light">
              {['구분', '인증서 종류', '발급대상', '수수료', '용도'].map(h => (
                <th key={h}
                  className="px-4 py-3 text-center font-bold text-kb-text border-b-2 border-kb-primary border-r last:border-r-0 whitespace-nowrap">
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-kb-border">

            {/* AXful인증서 */}
            <tr className="hover:bg-kb-beige-light/50">
              <td className="px-4 py-4 text-center font-bold text-kb-text border-r border-kb-border whitespace-nowrap" rowSpan={3}>
                개인
              </td>
              <td className="px-4 py-4 border-r border-kb-border text-center">
                <div className="flex flex-col items-center gap-1.5">
                  <span className="font-bold text-kb-text">AXful인증서</span>
                  <Link href="/cert/axful-cert-issue"
                    className="text-[11px] font-bold text-white px-3 py-0.5 rounded-sm"
                    style={{ backgroundColor: KB_PRIMARY }}>
                    발급
                  </Link>
                </div>
              </td>
              <td className="px-4 py-4 border-r border-kb-border text-[13px] text-kb-text-body leading-relaxed">
                AXful 인터넷뱅킹을 가입한<br />개인 고객
              </td>
              <td className="px-4 py-4 border-r border-kb-border text-center font-medium text-kb-primary">
                무료
              </td>
              <td className="px-4 py-4 text-kb-text-body leading-relaxed">
                <ul className="space-y-0.5">
                  {['온라인 은행 금융거래', '모든 금융거래', '전자정부 민원서비스', '다양한 제휴 서비스'].map(u => (
                    <li key={u} className="flex items-start gap-1"><span className="text-kb-primary mt-0.5">·</span>{u}</li>
                  ))}
                </ul>
              </td>
            </tr>

            {/* 금융인증서 */}
            <tr className="hover:bg-kb-beige-light/50">
              <td className="px-4 py-4 border-r border-kb-border text-center">
                <div className="flex flex-col items-center gap-1.5">
                  <span className="font-bold text-kb-text">금융인증서</span>
                  <Link href="/cert/fin-cert-issue"
                    className="text-[11px] font-bold text-white px-3 py-0.5 rounded-sm"
                    style={{ backgroundColor: KB_PRIMARY }}>
                    발급
                  </Link>
                </div>
              </td>
              <td className="px-4 py-4 border-r border-kb-border text-[13px] text-kb-text-body leading-relaxed">
                AXful 인터넷뱅킹을 가입한<br />개인 고객
              </td>
              <td className="px-4 py-4 border-r border-kb-border text-center font-medium text-kb-primary">
                무료
              </td>
              <td className="px-4 py-4 text-kb-text-body leading-relaxed">
                <ul className="space-y-0.5">
                  {['온라인 은행 금융거래', '보험·증거래 등 모든 금융거래', '전자정부 민원서비스'].map(u => (
                    <li key={u} className="flex items-start gap-1"><span className="text-kb-primary mt-0.5">·</span>{u}</li>
                  ))}
                </ul>
              </td>
            </tr>

            {/* 공동인증서 */}
            <tr className="hover:bg-kb-beige-light/50">
              <td className="px-4 py-4 border-r border-kb-border text-center">
                <div className="flex flex-col items-center gap-1.5">
                  <span className="font-bold text-kb-text">공동인증서</span>
                  <span className="text-[11px] text-kb-text-muted">(구 공인인증서)</span>
                  <Link href="/cert/joint-cert-issue"
                    className="text-[11px] font-bold text-white px-3 py-0.5 rounded-sm"
                    style={{ backgroundColor: KB_PRIMARY }}>
                    발급
                  </Link>
                </div>
              </td>
              <td className="px-4 py-4 border-r border-kb-border text-[13px] text-kb-text-body leading-relaxed">
                AXful 인터넷뱅킹을 가입한<br />개인 고객
              </td>
              <td className="px-4 py-4 border-r border-kb-border text-center">
                <p className="font-medium text-kb-text-body">4,400원/년</p>
                <p className="text-[11px] text-kb-text-muted">(부가세포함)</p>
              </td>
              <td className="px-4 py-4 text-kb-text-body leading-relaxed">
                <ul className="space-y-0.5">
                  {['온라인 은행 금융거래', '보험·증거래 등 모든 금융거래', '전자정부 민원서비스'].map(u => (
                    <li key={u} className="flex items-start gap-1"><span className="text-kb-primary mt-0.5">·</span>{u}</li>
                  ))}
                </ul>
              </td>
            </tr>

          </tbody>
        </table>
      </div>

    </div>
  )
}
