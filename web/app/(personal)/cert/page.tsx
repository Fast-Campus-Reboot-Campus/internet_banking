'use client'

import Link from 'next/link'

<<<<<<< HEAD
const SIDEBAR_ITEMS = [
  { label: 'AXful인증서',             href: '#' },
  { label: '금융인증서',               href: '/cert/fin-cert-issue' },
  { label: '공동인증서(구 공인인증서)', href: '/cert/joint-cert-issue' },
  { label: '인증서 발급 안내',          href: '/cert', active: true },
  { label: '인증센터 FAQ',             href: '#' },
]

const CERT_ROWS = [
  {
    group: '개인',
    rowspan: 2,
    certs: [
      {
        name: 'AXful인증서',
        nameNote: '',
        badge: '발급',
        badgeHref: '/cert',
        target: 'AXful 인터넷뱅킹을 가입한\n개인 고객',
        fee: '무료',
        usage: [
          '온라인 은행 금융거래',
          '모든 금융거래',
          '전자정부 민원서비스',
          '다양한 제휴 서비스',
        ],
        href: '/cert',
      },
      {
        name: '금융인증서',
        nameNote: '',
        badge: '발급',
        badgeHref: '/cert/fin-cert-issue',
        target: 'AXful 인터넷뱅킹을 가입한\n개인 고객',
        fee: '무료',
        usage: [
          '온라인 은행 금융거래',
          '보험·증거래 등 모든 금융거래',
          '전자정부 민원서비스',
        ],
        href: '/cert/fin-cert-issue',
      },
    ],
  },
  {
    group: '개인\n(공동인증서)',
    rowspan: 1,
    certs: [
      {
        name: '공동인증서',
        nameNote: '(구 공인인증서)',
        badge: '발급',
        badgeHref: '/cert/joint-cert-issue',
        target: 'AXful 인터넷뱅킹을 가입한\n개인 고객',
        fee: '4,400원/년\n(부가세포함)',
        feeNote: '무료 발급 대상 별도 문의',
        usage: [
          '온라인 은행 금융거래',
          '보험·증거래 등 모든 금융거래',
          '전자정부 민원서비스',
        ],
        href: '/cert/joint-cert-issue',
      },
=======
const CERT_GROUPS = [
  {
    type: '금융인증서',
    desc: '금융결제원 클라우드에 저장, PC·스마트폰 어디서나 이용 가능',
    items: [
      { label: '인증서 발급/재발급', href: '/cert/fin-cert-issue', primary: true },
      { label: '인증서 관리', href: '/cert/cert-management', primary: false },
    ],
  },
  {
    type: '공동인증서',
    desc: '구 공인인증서. PC·이동식 저장장치에 저장하여 이용',
    items: [
      { label: '인증서 발급/재발급', href: '/cert/joint-cert-issue', primary: true },
      { label: '인증서 관리', href: '/cert/joint-cert-management', primary: false },
>>>>>>> fdd0eea2117b6ec92dbe3c5ed5ccf099c712793a
    ],
  },
]

export default function CertPage() {
  return (
<<<<<<< HEAD
    <div className="max-w-kb-container mx-auto px-8 py-8 flex gap-8">

      {/* 좌측 사이드바 */}
      <aside className="w-52 flex-shrink-0">
        <div className="bg-white border border-kb-border">
          <div className="px-4 py-3" style={{ backgroundColor: '#0D5C47' }}>
            <p className="text-[14px] font-bold text-white">인증센터(개인)</p>
          </div>
          {SIDEBAR_ITEMS.map((item) => (
            <Link
              key={item.label}
              href={item.href}
              className={`block px-5 py-2.5 text-[13px] border-b border-kb-border transition-colors ${
                item.active
                  ? 'bg-[#0D5C47] font-bold text-white'
                  : 'text-kb-text-body hover:bg-kb-beige-light'
              }`}
            >
              {item.label}
            </Link>
          ))}
        </div>
      </aside>

      {/* 우측 메인 */}
      <main className="flex-1 min-w-0 space-y-6">

        {/* 브레드크럼 */}
        <div className="flex items-center gap-1 text-[12px] text-kb-text-muted">
          <span>인증센터(개인)</span>
          <span>›</span>
          <span className="text-kb-text font-medium">인증서 발급 안내</span>
        </div>

        {/* 페이지 제목 */}
        <h2 className="text-[22px] font-bold text-kb-text border-b-2 border-[#0D5C47] pb-3">
          인증서 발급 안내
        </h2>

        {/* 안내 문구 */}
        <div className="border border-kb-border bg-kb-beige-light px-5 py-4 space-y-1.5 text-[13px] text-kb-text-body">
          <p>· 인증서는 온라인 상에서 모든 전자거래를 안전하고 편리하게 이용할 수 있도록 하는 온라인 인감증명서입니다.</p>
          <p>· 발급당일 및 용도에 맞는 인증서를 발급하여 AXful 인터넷뱅킹 서비스를 이용하실 수 있습니다.</p>
          <p>· 인증서 신규발급·재발급·갱신 시 발급증을 포함한 4일 동안 AXful뱅킹·인터넷뱅킹에서 이체신청을 할 수 없습니다.</p>
        </div>

        {/* 발급 안내 테이블 */}
        <div className="overflow-x-auto border border-kb-border">
          <table className="w-full border-collapse text-[13px]">
            <thead>
              <tr className="bg-kb-beige-light">
                {['구분', '인증서 종류', '발급대상', '수수료', '용도'].map(h => (
                  <th key={h}
                    className="px-4 py-3 text-center font-bold text-kb-text border-b-2 border-[#0D5C47] border-r last:border-r-0 whitespace-nowrap">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-kb-border">

              {/* 개인 — AXful인증서 */}
              <tr className="hover:bg-kb-beige-light/50">
                <td className="px-4 py-4 text-center font-bold text-kb-text border-r border-kb-border whitespace-nowrap" rowSpan={3}>
                  개인
                </td>
                <td className="px-4 py-4 border-r border-kb-border text-center">
                  <div className="flex flex-col items-center gap-1.5">
                    <span className="font-bold text-kb-text">AXful인증서</span>
                    <Link href="/cert"
                      className="text-[11px] font-bold text-white px-3 py-0.5 rounded-sm"
                      style={{ backgroundColor: '#0D5C47' }}>
                      발급
                    </Link>
                  </div>
                </td>
                <td className="px-4 py-4 border-r border-kb-border text-[13px] text-kb-text-body leading-relaxed">
                  AXful 인터넷뱅킹을 가입한<br />개인 고객
                </td>
                <td className="px-4 py-4 border-r border-kb-border text-center font-medium text-[#0D5C47]">
                  무료
                </td>
                <td className="px-4 py-4 text-kb-text-body leading-relaxed">
                  <ul className="space-y-0.5">
                    {['온라인 은행 금융거래', '모든 금융거래', '전자정부 민원서비스', '다양한 제휴 서비스'].map(u => (
                      <li key={u} className="flex items-start gap-1"><span className="text-[#0D5C47] mt-0.5">·</span>{u}</li>
                    ))}
                  </ul>
                </td>
              </tr>

              {/* 개인 — 금융인증서 */}
              <tr className="hover:bg-kb-beige-light/50">
                <td className="px-4 py-4 border-r border-kb-border text-center">
                  <div className="flex flex-col items-center gap-1.5">
                    <span className="font-bold text-kb-text">금융인증서</span>
                    <Link href="/cert/fin-cert-issue"
                      className="text-[11px] font-bold text-white px-3 py-0.5 rounded-sm"
                      style={{ backgroundColor: '#0D5C47' }}>
                      발급
                    </Link>
                  </div>
                </td>
                <td className="px-4 py-4 border-r border-kb-border text-[13px] text-kb-text-body leading-relaxed">
                  AXful 인터넷뱅킹을 가입한<br />개인 고객
                </td>
                <td className="px-4 py-4 border-r border-kb-border text-center font-medium text-[#0D5C47]">
                  무료
                </td>
                <td className="px-4 py-4 text-kb-text-body leading-relaxed">
                  <ul className="space-y-0.5">
                    {['온라인 은행 금융거래', '보험·증거래 등 모든 금융거래', '전자정부 민원서비스'].map(u => (
                      <li key={u} className="flex items-start gap-1"><span className="text-[#0D5C47] mt-0.5">·</span>{u}</li>
                    ))}
                  </ul>
                </td>
              </tr>

              {/* 개인 — 공동인증서 */}
              <tr className="hover:bg-kb-beige-light/50">
                <td className="px-4 py-4 border-r border-kb-border text-center">
                  <div className="flex flex-col items-center gap-1.5">
                    <span className="font-bold text-kb-text">공동인증서</span>
                    <span className="text-[11px] text-kb-text-muted">(구 공인인증서)</span>
                    <Link href="/cert/joint-cert-issue"
                      className="text-[11px] font-bold text-white px-3 py-0.5 rounded-sm"
                      style={{ backgroundColor: '#0D5C47' }}>
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
                      <li key={u} className="flex items-start gap-1"><span className="text-[#0D5C47] mt-0.5">·</span>{u}</li>
                    ))}
                  </ul>
                </td>
              </tr>

              {/* 기업 */}
              <tr className="hover:bg-kb-beige-light/50">
                <td className="px-4 py-4 text-center font-bold text-kb-text border-r border-kb-border whitespace-nowrap">
                  기업
                </td>
                <td className="px-4 py-4 border-r border-kb-border text-center">
                  <div className="flex flex-col items-center gap-1.5">
                    <span className="font-bold text-kb-text">AXful인증서</span>
                    <span className="text-[11px] text-kb-text-muted">(기업)</span>
                    <Link href="/cert-biz/kb-cert-issue"
                      className="text-[11px] font-bold text-white px-3 py-0.5 rounded-sm"
                      style={{ backgroundColor: '#0D5C47' }}>
                      발급
                    </Link>
                  </div>
                </td>
                <td className="px-4 py-4 border-r border-kb-border text-[13px] text-kb-text-body leading-relaxed">
                  AXful 기업인터넷뱅킹을 가입한<br />법인·개인사업자 고객
                </td>
                <td className="px-4 py-4 border-r border-kb-border text-center">
                  <p className="font-medium text-kb-text-body">110,000원/년</p>
                  <p className="text-[11px] text-kb-text-muted">(부가세포함)</p>
                </td>
                <td className="px-4 py-4 text-kb-text-body leading-relaxed">
                  <ul className="space-y-0.5">
                    {['모든 인터넷 전자거래', '(전자입찰, 전자세금계산서 발행 포함)'].map(u => (
                      <li key={u} className="flex items-start gap-1"><span className="text-[#0D5C47] mt-0.5">·</span>{u}</li>
                    ))}
                  </ul>
                </td>
              </tr>

            </tbody>
          </table>
        </div>

      </main>
=======
    <div className="max-w-kb-container mx-auto px-8 py-12">

      <div className="mb-8 border-b-2 border-[#0D5C47] pb-4">
        <p className="text-[11px] font-semibold tracking-widest uppercase mb-1" style={{ color: '#5BC9A8' }}>Certificate</p>
        <h1 className="text-[26px] font-bold text-kb-text">인증센터</h1>
      </div>

      <div className="grid grid-cols-2 gap-6">
        {CERT_GROUPS.map((group) => (
          <div key={group.type} className="border border-kb-border">
            {/* 헤더 */}
            <div className="px-6 py-4 border-b border-kb-border" style={{ backgroundColor: '#0D5C47' }}>
              <h2 className="text-[17px] font-bold text-white">{group.type}</h2>
              <p className="text-[12px] mt-0.5" style={{ color: 'rgba(255,255,255,0.7)' }}>{group.desc}</p>
            </div>

            {/* 메뉴 */}
            <div className="divide-y divide-kb-border">
              {group.items.map((item) => (
                <Link
                  key={item.label}
                  href={item.href}
                  className="flex items-center justify-between px-6 py-5 hover:bg-[#F5F6F8] transition-colors group"
                >
                  <span className={`text-[15px] font-${item.primary ? 'bold' : 'medium'} text-kb-text`}>
                    {item.label}
                  </span>
                  <span className="text-[18px] text-kb-text-muted group-hover:translate-x-1 transition-transform duration-150" style={{ color: '#5BC9A8' }}>›</span>
                </Link>
              ))}
            </div>
          </div>
        ))}
      </div>

>>>>>>> fdd0eea2117b6ec92dbe3c5ed5ccf099c712793a
    </div>
  )
}
