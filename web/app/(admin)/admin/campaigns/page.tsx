'use client'
import { useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { MOCK_CAMPAIGNS, CampaignRecord, CampaignStatus } from '@/lib/admin-mock-data'

const STATUS_COLOR: Record<CampaignStatus, string> = {
  '예약':   'bg-blue-100 text-blue-700',
  '진행중': 'bg-orange-100 text-orange-700',
  '완료':   'bg-green-100 text-green-700',
}
const CHANNEL_COLOR: Record<string, string> = {
  'SMS':    'bg-yellow-100 text-yellow-700',
  '이메일': 'bg-purple-100 text-purple-700',
  '앱 푸시': 'bg-blue-100 text-blue-700',
}

export default function CampaignsPage() {
  const [showNew, setShowNew] = useState(false)
  const [selected, setSelected] = useState<CampaignRecord | null>(null)

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          마케팅 &gt; <span className="text-gray-700 font-medium">마케팅 발송</span>
        </div>
        <div className="px-6 py-5">
          <h1 className="text-lg font-bold text-gray-800 mb-4">마케팅 발송 (캠페인)</h1>

          <div className="flex items-center gap-3 mb-4 bg-white border border-kb-border rounded-lg px-4 py-3 shadow-sm">
            <input placeholder="캠페인명" className="border border-gray-300 text-xs px-2 py-1.5 rounded w-40" />
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">채널</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white">
                <option>전체</option><option>SMS</option><option>이메일</option><option>앱 푸시</option>
              </select>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">상태</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white">
                {['전체','예약','진행중','완료'].map(o => <option key={o}>{o}</option>)}
              </select>
            </div>
            <button className="ml-auto px-3 py-1.5 bg-kb-yellow text-white text-xs font-bold rounded hover:bg-kb-yellow-dark transition-colors">조회</button>
            <button onClick={() => setShowNew(true)} className="text-xs bg-blue-50 text-blue-700 border border-blue-300 px-3 py-1.5 rounded">+ 캠페인 등록</button>
          </div>

          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm mb-4">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  {['캠페인 ID','캠페인명','채널','타겟 세그먼트','발송 대상','발송 예정일','결과','상태','작업'].map(h => (
                    <th key={h} className="px-3 py-2.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {MOCK_CAMPAIGNS.map(c => (
                  <tr key={c.id} className="hover:bg-kb-beige-light cursor-pointer" onClick={() => setSelected(c)}>
                    <td className="px-3 py-2.5 text-xs font-mono text-blue-600">{c.id}</td>
                    <td className="px-3 py-2.5 font-medium">{c.name}</td>
                    <td className="px-3 py-2.5">
                      <span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${CHANNEL_COLOR[c.channel]}`}>{c.channel}</span>
                    </td>
                    <td className="px-3 py-2.5 text-xs text-gray-600">{c.targetSegment}</td>
                    <td className="px-3 py-2.5 text-xs font-medium">{c.targetCount.toLocaleString()}명</td>
                    <td className="px-3 py-2.5 text-xs text-gray-400">{c.scheduledAt}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-500">{c.result ?? '-'}</td>
                    <td className="px-3 py-2.5">
                      <span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${STATUS_COLOR[c.status]}`}>{c.status}</span>
                    </td>
                    <td className="px-3 py-2.5">
                      {c.status === '예약' && <button className="text-xs border border-red-300 text-red-600 px-2 py-0.5 rounded">취소</button>}
                      {c.status === '완료' && <button className="text-xs border border-gray-300 text-gray-500 px-2 py-0.5 rounded">통계</button>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* 캠페인 상세 */}
          {selected && (
            <div className="bg-white border border-kb-border rounded-lg p-5 shadow-sm mb-4">
              <h3 className="text-sm font-bold mb-3">캠페인 상세 — {selected.id}</h3>
              <div className="grid grid-cols-3 gap-4 text-sm mb-4">
                <div><p className="text-xs text-gray-400">채널</p>
                  <span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${CHANNEL_COLOR[selected.channel]}`}>{selected.channel}</span>
                </div>
                <div><p className="text-xs text-gray-400">발송 대상</p><p className="font-medium">{selected.targetCount.toLocaleString()}명</p></div>
                <div><p className="text-xs text-gray-400">발송 예정</p><p className="font-medium">{selected.scheduledAt}</p></div>
                <div className="col-span-3"><p className="text-xs text-gray-400">타겟 세그먼트</p><p className="font-medium">{selected.targetSegment}</p></div>
                {selected.result && <div className="col-span-3"><p className="text-xs text-gray-400">결과</p><p className="font-medium text-green-600">{selected.result}</p></div>}
              </div>
              <div className="flex justify-end gap-2">
                <button onClick={() => setSelected(null)} className="px-4 py-2 border border-gray-300 text-sm rounded">닫기</button>
              </div>
            </div>
          )}

          {/* 신규 캠페인 폼 */}
          {showNew && (
            <div className="bg-white border border-kb-border rounded-lg p-5 shadow-sm">
              <h3 className="text-sm font-bold mb-4">캠페인 등록</h3>
              <div className="grid grid-cols-2 gap-4 mb-4">
                <div><label className="text-xs text-gray-500 block mb-1">캠페인명 <span className="text-red-500">*</span></label>
                  <input placeholder="캠페인명" className="border border-gray-300 text-sm px-2 py-1.5 rounded w-full" />
                </div>
                <div><label className="text-xs text-gray-500 block mb-1">채널</label>
                  <select className="border border-gray-300 text-sm px-2 py-1.5 rounded bg-white w-full">
                    <option>SMS</option><option>이메일</option><option>앱 푸시</option>
                  </select>
                </div>
                <div className="col-span-2"><label className="text-xs text-gray-500 block mb-1">타겟 세그먼트</label>
                  <select className="border border-gray-300 text-sm px-2 py-1.5 rounded bg-white w-full">
                    <option>마케팅 동의 + 투자 관심 고객</option>
                    <option>전체 앱 사용자</option>
                    <option>FATCA 대상 미제출자</option>
                    <option>특화 지점 근방 거주 고객</option>
                    <option>신규 가입자 (30일 이내)</option>
                  </select>
                </div>
                <div><label className="text-xs text-gray-500 block mb-1">발송 일시</label>
                  <input type="datetime-local" className="border border-gray-300 text-sm px-2 py-1.5 rounded w-full" />
                </div>
                <div><label className="text-xs text-gray-500 block mb-1">메시지 유형</label>
                  <select className="border border-gray-300 text-sm px-2 py-1.5 rounded bg-white w-full">
                    <option>이벤트 안내</option><option>상품 안내</option><option>공지 안내</option><option>서비스 안내</option>
                  </select>
                </div>
                <div className="col-span-2"><label className="text-xs text-gray-500 block mb-1">메시지 내용</label>
                  <textarea rows={4} placeholder="발송할 메시지를 입력하세요." className="border border-gray-300 text-sm px-2 py-1.5 rounded w-full resize-none" />
                </div>
              </div>
              <div className="flex gap-2 justify-end">
                <button onClick={() => setShowNew(false)} className="px-4 py-2 border border-gray-300 text-sm rounded">취소</button>
                <button className="px-4 py-2 border border-gray-300 text-sm rounded text-gray-600">테스트 발송</button>
                <button className="px-4 py-2 bg-kb-yellow text-white text-sm font-bold rounded hover:bg-kb-yellow-dark transition-colors">예약 등록</button>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
