'use client'

import Link from 'next/link'
import { useState, useEffect } from 'react'

const SUPPORT_TABS = [
  { label: '고객상담', href: '#', active: true },
  { label: '고객정보관리', href: '#' },
  { label: '사고신고', href: '#' },
  { label: '소비자보호', href: '#' },
  { label: '금융서비스', href: '#' },
  { label: '서식/약관/설명서', href: '#' },
  { label: '상품공시실', href: '#' },
]

const LEFT_MENU = [
  { label: '자주찾는 질문', href: '#', sub: [] },
  {
    label: '상담신청',
    href: '#',
    open: true,
    sub: [
      { label: '챗봇/채팅/이메일상담', href: '#' },
      { label: '나의상담내역', href: '#' },
      { label: '지점 상담 예약서비스', href: '/support/consultation/branch', active: true },
    ],
  },
  { label: '고객의 소리', href: '#', sub: [] },
]

const CONTENT_TYPES = [
  '선택',
  '예금/적금',
  '대출',
  '펀드/신탁',
  '카드',
  '보험',
  '외환',
  '기타',
]

const MONTHS = Array.from({ length: 3 }, (_, i) => {
  const d = new Date()
  d.setMonth(d.getMonth() + i)
  return `${d.getFullYear()}년 ${d.getMonth() + 1}월`
})

const TIMES = [
  '선택', '09:00', '09:30', '10:00', '10:30', '11:00', '11:30',
  '14:00', '14:30', '15:00', '15:30',
]

export default function BranchConsultationPage() {
  const [userName, setUserName] = useState('고객')
  const [pageTab, setPageTab] = useState<'apply' | 'history'>('apply')
  const [branch, setBranch] = useState('')
  const [contentType, setContentType] = useState('선택')
  const [contentDetail, setContentDetail] = useState('선택')
  const [month, setMonth] = useState('선택')
  const [day, setDay] = useState('선택')
  const [time, setTime] = useState('선택')
  const [timeDetail, setTimeDetail] = useState('선택')
  const [reserveType, setReserveType] = useState<'auto' | 'manual'>('auto')
  const [staffSelect, setStaffSelect] = useState('선택')
  const [memo, setMemo] = useState('')

  useEffect(() => {
    try {
      const token = localStorage.getItem('access_token')
      if (token) {
        const payload = JSON.parse(atob(token.split('.')[1]))
        if (payload?.name) setUserName(payload.name)
      }
    } catch {}
  }, [])

  function handleSubmit() {
    if (!branch) { alert('상담 지점을 입력해주세요.'); return }
    if (contentType === '선택') { alert('상담 내용을 선택해주세요.'); return }
    alert('상담 예약이 완료되었습니다.\n카카오톡 또는 문자로 안내드리겠습니다.')
  }

  return (
    <div className="min-h-screen bg-white">
      {/* 고객센터 탭 */}
      <div className="bg-[#5D3D2B]">
        <div className="max-w-kb-container mx-auto px-6">
          <div className="flex">
            {SUPPORT_TABS.map(tab => (
              <Link key={tab.label} href={tab.href}
                className={`px-6 py-3 text-[14px] font-medium transition-colors ${
                  tab.active
                    ? 'bg-[#5BC9A8] text-kb-text font-bold'
                    : 'text-white hover:bg-white/10'
                }`}>
                {tab.label}
              </Link>
            ))}
          </div>
        </div>
      </div>

      {/* 본문 */}
      <div className="max-w-kb-container mx-auto px-6 py-6">
        {/* 브레드크럼 */}
        <div className="flex justify-end mb-3 text-[12px] text-kb-text-muted gap-1 items-center">
          <Link href="#" className="hover:underline">고객센터</Link><span>&gt;</span>
          <Link href="#" className="hover:underline">고객상담</Link><span>&gt;</span>
          <Link href="#" className="hover:underline">상담신청</Link><span>&gt;</span>
          <Link href="#" className="hover:underline">지점 상담 예약서비스</Link><span>&gt;</span>
          <span className="text-kb-blue">예약 신청</span>
        </div>

        <div className="flex gap-6">
          {/* 왼쪽 사이드바 */}
          <aside className="w-[200px] flex-shrink-0">
            <div className="border border-kb-border">
              <div className="bg-[#5D3D2B] px-4 py-3">
                <span className="text-white font-bold text-[14px]">고객상담</span>
              </div>
              {LEFT_MENU.map(item => (
                <div key={item.label}>
                  <Link href={item.href}
                    className="flex items-center justify-between px-4 py-3 text-[13px] border-t border-kb-border hover:bg-kb-beige-light text-kb-text-body">
                    {item.label}
                    {item.sub.length > 0 && <span className="text-xs text-kb-text-muted">{item.open ? '▼' : '▶'}</span>}
                  </Link>
                  {item.open && item.sub.map(sub => (
                    <Link key={sub.label} href={sub.href}
                      className={`block pl-6 pr-4 py-2.5 text-[12px] border-t border-kb-border transition-colors ${
                        sub.active
                          ? 'bg-[#5BC9A8] text-kb-text font-bold'
                          : 'hover:bg-kb-beige-light text-kb-text-muted hover:text-kb-text'
                      }`}>
                      {sub.label}
                    </Link>
                  ))}
                </div>
              ))}
              <div className="border-t border-kb-border">
                <Link href="#"
                  className="flex items-center justify-between px-4 py-3 text-[13px] hover:bg-kb-beige-light text-kb-text-body">
                  인증센터 <span className="text-kb-blue text-xs">▶</span>
                </Link>
              </div>
            </div>
          </aside>

          {/* 메인 콘텐츠 */}
          <main className="flex-1 min-w-0">
            <h1 className="text-[20px] font-bold text-kb-text mb-4">지점 상담 예약서비스</h1>

            {/* 서브 탭 */}
            <div className="flex gap-0 mb-5">
              {[{ key: 'apply', label: '예약 신청' }, { key: 'history', label: '예약 현황 조회' }].map(t => (
                <button key={t.key}
                  onClick={() => setPageTab(t.key as 'apply' | 'history')}
                  className={`flex items-center gap-1.5 px-5 py-2 text-[13px] border transition-colors ${
                    pageTab === t.key
                      ? 'border-kb-text bg-white font-bold text-kb-text'
                      : 'border-kb-border bg-kb-beige-light text-kb-text-muted hover:text-kb-text'
                  }`}>
                  <span className="text-[#5BC9A8] font-bold">▶</span> {t.label}
                </button>
              ))}
            </div>

            {pageTab === 'apply' && (
              <>
                {/* 상담예약 절차 */}
                <div className="border border-kb-border bg-[#FAFAFA] p-5 mb-6">
                  <p className="text-[13px] font-bold text-kb-text mb-4">상담예약 절차</p>
                  <div className="flex items-start gap-3">
                    {/* Step 1 */}
                    <div className="flex flex-col items-center">
                      <div className="bg-[#5D3D2B] text-white text-[12px] px-4 py-2.5 font-bold whitespace-nowrap">
                        <span className="text-[#5BC9A8] mr-1">1</span> 상담예약 신청
                      </div>
                    </div>
                    <div className="text-[#999] text-xl mt-1">→</div>
                    {/* Step 2 */}
                    <div className="flex flex-col items-center">
                      <div className="bg-kb-beige-light border border-kb-border text-kb-text-muted text-[12px] px-4 py-2.5 font-medium whitespace-nowrap mb-2">
                        <span className="mr-1">2</span> 상담직원 선택
                      </div>
                      <div className="flex gap-2">
                        <div className="border border-kb-border bg-white px-3 py-2 text-[11px] text-center">
                          <p className="font-semibold text-kb-text">직원 자동배정</p>
                          <p className="text-kb-text-muted mt-0.5">예약 즉시 확정</p>
                        </div>
                        <div className="border border-kb-border bg-white px-3 py-2 text-[11px] text-center">
                          <p className="font-semibold text-kb-text">직원 직접 선택</p>
                          <p className="text-kb-text-muted mt-0.5">직원 확인 후 확정</p>
                        </div>
                      </div>
                    </div>
                    <div className="text-[#999] text-xl mt-1">→</div>
                    {/* Step 3 */}
                    <div className="flex flex-col items-center">
                      <div className="bg-kb-beige-light border border-kb-border text-kb-text-muted text-[12px] px-4 py-2.5 font-medium whitespace-nowrap">
                        <span className="mr-1">3</span> 예약지점 방문
                      </div>
                    </div>
                  </div>
                </div>

                {/* 안내 사항 */}
                <ul className="space-y-1.5 mb-6 text-[12px] text-kb-text-muted">
                  {[
                    '상담 직원을 선택하면 직원 확인 후 예약이 확정돼요.',
                    '직원 선택으로 예약 시, 영업일 16시 이전 신청 건은 당일 19시까지, 영업일 16시 이후 또는 휴일 신청 건은 다음 영업일 19시까지 결과를 알려드려요.',
                    '지점 상황에 따라 예약이 취소될 수 있어요.',
                    '예약시간 15분 전부터 번호표가 발권되며, 10분이 지나면 예약이 취소됩니다.',
                    '예약 당일 지점에 방문하시면, 지점 번호표 발행기의 [예약 발권]을 눌러주세요.',
                    '예금/펀드/신탁, 개인대출, 개인사업자/법인대출, 외환(수출입거래) 상담(신규, 연장)을 받을 수 있습니다.',
                  ].map((note, i) => (
                    <li key={i} className="flex gap-1.5">
                      <span className="text-[#5BC9A8] font-bold flex-shrink-0">*</span>
                      {note}
                    </li>
                  ))}
                </ul>

                {/* 상담 예약 정보 입력 */}
                <h2 className="text-[16px] font-bold text-kb-text mb-2">상담 예약 정보 입력</h2>
                <p className="text-[12px] text-[#E05555] mb-3">
                  <span className="text-[#5BC9A8] font-bold">★</span> 표시가 있는 항목은 반드시 입력해주세요.
                </p>

                <table className="w-full border-collapse text-[13px] border-t-2 border-kb-text">
                  <tbody>
                    {/* 상담 지점 */}
                    <tr>
                      <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text w-[120px] whitespace-nowrap align-middle">
                        상담 지점<span className="text-[#5BC9A8] font-bold ml-0.5">★</span>
                      </td>
                      <td className="border border-kb-border px-4 py-3">
                        <div className="flex items-center gap-2">
                          <input
                            type="text"
                            value={branch}
                            onChange={e => setBranch(e.target.value)}
                            placeholder=""
                            className="border border-kb-border px-3 py-1.5 text-[13px] w-48 outline-none focus:border-kb-blue"
                          />
                          <button className="border border-kb-border px-4 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light flex items-center gap-1">
                            지점검색
                            <svg viewBox="0 0 16 16" fill="none" className="w-3 h-3" stroke="currentColor" strokeWidth="2">
                              <path d="M7 1h7v7M14 1L7 8" /><rect x="1" y="5" width="7" height="9" />
                            </svg>
                          </button>
                        </div>
                      </td>
                    </tr>

                    {/* 상담 내용 */}
                    <tr>
                      <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text align-middle whitespace-nowrap">
                        상담 내용<span className="text-[#5BC9A8] font-bold ml-0.5">★</span>
                      </td>
                      <td className="border border-kb-border px-4 py-3">
                        <div className="flex items-center gap-2 mb-1.5">
                          <select
                            value={contentType}
                            onChange={e => setContentType(e.target.value)}
                            className="border border-kb-border px-3 py-1.5 text-[13px] outline-none bg-white"
                          >
                            {CONTENT_TYPES.map(c => <option key={c}>{c}</option>)}
                          </select>
                          <button className="border border-kb-border px-4 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light">선택</button>
                        </div>
                        <p className="text-[12px] text-[#E05555] flex items-start gap-1">
                          <span className="bg-[#E05555] text-white rounded-full w-4 h-4 flex items-center justify-center text-[10px] flex-shrink-0 mt-0.5">ⓘ</span>
                          입출금, 통장 재발행, 통장정리, 동전교환 등 단순 업무는 예약 대상이 아닙니다.
                        </p>
                      </td>
                    </tr>

                    {/* 상담 일시 */}
                    <tr>
                      <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text align-top whitespace-nowrap pt-4">
                        상담 일시<span className="text-[#5BC9A8] font-bold ml-0.5">★</span>
                      </td>
                      <td className="border border-kb-border px-4 py-3">
                        <div className="flex items-center gap-2 mb-1.5 flex-wrap">
                          <select value={month} onChange={e => setMonth(e.target.value)}
                            className="border border-kb-border px-3 py-1.5 text-[13px] outline-none bg-white">
                            <option>선택</option>
                            {MONTHS.map(m => <option key={m}>{m}</option>)}
                          </select>
                          <button className="border border-kb-border px-4 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light">선택</button>
                          <select value={time} onChange={e => setTime(e.target.value)}
                            className="border border-kb-border px-3 py-1.5 text-[13px] outline-none bg-white">
                            {TIMES.map(t => <option key={t}>{t}</option>)}
                          </select>
                          <button className="border border-kb-border px-4 py-1.5 text-[12px] text-kb-text-body hover:bg-kb-beige-light">선택</button>
                        </div>
                        <p className="text-[12px] text-kb-text-muted flex items-start gap-1">
                          <span className="bg-kb-text-muted text-white rounded-full w-4 h-4 flex items-center justify-center text-[10px] flex-shrink-0 mt-0.5">ⓘ</span>
                          상담 예약은 같은 날짜에 1회만 신청할 수 있으며, 예약 상황에 따라 일부 시간대는 신청이 어려울 수 있습니다.
                        </p>
                      </td>
                    </tr>

                    {/* 예약 방법 */}
                    <tr>
                      <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text align-top whitespace-nowrap pt-4">
                        예약 방법<span className="text-[#5BC9A8] font-bold ml-0.5">★</span>
                      </td>
                      <td className="border border-kb-border px-4 py-3 space-y-3">
                        {/* 자동배정 */}
                        <label className="flex items-start gap-2 cursor-pointer">
                          <input type="radio" name="reserveType" checked={reserveType === 'auto'}
                            onChange={() => setReserveType('auto')}
                            className="mt-0.5 accent-[#5BC9A8]" />
                          <div>
                            <p className="text-[13px] font-semibold text-kb-text">직원 자동배정</p>
                            <p className="text-[12px] text-kb-text-body">예약이 바로 확정돼요.</p>
                            <p className="text-[12px] text-kb-text-muted flex items-start gap-1 mt-0.5">
                              <span className="bg-kb-text-muted text-white rounded-full w-4 h-4 flex items-center justify-center text-[10px] flex-shrink-0 mt-0.5">ⓘ</span>
                              예약한 시간에 바로 상담할 수 있는 직원이 배정돼요.
                            </p>
                          </div>
                        </label>
                        {/* 직접 선택 */}
                        <label className="flex items-start gap-2 cursor-pointer">
                          <input type="radio" name="reserveType" checked={reserveType === 'manual'}
                            onChange={() => setReserveType('manual')}
                            className="mt-0.5 accent-[#5BC9A8]" />
                          <div className="flex-1">
                            <div className="flex items-center gap-4">
                              <div>
                                <p className="text-[13px] font-semibold text-kb-text">상담직원 선택</p>
                                <p className="text-[12px] text-kb-text-body">직원 확인 후 예약이 확정돼요.</p>
                              </div>
                              {reserveType === 'manual' && (
                                <select value={staffSelect} onChange={e => setStaffSelect(e.target.value)}
                                  className="border border-kb-border px-3 py-1.5 text-[13px] outline-none bg-white">
                                  <option>선택</option>
                                </select>
                              )}
                              {reserveType !== 'manual' && (
                                <select disabled className="border border-kb-border px-3 py-1.5 text-[13px] bg-[#f5f5f5] text-kb-text-muted">
                                  <option>선택</option>
                                </select>
                              )}
                            </div>
                            <p className="text-[12px] text-kb-text-muted flex items-start gap-1 mt-0.5">
                              <span className="bg-kb-text-muted text-white rounded-full w-4 h-4 flex items-center justify-center text-[10px] flex-shrink-0 mt-0.5">ⓘ</span>
                              지점 상황에 따라 예약이 취소 될 수 있어요.
                            </p>
                          </div>
                        </label>
                      </td>
                    </tr>

                    {/* 상담 희망 내용 */}
                    <tr>
                      <td className="bg-kb-beige-light border border-kb-border px-4 py-3 font-semibold text-kb-text align-top whitespace-nowrap">
                        상담 희망 내용
                      </td>
                      <td className="border border-kb-border px-4 py-3">
                        <p className="text-[12px] text-kb-text-muted mb-1">{memo.length}/200자</p>
                        <textarea
                          value={memo}
                          onChange={e => { if (e.target.value.length <= 200) setMemo(e.target.value) }}
                          placeholder={`상담받고 싶은 내용을 입력하시면 원활한 상담을 받으실 수 있어요.\n예) 상담 상품, 상담 문의사항, 추가 상담 필요업무 등`}
                          className="w-full h-24 border border-kb-border px-3 py-2 text-[13px] resize-none outline-none leading-relaxed text-kb-text-body placeholder:text-kb-text-muted"
                        />
                      </td>
                    </tr>
                  </tbody>
                </table>

                {/* 주의사항 */}
                <ul className="space-y-1 mt-4 mb-5 text-[12px] text-kb-text-muted">
                  {[
                    '상담 예약을 신청하면 카카오톡 또는 문자 메시지로 내역이 발송됩니다.',
                    '스팸 차단을 했거나 SMS 수신에 동의하지 않은 경우, 예약 안내를 받을 수 없습니다.',
                    '연락처 변경이나 휴대폰 번호 등록은 [내 정보 수정]에서 할 수 있습니다',
                  ].map((n, i) => (
                    <li key={i} className="flex gap-1.5"><span className="flex-shrink-0">*</span>{n}</li>
                  ))}
                </ul>

                {/* 버튼 */}
                <div className="flex items-center gap-3">
                  <button className="border border-kb-border px-5 py-2.5 text-[13px] text-kb-text-body hover:bg-kb-beige-light transition-colors">
                    내 정보 수정
                  </button>
                  <button
                    onClick={handleSubmit}
                    className="px-10 py-2.5 text-[14px] font-bold hover:opacity-90 transition-opacity"
                    style={{ backgroundColor: '#5BC9A8', color: '#000' }}
                  >
                    상담 예약
                  </button>
                </div>
              </>
            )}

            {pageTab === 'history' && (
              <div className="py-16 text-center text-[14px] text-kb-text-muted border border-kb-border">
                예약 현황이 없습니다.
              </div>
            )}
          </main>
        </div>
      </div>
    </div>
  )
}
