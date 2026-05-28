'use client'
import { useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { MOCK_WINNERS, MOCK_EVENTS, WinnerStatus } from '@/lib/admin-mock-data'

const STATUS_COLOR: Record<WinnerStatus, string> = {
  '발송완료':   'bg-green-100 text-green-700',
  '발송대기':   'bg-orange-100 text-orange-700',
  '주소 확인중': 'bg-blue-100 text-blue-700',
}

export default function WinnersPage() {
  const [eventId, setEventId] = useState('EV-2026-024')
  const [showDrawModal, setShowDrawModal] = useState(false)

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          마케팅 &gt; <span className="text-gray-700 font-medium">당첨자 추첨/관리</span>
        </div>
        <div className="px-6 py-5">
          <h1 className="text-lg font-bold text-gray-800 mb-4">당첨자 추첨 / 관리</h1>

          <div className="flex items-center gap-3 mb-4 bg-white border border-kb-border rounded-lg px-4 py-3 shadow-sm">
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">이벤트</span>
              <select value={eventId} onChange={e => setEventId(e.target.value)} className="border border-gray-300 text-xs px-2 py-1 rounded bg-white w-64">
                {MOCK_EVENTS.map(e => (
                  <option key={e.id} value={e.id}>[{e.id}] {e.name}</option>
                ))}
              </select>
            </div>
            <button className="ml-auto px-3 py-1.5 bg-kb-yellow text-white text-xs font-bold rounded hover:bg-kb-yellow-dark transition-colors">조회</button>
            <button onClick={() => setShowDrawModal(true)} className="text-xs bg-blue-600 text-white font-bold px-3 py-1.5 rounded">추첨 실행</button>
            <button className="text-xs border border-gray-300 px-3 py-1.5 rounded text-gray-600">엑셀</button>
          </div>

          {/* 당첨자 통계 */}
          <div className="grid grid-cols-4 gap-3 mb-4">
            {[
              { label: '전체 당첨자', value: MOCK_WINNERS.length + '명' },
              { label: '발송 완료', value: MOCK_WINNERS.filter(w => w.status === '발송완료').length + '명', color: 'text-green-600' },
              { label: '발송 대기', value: MOCK_WINNERS.filter(w => w.status === '발송대기').length + '명', color: 'text-orange-600' },
              { label: '주소 확인중', value: MOCK_WINNERS.filter(w => w.status === '주소 확인중').length + '명', color: 'text-blue-600' },
            ].map(card => (
              <div key={card.label} className="bg-white border border-kb-border rounded-lg px-4 py-3 shadow-sm text-center">
                <p className="text-xs text-gray-400 mb-1">{card.label}</p>
                <p className={`text-xl font-bold ${card.color ?? 'text-gray-800'}`}>{card.value}</p>
              </div>
            ))}
          </div>

          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  {['당첨 ID','등수','고객번호','이름','연락처','경품','지급 방식','지급 예정일','상태','작업'].map(h => (
                    <th key={h} className="px-3 py-2.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {MOCK_WINNERS.map(w => (
                  <tr key={w.id} className="hover:bg-kb-beige-light">
                    <td className="px-3 py-2.5 text-xs font-mono text-blue-600">{w.id}</td>
                    <td className="px-3 py-2.5">
                      <span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${w.rank === '1등' ? 'bg-yellow-100 text-yellow-700' : 'bg-gray-100 text-gray-600'}`}>{w.rank}</span>
                    </td>
                    <td className="px-3 py-2.5 text-xs text-gray-500">{w.customerId}</td>
                    <td className="px-3 py-2.5 font-medium">{w.name}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-500">{w.phone}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-600">{w.prize}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-500">{w.paymentMethod}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-400">{w.paymentDate}</td>
                    <td className="px-3 py-2.5">
                      <span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${STATUS_COLOR[w.status]}`}>{w.status}</span>
                    </td>
                    <td className="px-3 py-2.5">
                      {w.status === '발송대기' && <button className="text-xs bg-kb-yellow text-white px-2 py-0.5 rounded font-medium">발송</button>}
                      {w.status === '주소 확인중' && <button className="text-xs border border-blue-300 text-blue-600 px-2 py-0.5 rounded">주소 입력</button>}
                      {w.status === '발송완료' && <button className="text-xs border border-gray-300 text-gray-500 px-2 py-0.5 rounded">완료</button>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* 추첨 모달 */}
          {showDrawModal && (
            <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
              <div className="bg-white rounded-lg shadow-xl p-6 w-96">
                <h3 className="text-sm font-bold mb-4">추첨 실행</h3>
                <div className="bg-kb-beige-light rounded p-3 text-xs text-gray-600 mb-4 space-y-1">
                  <p>이벤트: 케이봇뱀 포트폴리오 고객 챌린지</p>
                  <p>대상: 조건 충족 응모자 {MOCK_WINNERS.length}명</p>
                  <p>추첨 방식: 시스템 난수 (감사 로그 기록)</p>
                </div>
                <div className="space-y-2 mb-4">
                  {[{ rank: '1등', count: 2 }, { rank: '2등', count: 5 }].map(item => (
                    <div key={item.rank} className="flex items-center justify-between text-sm">
                      <span>{item.rank}</span>
                      <div className="flex items-center gap-2">
                        <input type="number" defaultValue={item.count} className="border border-gray-300 text-sm px-2 py-1 rounded w-16 text-center" />
                        <span className="text-xs text-gray-400">명</span>
                      </div>
                    </div>
                  ))}
                </div>
                <div className="flex gap-2 justify-end">
                  <button onClick={() => setShowDrawModal(false)} className="px-3 py-2 border border-gray-300 text-sm rounded">취소</button>
                  <button onClick={() => setShowDrawModal(false)} className="px-3 py-2 bg-kb-yellow text-white text-sm font-bold rounded hover:bg-kb-yellow-dark transition-colors">추첨 확정</button>
                </div>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
