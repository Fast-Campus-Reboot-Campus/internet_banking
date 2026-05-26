'use client'
import { useState } from 'react'
import Link from 'next/link'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { MOCK_MEMBERS, MemberRecord, MemberStatus, RiskLevel } from '@/lib/admin-mock-data'

const STATUS_COLOR: Record<MemberStatus, string> = {
  '활성': 'bg-green-100 text-green-700',
  '휴면': 'bg-gray-100 text-gray-500',
  '정지': 'bg-red-100 text-red-700',
  '탈퇴': 'bg-gray-100 text-gray-400',
}
const RISK_COLOR: Record<RiskLevel, string> = {
  '저': 'bg-green-100 text-green-700',
  '중': 'bg-yellow-100 text-yellow-700',
  '고': 'bg-red-100 text-red-700',
}

export default function MembersPage() {
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('전체')

  const filtered = MOCK_MEMBERS.filter(m =>
    (statusFilter === '전체' || m.status === statusFilter) &&
    (m.name.includes(search) || m.id.includes(search) || m.phone.includes(search))
  )

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          회원관리 &gt; <span className="text-gray-700 font-medium">회원 목록</span>
        </div>
        <div className="px-6 py-5">
          <h1 className="text-lg font-bold text-gray-800 mb-4">회원 목록/검색</h1>

          <div className="flex items-center gap-3 mb-4 bg-white border border-kb-border rounded-lg px-4 py-3 shadow-sm flex-wrap">
            <input value={search} onChange={e=>setSearch(e.target.value)} placeholder="고객번호 / 이름 / 휴대폰" className="border border-gray-300 text-xs px-2 py-1.5 rounded w-48" />
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">회원구분</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white"><option>전체</option><option>뱅킹이체회원</option><option>조회용ID 회원</option></select>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">회원상태</span>
              <select value={statusFilter} onChange={e=>setStatusFilter(e.target.value)} className="border border-gray-300 text-xs px-2 py-1 rounded bg-white">
                {['전체','활성','휴면','정지','탈퇴'].map(o=><option key={o}>{o}</option>)}
              </select>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">위험등급</span>
              <select className="border border-gray-300 text-xs px-2 py-1 rounded bg-white"><option>전체</option><option>저</option><option>중</option><option>고</option></select>
            </div>
            <span className="text-xs text-gray-400">가입일 2024-01-01 ~ 2026-05-11</span>
            <button className="ml-auto px-3 py-1.5 bg-kb-yellow text-white text-xs font-bold rounded hover:bg-kb-yellow-dark transition-colors">조회</button>
            <button className="text-xs border border-gray-300 px-3 py-1.5 rounded text-gray-600">초기화</button>
          </div>

          <div className="flex items-center justify-between mb-2">
            <p className="text-xs text-gray-500">총 2,341,872명 (활성 2,189,402 / 정지 12,043 / 탈퇴 140,427) — 현재 조회 {filtered.length}건</p>
            <div className="flex gap-2">
              <button className="text-xs border border-gray-300 px-3 py-1 rounded text-gray-600">엑셀 다운로드</button>
              <button className="text-xs bg-orange-100 text-orange-700 border border-orange-300 px-3 py-1 rounded">상태 일괄변경</button>
            </div>
          </div>

          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  <th className="w-8 px-3 py-2.5"><input type="checkbox" /></th>
                  {['고객번호','이름','생년월일','휴대폰','회원구분','회원상태','위험등급','최근 로그인','가입일','작업'].map(h=>(
                    <th key={h} className="px-3 py-2.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filtered.map(m => (
                  <tr key={m.id} className="hover:bg-kb-beige-light">
                    <td className="px-3 py-2.5"><input type="checkbox" /></td>
                    <td className="px-3 py-2.5 text-xs text-blue-600 font-mono">{m.id}</td>
                    <td className="px-3 py-2.5 font-medium">{m.name}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-400">{m.birthDate}</td>
                    <td className="px-3 py-2.5 text-gray-500">{m.phone}</td>
                    <td className="px-3 py-2.5 text-gray-600 text-xs">{m.memberType}</td>
                    <td className="px-3 py-2.5"><span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${STATUS_COLOR[m.status]}`}>{m.status}</span></td>
                    <td className="px-3 py-2.5"><span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${RISK_COLOR[m.riskLevel]}`}>{m.riskLevel}</span></td>
                    <td className="px-3 py-2.5 text-xs text-gray-400">{m.lastLogin}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-400">{m.joinedAt}</td>
                    <td className="px-3 py-2.5">
                      <Link href={`/admin/members/${m.id}`} className="text-xs border border-gray-300 px-2 py-0.5 rounded text-gray-600 hover:bg-kb-beige-light">상세</Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {/* 페이지네이션 */}
          <div className="flex justify-center gap-1 mt-4">
            {[1,2,3,4,5].map(p => (
              <button key={p} className={`w-7 h-7 text-xs rounded ${p===1 ? 'bg-yellow-400 font-bold' : 'border border-gray-300 text-gray-600'}`}>{p}</button>
            ))}
            <button className="w-7 h-7 text-xs rounded border border-gray-300 text-gray-600">›</button>
          </div>
        </div>
      </main>
    </div>
  )
}
