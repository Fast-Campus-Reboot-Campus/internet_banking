'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import AdminSidebar from '@/components/admin/AdminSidebar'
import { AdminUser } from '@/lib/admin-mock-data'
import {
  fetchTopBiasRiskScores, fetchRecentOpinions, fetchQuarantineReports,
  ReviewerRiskScoreDto, AiAuditOpinionDto, QuarantineReportDto,
} from '@/lib/audit-api'
import { biasRiskLevel, conclusionLabel, analysisTypeLabel, ConclusionCd, AnalysisTypeCd } from '@/lib/audit-mock-data'

export default function AuditDashboardPage() {
  const [adminUser, setAdminUser] = useState<AdminUser | null>(null)
  const [riskScores, setRiskScores] = useState<ReviewerRiskScoreDto[]>([])
  const [opinions,   setOpinions]   = useState<AiAuditOpinionDto[]>([])
  const [quarantine, setQuarantine] = useState<QuarantineReportDto[]>([])
  const [loading,    setLoading]    = useState(true)

  useEffect(() => {
    try {
      const s = localStorage.getItem('admin_user')
      if (s) setAdminUser(JSON.parse(s))
    } catch {}
  }, [])

  useEffect(() => {
    if (!adminUser) return
    Promise.all([
      fetchTopBiasRiskScores(20),
      fetchRecentOpinions(20),
      fetchQuarantineReports(),
    ]).then(([scores, ops, quar]) => {
      setRiskScores(scores)
      setOpinions(ops)
      setQuarantine(quar)
    }).catch(() => {}).finally(() => setLoading(false))
  }, [adminUser])

  if (!adminUser) return null

  const biasSuspected      = opinions.filter((o) => o.conclusionCd === 'BIAS_SUSPECTED').length
  const violationSuspected = opinions.filter((o) => o.conclusionCd === 'VIOLATION_SUSPECTED').length
  const highRiskReviewers  = riskScores.filter((r) => r.biasScore >= 50 || r.complianceScore >= 40).length

  return (
    <div className="flex min-h-screen bg-kb-beige-light">
      <AdminSidebar />

      <main className="flex-1 overflow-auto">
        {/* 상단 바 */}
        <div className="bg-white border-b border-kb-border px-8 py-4 flex items-center justify-between">
          <h1 className="text-lg font-bold text-gray-800">AI 감사 대시보드</h1>
          <span className="text-sm text-gray-400">
            {new Date().toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' })} 기준
          </span>
        </div>

        <div className="px-8 py-6 space-y-6">

          {/* 요약 카드 */}
          <div className="grid grid-cols-4 gap-4">
            <SummaryCard
              label="격리 리포트"
              value={quarantine.length}
              sub="재심사 대기"
              color="red"
              href="/admin/audit/quarantine"
            />
            <SummaryCard label="편향 의심" value={biasSuspected} sub="BIAS_SUSPECTED" color="orange" />
            <SummaryCard label="규정 위반 의심" value={violationSuspected} sub="VIOLATION_SUSPECTED" color="yellow" />
            <SummaryCard label="고위험 심사관" value={highRiskReviewers} sub="bias ≥ 50 또는 compliance ≥ 40" color="purple" />
          </div>

          {loading && (
            <p className="text-sm text-gray-400 text-center py-8">데이터 조회 중...</p>
          )}

          {/* 심사관 위험도 순위 */}
          <section className="bg-white border border-kb-border rounded-lg shadow-sm">
            <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
              <div>
                <h2 className="text-sm font-bold text-gray-700">심사관 위험도 순위</h2>
                <p className="text-xs text-gray-400 mt-0.5">bias_score 내림차순. 최근 평가 기준.</p>
              </div>
            </div>
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light text-xs text-kb-text-muted uppercase">
                  <th className="px-5 py-2.5 text-left font-medium w-8">#</th>
                  <th className="px-5 py-2.5 text-left font-medium">심사관 ID</th>
                  <th className="px-5 py-2.5 text-center font-medium">편향 스코어</th>
                  <th className="px-5 py-2.5 text-center font-medium">규정준수 스코어</th>
                  <th className="px-5 py-2.5 text-center font-medium">평가 건수</th>
                  <th className="px-5 py-2.5 text-left font-medium">최근 평가</th>
                  <th className="px-5 py-2.5 text-center font-medium">위험도</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {riskScores.length === 0 && !loading ? (
                  <tr>
                    <td colSpan={7} className="px-5 py-8 text-center text-sm text-gray-400">
                      심사관 위험도 데이터가 없습니다.
                    </td>
                  </tr>
                ) : riskScores.map((r, idx) => {
                  const level = biasRiskLevel(r.biasScore)
                  return (
                    <tr key={r.reviewerId} className="hover:bg-kb-beige-light transition-colors">
                      <td className="px-5 py-3 text-gray-400 text-xs">{idx + 1}</td>
                      <td className="px-5 py-3 font-mono text-xs text-gray-700">#{r.reviewerId}</td>
                      <td className="px-5 py-3 text-center">
                        <BiasScoreBar score={r.biasScore} level={level} />
                      </td>
                      <td className="px-5 py-3 text-center">
                        <ComplianceScoreBar score={r.complianceScore} />
                      </td>
                      <td className="px-5 py-3 text-center text-gray-600">{r.evaluationCount}건</td>
                      <td className="px-5 py-3 text-gray-400 text-xs">
                        {r.lastEvaluatedAt ? new Date(r.lastEvaluatedAt).toLocaleString('ko-KR') : '-'}
                      </td>
                      <td className="px-5 py-3 text-center">
                        <RiskLevelBadge level={level} />
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </section>

          {/* 최근 AI 감사 의견 */}
          <section className="bg-white border border-kb-border rounded-lg shadow-sm">
            <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
              <div>
                <h2 className="text-sm font-bold text-gray-700">최근 AI 감사 의견</h2>
                <p className="text-xs text-gray-400 mt-0.5">LLM 분석 결과 최신순.</p>
              </div>
            </div>
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-kb-beige-light text-xs text-kb-text-muted uppercase">
                  <th className="px-5 py-2.5 text-left font-medium">심사 ID</th>
                  <th className="px-5 py-2.5 text-left font-medium">심사관 ID</th>
                  <th className="px-5 py-2.5 text-left font-medium">분석 유형</th>
                  <th className="px-5 py-2.5 text-left font-medium">결론</th>
                  <th className="px-5 py-2.5 text-center font-medium">신뢰도</th>
                  <th className="px-5 py-2.5 text-left font-medium w-80">근거 요약</th>
                  <th className="px-5 py-2.5 text-left font-medium">생성 시각</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {opinions.length === 0 && !loading ? (
                  <tr>
                    <td colSpan={7} className="px-5 py-8 text-center text-sm text-gray-400">
                      AI 감사 의견이 없습니다.
                    </td>
                  </tr>
                ) : opinions.map((op) => (
                  <tr key={op.opinionId} className="hover:bg-kb-beige-light transition-colors">
                    <td className="px-5 py-3 text-gray-500 font-mono text-xs">rev-{op.revId}</td>
                    <td className="px-5 py-3 font-mono text-xs text-gray-700">#{op.reviewerId}</td>
                    <td className="px-5 py-3">
                      <AnalysisTypeBadge type={op.analysisTypeCd as AnalysisTypeCd} />
                    </td>
                    <td className="px-5 py-3">
                      <ConclusionBadge cd={op.conclusionCd as ConclusionCd} />
                    </td>
                    <td className="px-5 py-3 text-center">
                      <ConfidenceBar score={op.confidenceScore} />
                    </td>
                    <td className="px-5 py-3 text-gray-500 text-xs leading-relaxed max-w-xs">
                      <p className="line-clamp-2">{op.reasoningSummary}</p>
                    </td>
                    <td className="px-5 py-3 text-gray-400 text-xs">
                      {new Date(op.generatedAt).toLocaleString('ko-KR')}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>

        </div>
      </main>
    </div>
  )
}

// ── 하위 컴포넌트 ───────────────────────────────────────────────

function SummaryCard({
  label, value, sub, color, href,
}: {
  label: string; value: number; sub: string; color: 'red' | 'orange' | 'yellow' | 'purple'; href?: string
}) {
  const colors = {
    red:    'border-red-200 bg-red-50',
    orange: 'border-orange-200 bg-orange-50',
    yellow: 'border-yellow-200 bg-yellow-50',
    purple: 'border-purple-200 bg-purple-50',
  }
  const textColors = {
    red: 'text-red-700', orange: 'text-orange-700', yellow: 'text-yellow-700', purple: 'text-purple-700',
  }
  const content = (
    <div className={`border rounded px-5 py-4 ${colors[color]} ${href ? 'cursor-pointer hover:shadow-md transition-shadow' : ''}`}>
      <p className={`text-xs font-medium opacity-70 ${textColors[color]}`}>{label}</p>
      <p className={`text-3xl font-bold mt-1 ${textColors[color]}`}>{value}</p>
      <p className={`text-xs opacity-60 mt-0.5 ${textColors[color]}`}>{sub}</p>
    </div>
  )
  return href ? <Link href={href}>{content}</Link> : content
}

function BiasScoreBar({ score, level }: { score: number; level: string }) {
  const barColor = level === 'critical' ? 'bg-red-500' : level === 'high' ? 'bg-orange-400' : level === 'medium' ? 'bg-yellow-400' : 'bg-green-400'
  return (
    <div className="flex items-center gap-2 justify-center">
      <div className="w-20 h-1.5 bg-gray-100 rounded-full overflow-hidden">
        <div className={`h-full rounded-full ${barColor}`} style={{ width: `${Math.min(score, 100)}%` }} />
      </div>
      <span className="text-xs font-mono text-gray-700 w-6 text-right">{score}</span>
    </div>
  )
}

function ComplianceScoreBar({ score }: { score: number }) {
  const barColor = score >= 40 ? 'bg-red-500' : score >= 20 ? 'bg-orange-400' : 'bg-green-400'
  return (
    <div className="flex items-center gap-2 justify-center">
      <div className="w-20 h-1.5 bg-gray-100 rounded-full overflow-hidden">
        <div className={`h-full rounded-full ${barColor}`} style={{ width: `${Math.min(score, 100)}%` }} />
      </div>
      <span className="text-xs font-mono text-gray-700 w-6 text-right">{score}</span>
    </div>
  )
}

function RiskLevelBadge({ level }: { level: string }) {
  const styles = {
    critical: 'bg-red-100 text-red-700',
    high:     'bg-orange-100 text-orange-700',
    medium:   'bg-yellow-100 text-yellow-700',
    low:      'bg-green-100 text-green-700',
  } as Record<string, string>
  const labels = { critical: '매우위험', high: '고위험', medium: '중위험', low: '저위험' } as Record<string, string>
  return <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${styles[level] ?? ''}`}>{labels[level] ?? level}</span>
}

function ConclusionBadge({ cd }: { cd: ConclusionCd }) {
  const styles: Record<ConclusionCd, string> = {
    BIAS_SUSPECTED:      'bg-red-100 text-red-700 border border-red-200',
    NO_BIAS_DETECTED:    'bg-green-100 text-green-700 border border-green-200',
    VIOLATION_SUSPECTED: 'bg-orange-100 text-orange-700 border border-orange-200',
    COMPLIANT:           'bg-green-100 text-green-700 border border-green-200',
    INSUFFICIENT_DATA:   'bg-gray-100 text-gray-500 border border-gray-200',
  }
  return (
    <span className={`text-xs px-2 py-0.5 rounded font-medium whitespace-nowrap ${styles[cd] ?? 'bg-gray-100 text-gray-500'}`}>
      {conclusionLabel(cd)}
    </span>
  )
}

function AnalysisTypeBadge({ type }: { type: AnalysisTypeCd }) {
  const styles: Record<AnalysisTypeCd, string> = {
    BIAS_DETECTION:           'bg-purple-100 text-purple-700',
    COMPLIANCE_VERIFICATION:  'bg-blue-100 text-blue-700',
  }
  return (
    <span className={`text-xs px-2 py-0.5 rounded font-medium ${styles[type] ?? 'bg-gray-100 text-gray-500'}`}>
      {analysisTypeLabel(type)}
    </span>
  )
}

function ConfidenceBar({ score }: { score: number }) {
  const pct = Math.round(score * 100)
  const color = pct >= 85 ? 'text-green-600' : pct >= 70 ? 'text-yellow-600' : 'text-gray-400'
  return <span className={`text-xs font-mono font-medium ${color}`}>{pct}%</span>
}
