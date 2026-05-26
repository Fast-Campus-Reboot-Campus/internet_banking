'use client'
import { use } from 'react'
import Link from 'next/link'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { MOCK_MEMBERS } from '@/lib/admin-mock-data'

export default function MemberDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params)
  const member = MOCK_MEMBERS.find(m => m.id === id) ?? {
    id, name: '알 수 없음', birthDate: '-', phone: '-',
    memberType: '-', status: '활성' as const, riskLevel: '저' as const,
    lastLogin: '-', joinedAt: '-',
  }

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">
        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          회원관리 &gt; <Link href="/admin/members" className="hover:underline">회원 목록</Link> &gt; <span className="text-gray-700 font-medium">회원 상세</span>
        </div>
        <div className="px-6 py-5 max-w-4xl">
          <div className="flex items-center justify-between mb-5">
            <h1 className="text-lg font-bold text-gray-800">회원 상세 — {member.id}</h1>
            <Link href="/admin/members" className="text-xs border border-gray-300 px-3 py-1.5 rounded text-gray-600 hover:bg-gray-50">← 목록으로</Link>
          </div>

          {/* 기본 정보 */}
          <div className="bg-white border border-kb-border rounded-lg mb-4 shadow-sm">
            <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light">
              <h2 className="text-sm font-semibold text-gray-700">기본 정보</h2>
            </div>
            <div className="grid grid-cols-3 gap-0 divide-x divide-y divide-gray-100">
              {[
                ['고객번호', member.id],
                ['이름', member.name],
                ['생년월일', member.birthDate],
                ['휴대폰', member.phone],
                ['회원구분', member.memberType],
                ['가입일', member.joinedAt],
              ].map(([label, value]) => (
                <div key={label} className="px-4 py-3">
                  <p className="text-xs text-gray-400 mb-0.5">{label}</p>
                  <p className="text-sm font-medium text-gray-800">{value}</p>
                </div>
              ))}
            </div>
          </div>

          {/* 상태 및 위험 */}
          <div className="bg-white border border-kb-border rounded-lg mb-4 shadow-sm">
            <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light">
              <h2 className="text-sm font-semibold text-gray-700">상태 / 위험등급</h2>
            </div>
            <div className="grid grid-cols-3 gap-0 divide-x divide-y divide-gray-100">
              <div className="px-4 py-3">
                <p className="text-xs text-gray-400 mb-0.5">회원 상태</p>
                <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                  member.status === '활성' ? 'bg-green-100 text-green-700' :
                  member.status === '정지' ? 'bg-red-100 text-red-700' :
                  'bg-gray-100 text-gray-500'
                }`}>{member.status}</span>
              </div>
              <div className="px-4 py-3">
                <p className="text-xs text-gray-400 mb-0.5">위험등급</p>
                <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                  member.riskLevel === '저' ? 'bg-green-100 text-green-700' :
                  member.riskLevel === '중' ? 'bg-yellow-100 text-yellow-700' :
                  'bg-red-100 text-red-700'
                }`}>{member.riskLevel}</span>
              </div>
              <div className="px-4 py-3">
                <p className="text-xs text-gray-400 mb-0.5">최근 로그인</p>
                <p className="text-sm font-medium text-gray-800">{member.lastLogin}</p>
              </div>
            </div>
          </div>

          {/* 계좌 현황 */}
          <div className="bg-white border border-kb-border rounded-lg mb-4 shadow-sm">
            <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light">
              <h2 className="text-sm font-semibold text-gray-700">보유 계좌</h2>
            </div>
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  {['계좌번호','상품명','잔액','개설일','상태'].map(h => (
                    <th key={h} className="px-4 py-2.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                <tr>
                  <td className="px-4 py-2.5 font-mono text-xs text-gray-600">012-34-5678901</td>
                  <td className="px-4 py-2.5">KB스타★통장</td>
                  <td className="px-4 py-2.5 text-right font-medium">12,500,000원</td>
                  <td className="px-4 py-2.5 text-xs text-gray-400">{member.joinedAt}</td>
                  <td className="px-4 py-2.5"><span className="text-xs px-1.5 py-0.5 bg-green-100 text-green-700 rounded-full">정상</span></td>
                </tr>
                <tr>
                  <td className="px-4 py-2.5 font-mono text-xs text-gray-600">012-34-5678902</td>
                  <td className="px-4 py-2.5">KB플러스저축예금</td>
                  <td className="px-4 py-2.5 text-right font-medium">5,200,000원</td>
                  <td className="px-4 py-2.5 text-xs text-gray-400">2022-11-15</td>
                  <td className="px-4 py-2.5"><span className="text-xs px-1.5 py-0.5 bg-green-100 text-green-700 rounded-full">정상</span></td>
                </tr>
              </tbody>
            </table>
          </div>

          {/* 동의 이력 요약 */}
          <div className="bg-white border border-kb-border rounded-lg mb-4 shadow-sm">
            <div className="px-4 py-3 border-b border-gray-100 bg-kb-beige-light">
              <h2 className="text-sm font-semibold text-gray-700">동의 현황</h2>
            </div>
            <div className="grid grid-cols-4 divide-x divide-gray-100">
              {[
                { label: '전자금융거래 기본약관', value: '동의', color: 'text-green-600' },
                { label: '개인정보 수집·이용', value: '동의', color: 'text-green-600' },
                { label: '[은행] 마케팅', value: '미동의', color: 'text-gray-400' },
                { label: '[계열사] 마케팅', value: '미동의', color: 'text-gray-400' },
              ].map(item => (
                <div key={item.label} className="px-4 py-3 text-center">
                  <p className="text-xs text-gray-400 mb-1">{item.label}</p>
                  <p className={`text-sm font-medium ${item.color}`}>{item.value}</p>
                </div>
              ))}
            </div>
          </div>

          {/* 액션 버튼 */}
          <div className="flex gap-2 justify-end">
            <button className="px-4 py-2 border border-gray-300 text-sm rounded text-gray-600">동의이력 조회</button>
            <button className="px-4 py-2 border border-orange-400 text-sm rounded text-orange-600">상태 변경</button>
            <button className="px-4 py-2 bg-kb-yellow text-white text-sm font-bold rounded hover:bg-kb-yellow-dark transition-colors">EDD 접수</button>
          </div>
        </div>
      </main>
    </div>
  )
}
