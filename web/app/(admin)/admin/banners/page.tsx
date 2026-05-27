'use client'
import { useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { MOCK_BANNERS, BannerRecord, BannerStatus } from '@/lib/admin-mock-data'

const STATUS_COLOR: Record<BannerStatus, string> = {
  '노출중':   'bg-green-100 text-green-700',
  '종료예정': 'bg-orange-100 text-orange-700',
  '종료':     'bg-gray-100 text-gray-400',
}

export default function BannersPage() {
  const [selected, setSelected] = useState<BannerRecord | null>(null)
  const [editing, setEditing] = useState(false)

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          마케팅 &gt; <span className="text-gray-700 font-medium">배너 관리</span>
        </div>
        <div className="px-6 py-5">
          <h1 className="text-lg font-bold text-gray-800 mb-4">배너 관리</h1>

          <div className="flex items-center gap-3 mb-4 bg-white border border-kb-border rounded-lg px-4 py-3 shadow-sm">
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">노출 위치</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white">
                <option>전체</option><option>웹·앱 메인</option><option>웹 메인</option><option>앱 메인</option>
              </select>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">상태</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white">
                <option>전체</option><option>노출중</option><option>종료예정</option><option>종료</option>
              </select>
            </div>
            <button className="ml-auto px-3 py-1.5 bg-kb-yellow text-white text-xs font-bold rounded hover:bg-kb-yellow-dark transition-colors">조회</button>
            <button onClick={() => { setSelected(null); setEditing(true) }} className="text-xs bg-blue-50 text-blue-700 border border-blue-300 px-3 py-1.5 rounded">+ 배너 등록</button>
          </div>

          <div className="grid grid-cols-2 gap-4">
            {/* 배너 목록 */}
            <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
              <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light text-xs font-medium text-kb-text-muted">배너 목록 ({MOCK_BANNERS.length}건)</div>
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-100 text-xs text-gray-500">
                    {['ID','제목','노출 위치','기간','클릭','상태'].map(h => (
                      <th key={h} className="px-3 py-2 text-left font-medium">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {MOCK_BANNERS.map(b => (
                    <tr
                      key={b.id}
                      className={`cursor-pointer hover:bg-yellow-50 ${selected?.id === b.id ? 'bg-yellow-50 border-l-2 border-yellow-400' : ''}`}
                      onClick={() => { setSelected(b); setEditing(false) }}
                    >
                      <td className="px-3 py-2 text-xs font-mono text-blue-600">{b.id}</td>
                      <td className="px-3 py-2">
                        <div className="flex items-center gap-2">
                          <div className="w-3 h-3 rounded-sm flex-shrink-0" style={{ backgroundColor: b.color }} />
                          <span className="text-xs font-medium truncate max-w-28">{b.title}</span>
                        </div>
                      </td>
                      <td className="px-3 py-2 text-xs text-gray-500">{b.position}</td>
                      <td className="px-3 py-2 text-xs text-gray-400">{b.period}</td>
                      <td className="px-3 py-2 text-xs text-gray-600">{b.clickCount.toLocaleString()}</td>
                      <td className="px-3 py-2">
                        <span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${STATUS_COLOR[b.status]}`}>{b.status}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* 배너 미리보기 + 수정 */}
            <div className="bg-white border border-kb-border rounded-lg shadow-sm">
              <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light text-xs font-medium text-kb-text-muted">
                {editing ? '배너 등록/수정' : selected ? `미리보기 — ${selected.id}` : '배너 선택'}
              </div>
              {selected && !editing ? (
                <div className="p-4">
                  {/* 미리보기 */}
                  <div
                    className="rounded-lg h-24 flex items-center justify-center mb-4 relative overflow-hidden"
                    style={{ backgroundColor: selected.color }}
                  >
                    <span className="text-sm font-bold text-gray-800">{selected.title}</span>
                  </div>
                  <div className="space-y-2 text-sm mb-4">
                    <div className="flex justify-between"><span className="text-xs text-gray-400">노출 위치</span><span className="font-medium">{selected.position}</span></div>
                    <div className="flex justify-between"><span className="text-xs text-gray-400">노출 기간</span><span className="font-medium">{selected.period}</span></div>
                    <div className="flex justify-between"><span className="text-xs text-gray-400">연결 URL</span><span className="font-mono text-xs text-blue-600">{selected.linkUrl}</span></div>
                    <div className="flex justify-between"><span className="text-xs text-gray-400">총 클릭수</span><span className="font-medium">{selected.clickCount.toLocaleString()}회</span></div>
                  </div>
                  <div className="flex gap-2">
                    <button onClick={() => setEditing(true)} className="flex-1 px-3 py-1.5 border border-gray-300 text-sm rounded text-gray-600">수정</button>
                    {selected.status === '노출중' && <button className="flex-1 px-3 py-1.5 border border-red-300 text-red-600 text-sm rounded">종료</button>}
                  </div>
                </div>
              ) : editing ? (
                <div className="p-4 space-y-3">
                  <div><label className="text-xs text-gray-500 block mb-1">배너 제목</label>
                    <input defaultValue={selected?.title} placeholder="배너 제목" className="border border-gray-300 text-sm px-2 py-1.5 rounded w-full" />
                  </div>
                  <div><label className="text-xs text-gray-500 block mb-1">배경색</label>
                    <input type="color" defaultValue={selected?.color ?? '#F59E0B'} className="border border-gray-300 rounded h-8 w-20 cursor-pointer" />
                  </div>
                  <div><label className="text-xs text-gray-500 block mb-1">노출 위치</label>
                    <select defaultValue={selected?.position} className="border border-gray-300 text-sm px-2 py-1.5 rounded bg-white w-full">
                      <option>웹·앱 메인</option><option>웹 메인</option><option>앱 메인</option>
                    </select>
                  </div>
                  <div className="grid grid-cols-2 gap-2">
                    <div><label className="text-xs text-gray-500 block mb-1">시작일</label>
                      <input type="date" className="border border-gray-300 text-sm px-2 py-1.5 rounded w-full" />
                    </div>
                    <div><label className="text-xs text-gray-500 block mb-1">종료일</label>
                      <input type="date" className="border border-gray-300 text-sm px-2 py-1.5 rounded w-full" />
                    </div>
                  </div>
                  <div><label className="text-xs text-gray-500 block mb-1">연결 URL</label>
                    <input defaultValue={selected?.linkUrl} placeholder="/event/..." className="border border-gray-300 text-sm px-2 py-1.5 rounded w-full font-mono" />
                  </div>
                  <div className="flex gap-2">
                    <button onClick={() => setEditing(false)} className="flex-1 px-3 py-1.5 border border-gray-300 text-sm rounded">취소</button>
                    <button className="flex-1 px-3 py-1.5 bg-kb-yellow text-white text-sm font-bold rounded hover:bg-kb-yellow-dark transition-colors">저장</button>
                  </div>
                </div>
              ) : (
                <div className="p-8 text-center text-sm text-gray-400">배너를 선택하면 미리보기가 표시됩니다.</div>
              )}
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
