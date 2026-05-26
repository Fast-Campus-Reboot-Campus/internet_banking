'use client'

import Link from 'next/link'
import { useState } from 'react'
import ManageSidebar from '@/components/inquiry/ManageSidebar'

type MainTab = '인터넷뱅킹 이용안내' | '개인용인증서 발급안내' | '타인증서 등록안내'
type SubTab = '지점에서 뱅킹 가입하신 분' | '신규 시 필요서류 안내'

const CERT_STEPS_ROW1 = [
  { num: 1, label: '약관동의 및\n사용자 본인 확인' },
  { num: 2, label: '발급대상\n인증서 선택' },
  { num: 3, label: '출금계좌번호 및\n보안카드번호 입력' },
  { num: 4, label: '수수료출금\n예약 확인' },
]

const CERT_STEPS_ROW2 = [
  { num: 5, label: '고객세부정보\n입력' },
  { num: 6, label: '인증서암호 및\n저장위치 선택' },
  { num: 7, label: '인증서 발급\n완료' },
]

export default function FirstVisitPage() {
  const [mainTab, setMainTab] = useState<MainTab>('인터넷뱅킹 이용안내')
  const [subTab, setSubTab] = useState<SubTab>('지점에서 뱅킹 가입하신 분')

  return (
    <div className="max-w-kb-container mx-auto px-6 py-6 flex">

      <ManageSidebar />

      {/* 우측 메인 */}
      <main className="flex-1 min-w-0">

        {/* 브레드크럼 */}
        <div className="flex items-center gap-2 text-sm text-kb-text-muted mb-4 flex-wrap">
          <span>개인뱅킹</span>
          <span>&gt;</span>
          <span>뱅킹관리</span>
          <span>&gt;</span>
          <span>이용안내</span>
          <span>&gt;</span>
          <span>첫 방문 고객을 위한 안내</span>
          {mainTab === '인터넷뱅킹 이용안내' && (
            <>
              <span>&gt;</span>
              <span>인터넷뱅킹 이용안내</span>
              <span>&gt;</span>
              <span className="text-kb-text font-medium">{subTab}</span>
            </>
          )}
        </div>

        {/* 페이지 제목 */}
        <h2 className="text-2xl font-bold text-kb-text border-b-2 border-kb-text pb-3 mb-6">
          첫 방문 고객을 위한 안내
        </h2>

        {/* 메인 탭 3개 */}
        <div className="flex border-b border-kb-border mb-6">
          {(['인터넷뱅킹 이용안내', '개인용인증서 발급안내', '타인증서 등록안내'] as MainTab[]).map((tab) => (
            <button
              key={tab}
              onClick={() => setMainTab(tab)}
              className={`px-6 py-3 text-sm whitespace-nowrap transition-colors
                ${mainTab === tab
                  ? 'border-b-2 border-kb-text font-bold text-kb-text -mb-px'
                  : 'text-kb-text-muted hover:text-kb-text'
                }`}
            >
              {tab}
            </button>
          ))}
        </div>

        {mainTab === '인터넷뱅킹 이용안내' && (
          <div className="space-y-6">

            {/* 서브 탭 */}
            <div className="flex gap-1">
              {(['지점에서 뱅킹 가입하신 분', '신규 시 필요서류 안내'] as SubTab[]).map((tab) => (
                <button
                  key={tab}
                  onClick={() => setSubTab(tab)}
                  className={`px-2 py-1 text-sm transition-colors
                    ${subTab === tab
                      ? 'text-kb-taupe font-bold underline underline-offset-2'
                      : 'text-kb-text-muted hover:text-kb-text'
                    }`}
                >
                  ▶ {tab}
                </button>
              ))}
            </div>

            {subTab === '지점에서 뱅킹 가입하신 분' && (
              <div className="space-y-6">
                {/* 안내 문구 */}
                <div className="space-y-2">
                  <p className="text-sm text-kb-text-body leading-relaxed">
                    · 인터넷뱅킹은 은행이 정한 인증서를 발급받아야 이용이 가능합니다. 아래 이용절차에 따라 은행이 정한 인증서를 발급받으시기 바랍니다.
                  </p>
                  <p className="text-sm text-kb-text-body leading-relaxed">
                    · 이미 다른 은행에서 발급받은 인증서를 사용하고 있다면 [타행인증서등록]을 해야 AX풀뱅크 인터넷뱅킹을 이용하실 수 있습니다.
                  </p>
                  <Link href="#" className="text-sm text-kb-taupe font-medium underline underline-offset-2">
                    타행인증서 등록안내 바로가기
                  </Link>
                </div>

                {/* 서비스 이용절차 */}
                <div>
                  <h3 className="text-base font-bold text-kb-text mb-5">서비스 이용절차</h3>

                  {/* 01 — 개인용 인증서 발급 */}
                  <div className="border border-kb-border p-6 mb-4 space-y-4">
                    <p className="text-sm font-bold text-kb-taupe">01&nbsp; 개인용 인증서 발급</p>

                    {/* 스텝 Row 1 */}
                    <div className="flex items-stretch gap-1">
                      {CERT_STEPS_ROW1.map((step, i) => (
                        <div key={step.num} className="flex items-center flex-1">
                          <div className="flex-1 border border-kb-border px-3 py-3 bg-white">
                            <p className="text-[11px] font-bold text-kb-taupe mb-1">{step.num}</p>
                            <p className="text-[11px] text-kb-text whitespace-pre-line leading-snug">{step.label}</p>
                          </div>
                          {i < CERT_STEPS_ROW1.length - 1 && (
                            <span className="text-kb-yellow font-bold px-0.5">▶</span>
                          )}
                        </div>
                      ))}
                    </div>

                    {/* 스텝 Row 2 */}
                    <div className="flex items-stretch gap-1">
                      {CERT_STEPS_ROW2.map((step, i) => (
                        <div key={step.num} className="flex items-center flex-1">
                          <div className="flex-1 border border-kb-border px-3 py-3 bg-white">
                            <p className="text-[11px] font-bold text-kb-taupe mb-1">{step.num}</p>
                            <p className="text-[11px] text-kb-text whitespace-pre-line leading-snug">{step.label}</p>
                          </div>
                          {i < CERT_STEPS_ROW2.length - 1 && (
                            <span className="text-kb-yellow font-bold px-0.5">▶</span>
                          )}
                        </div>
                      ))}
                      {/* 빈 칸 채우기 */}
                      <div className="flex-1" />
                    </div>

                    {/* 안내 사항 */}
                    <div className="space-y-1.5 border-t border-kb-border pt-4">
                      <p className="text-sm text-kb-text-body leading-relaxed">
                        &gt; 인증서를 발급 시, 고객님이 지점에서 신청한 사용자 아이디(ID), 인터넷뱅킹 신청 시 수령한 보안카드의 지정번호, 출금계좌번호, 계좌비밀번호 등의 정보를 입력해야 합니다.
                      </p>
                      <p className="text-sm text-kb-text-body leading-relaxed">
                        * 2020년 12월 10일부터 시행된 전자서명법 개정으로 인해 기존 공인인증서 제도는 폐지되었습니다. AX풀뱅크에서 인정한 인증서를 통해 개인 법인 공동인증서 이용을 부탁드립니다.(유료 개인 범용인증서는 현행과 동일)
                      </p>
                      <p className="text-sm text-kb-text-body leading-relaxed">
                        &gt; 아래 [개인용인증서발급 바로가기] 버튼을 클릭하고 순서에 따라 인증서를 발급받으세요.
                      </p>
                    </div>
                  </div>

                  {/* 02 — 인터넷뱅킹 로그인 */}
                  <div className="border border-kb-border p-6 space-y-2">
                    <p className="text-sm font-bold text-kb-taupe">02&nbsp; 인터넷뱅킹 로그인</p>
                    <p className="text-sm text-kb-text-body leading-relaxed">
                      인증서 암호를 입력하고 로그인하면 AX풀뱅크 인터넷뱅킹을 편리하게 이용하실 수 있습니다.
                    </p>
                  </div>
                </div>

                {/* CTA 버튼 */}
                <div className="flex justify-center pt-2">
                  <Link
                    href="/cert/fin-cert-issue"
                    className="px-10 py-3 bg-kb-yellow text-base font-bold text-kb-text hover:brightness-95 transition-all"
                  >
                    개인용인증서발급 바로가기
                  </Link>
                </div>
              </div>
            )}

            {subTab === '신규 시 필요서류 안내' && (
              <div className="border border-kb-border p-6 space-y-3">
                <p className="text-base font-bold text-kb-text">신규 시 필요서류 안내</p>
                <ul className="space-y-2 text-sm text-kb-text-body list-disc list-inside">
                  <li>신분증 (주민등록증, 운전면허증 등)</li>
                  <li>도장 또는 서명</li>
                  <li>초기 입금용 현금 또는 이체 가능한 계좌</li>
                </ul>
              </div>
            )}
          </div>
        )}

        {mainTab === '개인용인증서 발급안내' && (
          <div className="border border-kb-border p-8 text-center space-y-3">
            <p className="text-base font-bold text-kb-text">개인용 인증서 발급 안내</p>
            <p className="text-sm text-kb-text-muted">AXful인증서 또는 금융인증서를 발급받아 이용하세요.</p>
            <Link href="/cert" className="inline-block mt-2 px-8 py-2.5 bg-kb-yellow text-sm font-bold text-kb-text hover:brightness-95 transition-all">
              인증센터 바로가기
            </Link>
          </div>
        )}

        {mainTab === '타인증서 등록안내' && (
          <div className="border border-kb-border p-8 text-center space-y-3">
            <p className="text-base font-bold text-kb-text">타인증서 등록 안내</p>
            <p className="text-sm text-kb-text-muted">타행·타기관에서 발급한 인증서를 등록하여 AX풀뱅크 인터넷뱅킹을 이용하실 수 있습니다.</p>
            <Link href="/cert" className="inline-block mt-2 px-8 py-2.5 bg-kb-yellow text-sm font-bold text-kb-text hover:brightness-95 transition-all">
              타행인증서 등록하기
            </Link>
          </div>
        )}

      </main>
    </div>
  )
}
