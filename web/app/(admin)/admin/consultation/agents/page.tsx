'use client'

import { useEffect, useState } from 'react'
import AdminSidebar from '@/components/admin/AdminSidebar'
import ConsultationTabs from '@/components/admin/ConsultationTabs'
import { listAgents, createAgent, updateAgent, deactivateAgent, AgentAccount } from '@/lib/consultation-api'

const ROLE_LABEL: Record<string, string> = { AGENT: '상담원', ADMIN: '관리자', SUPERVISOR: '슈퍼바이저' }
const emptyForm = { login_id: '', password: '', name: '', role: 'AGENT' }

export default function AgentsPage() {
  const [agents, setAgents] = useState<AgentAccount[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const [showCreate, setShowCreate] = useState(false)
  const [form, setForm] = useState(emptyForm)
  const [creating, setCreating] = useState(false)
  const [formError, setFormError] = useState('')

  const [editTarget, setEditTarget] = useState<AgentAccount | null>(null)
  const [editForm, setEditForm] = useState({ name: '', role: 'AGENT', password: '' })
  const [editing, setEditing] = useState(false)

  const load = async () => {
    setLoading(true); setError('')
    try { setAgents(await listAgents()) }
    catch { setError('목록을 불러오지 못했습니다. 상담 서비스 상태를 확인하세요.') }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const flash = (msg: string) => { setNotice(msg); setTimeout(() => setNotice(''), 3000) }

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault()
    if (!form.login_id || !form.password || !form.name) { setFormError('모든 항목을 입력하세요.'); return }
    setCreating(true); setFormError('')
    try {
      await createAgent(form)
      setShowCreate(false); setForm(emptyForm)
      flash('상담원 계정이 생성되었습니다.'); await load()
    } catch (err: unknown) {
      const e = err as { response?: { data?: { detail?: string } } }
      setFormError(e.response?.data?.detail ?? '생성에 실패했습니다.')
    } finally { setCreating(false) }
  }

  function openEdit(a: AgentAccount) {
    setEditTarget(a); setEditForm({ name: a.name, role: a.role, password: '' })
  }

  async function handleEdit(e: React.FormEvent) {
    e.preventDefault()
    if (!editTarget) return
    setEditing(true)
    try {
      await updateAgent(editTarget.employee_id, { name: editForm.name, role: editForm.role, password: editForm.password || undefined })
      setEditTarget(null); flash('수정되었습니다.'); await load()
    } catch { flash('수정에 실패했습니다.') }
    finally { setEditing(false) }
  }

  async function handleDeactivate(a: AgentAccount) {
    if (!confirm(`${a.name}(${a.login_id}) 계정을 비활성화하시겠습니까?`)) return
    try { await deactivateAgent(a.employee_id); flash('비활성화되었습니다.'); await load() }
    catch { flash('처리에 실패했습니다.') }
  }

  async function handleActivate(a: AgentAccount) {
    try { await updateAgent(a.employee_id, { status: 'ACTIVE' }); flash('활성화되었습니다.'); await load() }
    catch { flash('처리에 실패했습니다.') }
  }

  const inputCls = 'w-full border border-gray-300 text-xs px-2 py-1.5 rounded'
  const labelCls = 'block text-xs text-gray-600 mb-1'

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />
      <main className="flex-1 overflow-auto">

        <div className="bg-white border-b border-kb-border px-6 py-3 text-xs text-kb-text-muted">
          상담 &gt; <span className="text-gray-700 font-medium">상담원 계정 관리</span>
        </div>

        <ConsultationTabs />

        <div className="px-6 py-5">
          <div className="flex items-center justify-between mb-4">
            <h1 className="text-lg font-bold text-gray-800">상담원 계정 관리</h1>
            <button
              onClick={() => setShowCreate(true)}
              className="px-3 py-1.5 bg-kb-yellow text-white text-xs font-bold rounded hover:bg-kb-yellow-dark transition-colors"
            >
              + 신규 등록
            </button>
          </div>

          {notice && (
            <div className="mb-3 bg-green-50 border border-green-200 rounded px-4 py-2.5 text-xs text-green-700">{notice}</div>
          )}
          {error && (
            <div className="mb-3 bg-red-50 border border-red-200 rounded px-4 py-2.5 text-xs text-red-700">{error}</div>
          )}

          <div className="bg-white border border-kb-border rounded-lg overflow-hidden shadow-sm">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light border-b border-kb-border text-xs text-kb-text-muted">
                  {['ID', '아이디', '이름', '역할', '상태', '등록일', '관리'].map(h => (
                    <th key={h} className="px-3 py-2.5 text-left font-medium whitespace-nowrap">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {loading && (
                  <tr><td colSpan={7} className="px-3 py-8 text-center text-gray-400 text-sm">불러오는 중…</td></tr>
                )}
                {!loading && agents.length === 0 && (
                  <tr><td colSpan={7} className="px-3 py-8 text-center text-gray-400 text-sm">등록된 상담원이 없습니다.</td></tr>
                )}
                {agents.map(a => (
                  <tr key={a.employee_id} className="hover:bg-kb-beige-light">
                    <td className="px-3 py-2.5 text-xs text-blue-600 font-mono">#{a.employee_id}</td>
                    <td className="px-3 py-2.5 font-mono text-xs text-gray-700">{a.login_id}</td>
                    <td className="px-3 py-2.5 font-medium">{a.name}</td>
                    <td className="px-3 py-2.5 text-xs text-gray-500">{ROLE_LABEL[a.role] ?? a.role}</td>
                    <td className="px-3 py-2.5">
                      {a.status === 'ACTIVE'
                        ? <span className="text-xs px-1.5 py-0.5 rounded-full font-medium bg-green-100 text-green-700">활성</span>
                        : <span className="text-xs px-1.5 py-0.5 rounded-full font-medium bg-gray-100 text-gray-500">비활성</span>}
                    </td>
                    <td className="px-3 py-2.5 text-xs text-gray-400">{a.created_at ? a.created_at.slice(0, 10) : '-'}</td>
                    <td className="px-3 py-2.5 flex gap-2">
                      <button onClick={() => openEdit(a)} className="text-xs border border-gray-300 px-2 py-0.5 rounded text-gray-600 hover:bg-kb-beige-light">수정</button>
                      {a.status === 'ACTIVE'
                        ? <button onClick={() => handleDeactivate(a)} className="text-xs border border-red-200 px-2 py-0.5 rounded text-red-500 hover:bg-red-50">비활성화</button>
                        : <button onClick={() => handleActivate(a)} className="text-xs border border-green-200 px-2 py-0.5 rounded text-green-600 hover:bg-green-50">활성화</button>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </main>

      {/* 신규 등록 모달 */}
      {showCreate && (
        <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
          <div className="w-80 bg-white rounded-xl shadow-lg border border-gray-200 px-8 py-7">
            <h2 className="text-sm font-bold text-gray-800 mb-5">신규 상담원 등록</h2>
            <form onSubmit={handleCreate} className="space-y-3">
              {[
                { label: '아이디', key: 'login_id', type: 'text', placeholder: 'agent01' },
                { label: '비밀번호', key: 'password', type: 'password', placeholder: '초기 비밀번호' },
                { label: '이름', key: 'name', type: 'text', placeholder: '홍길동' },
              ].map(f => (
                <div key={f.key}>
                  <label className={labelCls}>{f.label}</label>
                  <input type={f.type} placeholder={f.placeholder}
                    value={(form as Record<string, string>)[f.key]}
                    onChange={e => setForm(p => ({ ...p, [f.key]: e.target.value }))}
                    className={inputCls} />
                </div>
              ))}
              <div>
                <label className={labelCls}>역할</label>
                <select value={form.role} onChange={e => setForm(p => ({ ...p, role: e.target.value }))} className={inputCls}>
                  <option value="AGENT">상담원</option>
                  <option value="SUPERVISOR">슈퍼바이저</option>
                  <option value="ADMIN">관리자</option>
                </select>
              </div>
              {formError && <p className="text-xs text-red-500">{formError}</p>}
              <div className="flex gap-2 pt-1">
                <button type="submit" disabled={creating}
                  className="flex-1 py-1.5 bg-kb-yellow text-white text-xs font-bold rounded hover:bg-kb-yellow-dark disabled:opacity-50">
                  {creating ? '등록 중…' : '등록'}
                </button>
                <button type="button" onClick={() => { setShowCreate(false); setFormError('') }}
                  className="flex-1 py-1.5 border border-gray-300 text-gray-600 text-xs rounded hover:bg-gray-50">
                  취소
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 수정 모달 */}
      {editTarget && (
        <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
          <div className="w-80 bg-white rounded-xl shadow-lg border border-gray-200 px-8 py-7">
            <h2 className="text-sm font-bold text-gray-800 mb-1">상담원 정보 수정</h2>
            <p className="text-xs text-gray-400 mb-4 font-mono">{editTarget.login_id}</p>
            <form onSubmit={handleEdit} className="space-y-3">
              <div>
                <label className={labelCls}>이름</label>
                <input type="text" value={editForm.name}
                  onChange={e => setEditForm(p => ({ ...p, name: e.target.value }))} className={inputCls} />
              </div>
              <div>
                <label className={labelCls}>역할</label>
                <select value={editForm.role} onChange={e => setEditForm(p => ({ ...p, role: e.target.value }))} className={inputCls}>
                  <option value="AGENT">상담원</option>
                  <option value="SUPERVISOR">슈퍼바이저</option>
                  <option value="ADMIN">관리자</option>
                </select>
              </div>
              <div>
                <label className={labelCls}>새 비밀번호 (변경 시만 입력)</label>
                <input type="password" placeholder="비워두면 변경 안 함"
                  value={editForm.password}
                  onChange={e => setEditForm(p => ({ ...p, password: e.target.value }))} className={inputCls} />
              </div>
              <div className="flex gap-2 pt-1">
                <button type="submit" disabled={editing}
                  className="flex-1 py-1.5 bg-kb-yellow text-white text-xs font-bold rounded hover:bg-kb-yellow-dark disabled:opacity-50">
                  {editing ? '저장 중…' : '저장'}
                </button>
                <button type="button" onClick={() => setEditTarget(null)}
                  className="flex-1 py-1.5 border border-gray-300 text-gray-600 text-xs rounded hover:bg-gray-50">
                  취소
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
