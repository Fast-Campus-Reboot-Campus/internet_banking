'use client'
import { useState } from 'react'
import Link from 'next/link'
import AdminSidebar from '@/components/admin/AdminSidebar'

export default function EventNewPage() {
  const [eventType, setEventType] = useState('응모형')
  const [hasPrize, setHasPrize] = useState(true)

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          마케팅 &gt; <Link href="/admin/events" className="hover:underline">이벤트 목록</Link> &gt; <span className="text-gray-700 font-medium">이벤트 등록/수정</span>
        </div>
        <div className="px-6 py-5 max-w-3xl">
          <div className="flex items-center justify-between mb-5">
            <h1 className="text-lg font-bold text-gray-800">이벤트 등록</h1>
            <Link href="/admin/events" className="text-xs border border-gray-300 px-3 py-1.5 rounded text-gray-600 hover:bg-gray-50">← 목록으로</Link>
          </div>

          {/* 기본 정보 */}
          <div className="bg-white border border-kb-border rounded-lg mb-4 shadow-sm">
            <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light text-sm font-semibold text-kb-text">기본 정보</div>
            <div className="p-4 space-y-4">
              <div>
                <label className="text-xs text-gray-500 block mb-1">이벤트명 <span className="text-red-500">*</span></label>
                <input placeholder="이벤트명을 입력하세요" className="border border-gray-300 text-sm px-3 py-2 rounded w-full" />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs text-gray-500 block mb-1">이벤트 유형</label>
                  <select value={eventType} onChange={e => setEventType(e.target.value)} className="border border-gray-300 text-sm px-2 py-2 rounded bg-white w-full">
                    {['응모형','미션','가입 이벤트','쿠폰'].map(o => <option key={o}>{o}</option>)}
                  </select>
                </div>
                <div>
                  <label className="text-xs text-gray-500 block mb-1">대상 고객</label>
                  <select className="border border-gray-300 text-sm px-2 py-2 rounded bg-white w-full">
                    {['전체','신규 가입자','마케팅 동의 고객','특정 상품 보유 고객','개인사업자'].map(o => <option key={o}>{o}</option>)}
                  </select>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs text-gray-500 block mb-1">시작일 <span className="text-red-500">*</span></label>
                  <input type="date" className="border border-gray-300 text-sm px-2 py-2 rounded w-full" />
                </div>
                <div>
                  <label className="text-xs text-gray-500 block mb-1">종료일 <span className="text-red-500">*</span></label>
                  <input type="date" className="border border-gray-300 text-sm px-2 py-2 rounded w-full" />
                </div>
              </div>
            </div>
          </div>

          {/* 응모 조건 (응모형일 때) */}
          {eventType === '응모형' && (
            <div className="bg-white border border-kb-border rounded-lg mb-4 shadow-sm">
              <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light text-sm font-semibold text-kb-text">응모 조건</div>
              <div className="p-4 space-y-3">
                <div className="flex items-center gap-3">
                  <input type="checkbox" id="cond1" defaultChecked className="w-4 h-4" />
                  <label htmlFor="cond1" className="text-sm text-gray-700">특정 상품 가입</label>
                  <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white ml-2">
                    <option>케이봇뱀 (전체)</option><option>케이봇뱀 밋충형</option><option>케이봇뱀 AI</option>
                  </select>
                </div>
                <div className="flex items-center gap-3">
                  <input type="checkbox" id="cond2" defaultChecked className="w-4 h-4" />
                  <label htmlFor="cond2" className="text-sm text-gray-700">최소 가입금액</label>
                  <input type="number" defaultValue={100000} className="border border-gray-300 text-sm px-2 py-1 rounded w-32 ml-2" />
                  <span className="text-xs text-gray-400">원 이상</span>
                </div>
                <div className="flex items-center gap-3">
                  <input type="checkbox" id="cond3" className="w-4 h-4" />
                  <label htmlFor="cond3" className="text-sm text-gray-700">자동이체 설정 필수</label>
                  <input type="number" defaultValue={100000} className="border border-gray-300 text-sm px-2 py-1 rounded w-32 ml-2" />
                  <span className="text-xs text-gray-400">원 이상</span>
                </div>
              </div>
            </div>
          )}

          {/* 경품 설정 */}
          <div className="bg-white border border-kb-border rounded-lg mb-4 shadow-sm">
            <div className="px-4 py-3 border-b border-gray-100 bg-gray-50 flex items-center justify-between">
              <span className="text-sm font-semibold text-gray-700">경품 설정</span>
              <label className="flex items-center gap-2 text-xs text-gray-500">
                <input type="checkbox" checked={hasPrize} onChange={e => setHasPrize(e.target.checked)} className="w-3 h-3" />경품 있음
              </label>
            </div>
            {hasPrize && (
              <div className="p-4 space-y-3">
                {[{ rank: '1등', prize: '신라호텔 숙박권 (1박)', count: '2명', method: '모바일쿠폰' },
                  { rank: '2등', prize: 'iPad mini 7세대',   count: '5명', method: '택배' }].map((item, i) => (
                  <div key={i} className="grid grid-cols-4 gap-2 items-center">
                    <select defaultValue={item.rank} className="border border-gray-300 text-sm px-2 py-1.5 rounded bg-white">
                      {['1등','2등','3등','참가상'].map(o => <option key={o}>{o}</option>)}
                    </select>
                    <input defaultValue={item.prize} className="border border-gray-300 text-sm px-2 py-1.5 rounded" />
                    <input defaultValue={item.count} className="border border-gray-300 text-sm px-2 py-1.5 rounded w-20" />
                    <select defaultValue={item.method} className="border border-gray-300 text-sm px-2 py-1.5 rounded bg-white">
                      {['모바일쿠폰','택배','계좌입금'].map(o => <option key={o}>{o}</option>)}
                    </select>
                  </div>
                ))}
                <button className="text-xs border border-dashed border-gray-300 text-gray-400 px-3 py-1 rounded w-full hover:border-gray-400">+ 경품 추가</button>
              </div>
            )}
          </div>

          {/* 버튼 */}
          <div className="flex gap-2 justify-end">
            <Link href="/admin/events" className="px-4 py-2 border border-gray-300 text-sm rounded text-gray-600">취소</Link>
            <button className="px-4 py-2 border border-gray-300 text-sm rounded text-gray-600">임시 저장</button>
            <button className="px-4 py-2 bg-kb-yellow text-white text-sm font-bold rounded hover:bg-kb-yellow-dark transition-colors">등록 완료</button>
          </div>
        </div>
      </main>
    </div>
  )
}
