'use client'
import { useCallback, useEffect, useState } from 'react'
import Link from 'next/link'
import AdminSidebar from '@/components/admin/AdminSidebar'
import {
  searchCustomers, CustomerSummary, STATUS_LABEL, STATUS_CODE,
} from '@/lib/admin-customer-api'

const STATUS_COLOR: Record<string, string> = {
  '활성': 'bg-green-100 text-green-700',
  '휴면': 'bg-gray-100 text-gray-500',
  '정지': 'bg-red-100 text-red-700',
  '탈퇴': 'bg-gray-100 text-gray-400',
}

/** ISO 일시 → YYYY-MM-DD */
function fmtDate(v: string | null): string {
  return v ? v.slice(0, 10) : '-'
}
/** ISO 일시 → YYYY-MM-DD HH:mm */
function fmtDateTime(v: string | null): string {
  return v ? v.slice(0, 16).replace('T', ' ') : '-'
}

const PAGE_SIZE = 20

export default function MembersPage() {
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('전체')
  const [rows, setRows] = useState<CustomerSummary[]>([])
  const [total, setTotal] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // override 가 주어지면(초기화 등) 그 값으로 조회한다. setState 는 비동기라
  // 같은 핸들러에서 비운 search/statusFilter 가 즉시 반영되지 않으므로 명시 전달이 필요.
  const load = useCallback(async (pageNo: number, override?: { keyword?: string; status?: string }) => {
    setLoading(true)
    setError(null)
    try {
      const res = await searchCustomers({
        keyword: override ? override.keyword : (search.trim() || undefined),
        status: override ? override.status : (statusFilter === '전체' ? undefined : STATUS_CODE[statusFilter]),
        page: pageNo,
        size: PAGE_SIZE,
      })
      setRows(res.content)
      setTotal(res.totalElements)
      setTotalPages(res.totalPages)
      setPage(res.number)
    } catch (e: unknown) {
      const status = (e as { response?: { status?: number } })?.response?.status
      setError(status === 403 ? '접근 권한이 없습니다 (직원 역할 필요).' : '회원 목록을 불러오지 못했습니다.')
      setRows([])
      setTotal(0)
      setTotalPages(0)
    } finally {
      setLoading(false)
    }
  }, [search, statusFilter])

  // 최초 1회 로드 (이후는 조회 버튼/페이지 이동으로 트리거)
  useEffect(() => { load(0) }, []) // eslint-disable-line react-hooks/exhaustive-deps

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
            <input
              value={search}
              onChange={e => setSearch(e.target.value)}
              onKeyDown={e => { if (e.key === 'Enter') load(0) }}
              placeholder="이름 / 휴대폰"
              className="border border-gray-300 text-xs px-2 py-1.5 rounded w-48"
            />
            <div className="flex items-center gap-1.5">
              <span className="text-xs text-gray-500">회원상태</span>
              <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)} className="border border-gray-300 text-xs px-2 py-1 rounded bg-white">
                {['전체', '활성', '휴면', '정지', '탈퇴'].map(o => <option key={o}>{o}</option>)}
              </select>
            </div>
            <button onClick={() => load(0)} disabled={loading} className="ml-auto px-3 py-1.5 bg-kb-yellow text-white text-xs font-bold rounded hover:bg-kb-yellow-dark transition-colors disabled:opacity-50">
              {loading ? '조회 중…' : '조회'}
            </button>
            <button onClick={() => { setSearch(''); setStatusFilter('전체'); setPage(0); load(0, {}) }} className="text-xs border border-gray-300 px-3 py-1.5 rounded text-gray-600">초기화</button>
          </div>

          <div className="flex items-center justify-between mb-2">
            <p className="text-xs text-gray-500">총 {total.toLocaleString()}명 — 현재 조회 {rows.length}건</p>
          </div>

          {error && (
            <div className="mb-3 bg-red-50 border border-red-200 rounded px-4 py-2.5 text-xs text-red-700">{error}</div>
          )}

          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  {['고객번호', '이름', '휴대폰', '이메일', '등급', '회원상태', '최근거래', '가입일', '작업'].map(h => (
                    <th key={h} className="px-3 py-2.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {rows.map(m => {
                  const label = STATUS_LABEL[m.customerStatusCode] ?? m.customerStatusCode
                  return (
                    <tr key={m.customerId} className="hover:bg-kb-beige-light">
                      <td className="px-3 py-2.5 text-xs text-blue-600 font-mono">{m.customerId}</td>
                      <td className="px-3 py-2.5 font-medium">{m.partyName}</td>
                      <td className="px-3 py-2.5 text-gray-500">{m.phone ?? '-'}</td>
                      <td className="px-3 py-2.5 text-xs text-gray-400">{m.email ?? '-'}</td>
                      <td className="px-3 py-2.5 text-gray-600 text-xs">{m.customerGradeCode ?? '-'}</td>
                      <td className="px-3 py-2.5"><span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${STATUS_COLOR[label] ?? 'bg-gray-100 text-gray-500'}`}>{label}</span></td>
                      <td className="px-3 py-2.5 text-xs text-gray-400">{fmtDateTime(m.lastTransactionAt)}</td>
                      <td className="px-3 py-2.5 text-xs text-gray-400">{fmtDate(m.joinedAt)}</td>
                      <td className="px-3 py-2.5">
                        <Link href={`/admin/members/${m.customerId}`} className="text-xs border border-gray-300 px-2 py-0.5 rounded text-gray-600 hover:bg-kb-beige-light">상세</Link>
                      </td>
                    </tr>
                  )
                })}
                {!loading && rows.length === 0 && !error && (
                  <tr><td colSpan={9} className="px-3 py-8 text-center text-gray-400 text-sm">조회 결과가 없습니다.</td></tr>
                )}
              </tbody>
            </table>
          </div>

          {/* 페이지네이션 */}
          {totalPages > 1 && (
            <div className="flex justify-center gap-1 mt-4">
              {Array.from({ length: totalPages }, (_, i) => i).slice(0, 10).map(p => (
                <button key={p} onClick={() => load(p)} className={`w-7 h-7 text-xs rounded ${p === page ? 'bg-yellow-400 font-bold' : 'border border-gray-300 text-gray-600'}`}>{p + 1}</button>
              ))}
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
