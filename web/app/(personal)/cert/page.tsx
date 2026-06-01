'use client'

import Link from 'next/link'

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
    ],
  },
]

export default function CertPage() {
  return (
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

    </div>
  )
}
