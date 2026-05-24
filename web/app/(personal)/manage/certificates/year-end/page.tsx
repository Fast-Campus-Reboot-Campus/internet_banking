import Link from 'next/link'
import ManageSidebar from '@/components/inquiry/ManageSidebar'

const RELATED_LINKS = [
  '주택자금대출 상환증명서',
  '주택마련/개인연금/장기주식형저축/(청년)소득공제장기펀드',
  '직불카드 사용금액확인서',
  '연금카드 사용금액확인서',
  '전자서명 기부금수증',
  '공익신탁 기부금수증',
  '퇴직연금납입확인서',
]

const TABLE_ROWS = [
  ['2012년 이전 차입', '3억원 이하', '15년 이상', '비거치식 분할상환', '1,500만원'],
  ['2015년 이전 차입', '4억원 이하', '15년 이상', '비거치식 분할상환\n또는 고정금리', '1,800만원'],
  ['2019년 이전 차입', '5억원 이하', '15년 이상', '비거치식 분할상환\n또는 고정금리 또는 두 가지 모두', '1,800만원'],
  ['2024년 이전 차입', '6억원 이하', '10년 이상', '거치식', '600만원'],
  ['2024년 이전 차입', '6억원 이하', '15년 이상', '비거치식 분할상환\n또는 고정금리 또는 두 가지 모두', '2,000만원'],
]

export default function YearEndCertPage() {
  return (
    <div className="max-w-kb-container mx-auto px-6">
      <div className="flex">
        <ManageSidebar />

        {/* 본문 */}
        <main className="flex-1 pl-8 pt-4 pb-12">
          <div className="flex justify-end mb-2 text-[12px] text-kb-text-muted gap-1">
            <span>개인뱅킹</span><span>&gt;</span>
            <span>뱅킹관리</span><span>&gt;</span>
            <span>제증명발급</span><span>&gt;</span>
            <span>연말정산증명서</span><span>&gt;</span>
            <span className="font-semibold text-kb-text">발급대상자 안내</span>
          </div>

          <h1 className="text-[20px] font-bold text-kb-text mb-4">연말정산증명서</h1>

          {/* 관련 링크 */}
          <div className="mb-5 text-[13px] space-y-1.5">
            {RELATED_LINKS.map(item => (
              <div key={item}>
                <Link href="#" className="text-kb-blue hover:underline">▶ {item}</Link>
              </div>
            ))}
          </div>

          {/* 탭 */}
          <div className="border-b border-kb-border mb-6">
            <div className="flex">
              {['발급대상자 안내', '발급신청'].map((tab, i) => (
                <Link key={tab} href="#"
                  className={`px-6 py-3 text-base border-b-2 -mb-px ${
                    i === 0
                      ? 'border-kb-text font-bold text-kb-text'
                      : 'border-transparent text-kb-text-muted hover:text-kb-text'
                  }`}>
                  {tab}
                </Link>
              ))}
            </div>
          </div>

          {/* 소득공제 안내 */}
          <h2 className="text-lg font-bold text-kb-text mb-1">
            소득공제 - 장기주택저당차입금 이자상환증명서
          </h2>
          <p className="text-[12px] text-kb-text-muted mb-4">(주택구입자금대출/중도대출)</p>

          <table className="w-full border-collapse text-[13px] mb-5">
            <thead>
              <tr className="bg-kb-beige-light">
                <th className="border border-kb-border px-4 py-2 text-center font-semibold" rowSpan={2}>구분</th>
                <th className="border border-kb-border px-4 py-2 text-center font-semibold" colSpan={3}>공제조건</th>
                <th className="border border-kb-border px-4 py-2 text-center font-semibold" rowSpan={2}>공제한도</th>
              </tr>
              <tr className="bg-kb-beige-light">
                <th className="border border-kb-border px-4 py-2 text-center font-semibold">주택기준가격</th>
                <th className="border border-kb-border px-4 py-2 text-center font-semibold">대출기간</th>
                <th className="border border-kb-border px-4 py-2 text-center font-semibold">상환방법</th>
              </tr>
            </thead>
            <tbody>
              {TABLE_ROWS.map((row, i) => (
                <tr key={i} className={i % 2 === 0 ? '' : 'bg-kb-beige-light/40'}>
                  <td className="border border-kb-border px-4 py-2.5 text-center">{row[0]}</td>
                  <td className="border border-kb-border px-4 py-2.5 text-center">{row[1]}</td>
                  <td className="border border-kb-border px-4 py-2.5 text-center">{row[2]}</td>
                  <td className="border border-kb-border px-4 py-2.5 text-center whitespace-pre-line leading-snug">{row[3]}</td>
                  <td className="border border-kb-border px-4 py-2.5 text-center font-semibold">{row[4]}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="border border-kb-border rounded-xl p-6 bg-kb-beige-light text-[12px] text-kb-text-muted space-y-1.5">
            <p className="font-semibold text-kb-text mb-2">유의사항</p>
            <p>· 해당 주택에 실제 거주(주민등록 전입)하여야 합니다.</p>
            <p>· 세대주인 근로자가 주택 구입에 필요한 자금을 금융기관으로부터 차입한 경우에 한합니다.</p>
            <p>· 차입 당시 무주택 세대주이거나 1주택 보유 세대주이어야 합니다.</p>
            <p>· 공제 가능 여부 및 한도는 관련 세법에 따라 달라질 수 있으니 반드시 해당 세무서에 문의하시기 바랍니다.</p>
          </div>
        </main>
      </div>
    </div>
  )
}
