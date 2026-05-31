'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { formatNumber } from '@/lib/mock-data'
import TransferSidebar from '@/components/inquiry/TransferSidebar'

type PendingTransfer = {
  fromNumber: string
  fromName: string
  toBank: string
  toAccount: string
  amount: number
  receiverName: string
  fee: number
}

const PIN_PAD = [
  [5, 2, 7],
  [9, 8, 0],
  [6, 1, 4],
  ['↺', 3, '✕'],
]

export default function TransferConfirmPage() {
  const router = useRouter()
  const [data, setData] = useState<PendingTransfer | null>(null)
  const [showCertModal, setShowCertModal] = useState(false)
  const [certStep, setCertStep] = useState<'info' | 'pin'>('info')
  const [pin, setPin] = useState<number[]>([])
  const [cardInput1, setCardInput1] = useState('')
  const [cardInput2, setCardInput2] = useState('')
  const [passwordError, setPasswordError] = useState(false)
  const [errorCount, setErrorCount] = useState(0)

  useEffect(() => {
    const raw = sessionStorage.getItem('pendingTransfer')
    if (!raw) { router.push('/transfer/account'); return }
    setData(JSON.parse(raw))
  }, [router])

  function handlePinKey(key: number | string) {
    if (key === '↺') { setPin([]); return }
    if (key === '✕') { setPin(p => p.slice(0, -1)); return }
    if (typeof key === 'number' && pin.length < 6) {
      const next = [...pin, key]
      setPin(next)
      if (next.length === 6) {
        setTimeout(() => {
          setShowCertModal(false)
          router.push('/transfer/result')
        }, 400)
      }
    }
  }

  if (!data) return null

  return (
    <div className="max-w-kb-container mx-auto px-6">
      <div className="flex">
        <TransferSidebar />

        {/* ===== 본문 ===== */}
        <main className="flex-1 pl-8 pt-4 pb-12">
          {/* 브레드크럼 */}
          <div className="flex justify-end mb-2 text-[12px] text-kb-text-muted gap-1">
            <span>개인뱅킹</span><span>&gt;</span><span>이체</span><span>&gt;</span>
            <span>계좌이체</span><span>&gt;</span>
            <span className="font-semibold text-kb-text">계좌이체</span>
            <span className="ml-2 text-kb-blue cursor-pointer">? 도움말</span>
          </div>

          <h1 className="text-[20px] font-bold text-kb-text mb-5">계좌이체</h1>

          {/* STEP 표시 */}
          <div className="mb-4 space-y-2">
            <p className="text-[13px] text-kb-text-muted">STEP 1. 이체정보 입력</p>
            <p className="text-[14px] font-bold text-kb-text border-b-2 border-kb-text pb-1 w-fit">STEP 2. 이체정보 확인</p>
          </div>

          {/* 비밀번호 오류 박스 */}
          {passwordError && (
            <div className="border-2 border-kb-yellow rounded mb-5">
              <div className="flex items-start gap-4 px-5 py-4">
                <div className="flex-shrink-0 w-14 h-14 flex items-center justify-center">
                  <svg viewBox="0 0 56 56" fill="none" className="w-14 h-14">
                    <rect x="6" y="10" width="36" height="28" rx="2" fill="#D0D0D0" stroke="#AAAAAA" strokeWidth="1.5"/>
                    <rect x="14" y="38" width="20" height="4" rx="1" fill="#AAAAAA"/>
                    <rect x="10" y="42" width="28" height="2" rx="1" fill="#AAAAAA"/>
                    <circle cx="24" cy="24" r="9" fill="white" stroke="#AAAAAA" strokeWidth="1.5"/>
                    <line x1="17.5" y1="17.5" x2="30.5" y2="30.5" stroke="#CC3333" strokeWidth="2.5" strokeLinecap="round"/>
                    <line x1="30.5" y1="17.5" x2="17.5" y2="30.5" stroke="#CC3333" strokeWidth="2.5" strokeLinecap="round"/>
                  </svg>
                </div>
                <div className="flex-1">
                  <p className="text-[14px] font-bold mb-1" style={{ color: '#CC3333' }}>고객님 죄송합니다.</p>
                  <p className="text-[13px] text-kb-text-body leading-relaxed">
                    계좌 비밀번호 {errorCount}회 오류입니다. 4회 이상 오류등록 되는 경우 거래가 제한되므로 유의하시기 바랍니다.
                  </p>
                  <Link href="#" className="text-[12px] text-kb-blue underline mt-1 inline-block">자세히 보기</Link>
                  <p className="text-[12px] text-kb-text-muted mt-1">· 응답코드 : (UKFA1889)</p>
                </div>
              </div>
              <div className="border-t border-kb-border px-5 py-2 text-right text-[12px] text-kb-text-muted">
                대표전화&nbsp;&nbsp;1588-9999 | 1599-9999 | 1644-9999
              </div>
            </div>
          )}

          {/* 확인 헤더 박스 */}
          <div className="border border-kb-border-dark rounded-xl bg-gray-50 p-6 mb-5 text-center">
            <p className="text-[15px] font-bold text-kb-text">
              ↻ {data.receiverName}님께 {formatNumber(data.amount)}원 이체하시겠습니까?
            </p>
          </div>

          {/* 안내 메시지 */}
          <div className="mb-4 text-[12px] space-y-1">
            <p className="text-kb-text-muted">· 고객님께서 입력하신 입금은행, 입금계좌번호, 이체금액 및 받는분을 다시 한번 확인하세요.</p>
            <p className="text-kb-text-muted">· 메시지 또는 문자로 송금을 요구받은 경우에는 반드시 사실관계 확인 후 이체하시기 바랍니다.</p>
            <p className="text-kb-red">· <Link href="#" className="underline">[확인] 버튼을 누른 후 5분 이내에 결과화면이 나오지 않거나 오류메세지 발생시, 반드시 이체실행 여부를 [이체결과 조회]에서 확인하시기 바랍니다.</Link></p>
            <p className="text-kb-text-muted">· 보안카드 비밀번호가 동일하게 생성될 경우 &quot;전화인증&quot; 후 거래하시기 바랍니다.</p>
            <p className="text-kb-text-muted">· 출금계좌가 이체수수료 면제횟수가 있는 상품일 경우, 잔여 면제횟수 내에서 면제됩니다.</p>
            <p className="text-kb-text-muted">· 이전 화면의 [수수료조회] 버튼을 누르면 수수료 면제 잔여횟수를 확인하실 수 있습니다.</p>
            <p className="text-kb-text-muted">· 2건 이상 이체할 경우 [이체추가] 버튼을 누르면 더욱 빠르게 이용하실 수 있습니다. (10건까지 가능)</p>
            <p className="text-kb-red">· <Link href="#" className="underline">이체 도중 오류 발생 시, 반드시 출금계좌 거래내역을 확인하시기 바랍니다.</Link></p>
          </div>

          {/* 이체정보 테이블 */}
          <div className="mb-5">
            <div className="flex justify-between items-center mb-2">
              <p className="text-[14px] font-bold text-kb-text">이체정보</p>
              <div className="flex gap-2">
                <button className="border border-kb-border px-4 py-1 text-[12px] text-kb-text-body hover:bg-kb-beige-light">수정</button>
                <button className="border border-kb-border px-4 py-1 text-[12px] text-kb-text-body hover:bg-kb-beige-light">이체추가</button>
              </div>
            </div>
            <table className="w-full border-collapse text-[13px]">
              <thead>
                <tr className="bg-kb-beige-light">
                  <th className="border border-kb-border px-3 py-2 text-center font-semibold">No</th>
                  <th className="border border-kb-border px-3 py-2 text-center font-semibold">출금계좌번호</th>
                  <th className="border border-kb-border px-3 py-2 text-center font-semibold">입금계좌번호</th>
                  <th className="border border-kb-border px-3 py-2 text-center font-semibold">
                    이체금액(원)<br />
                    <span className="font-normal text-kb-text-muted">수수료(원)</span>
                  </th>
                  <th className="border border-kb-border px-3 py-2 text-center font-semibold">
                    받는분 예금주명<br />
                    <span className="font-normal text-kb-text-muted">(실제 예금주명)</span>
                  </th>
                  <th className="border border-kb-border px-3 py-2 text-center font-semibold">결과</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td className="border border-kb-border px-3 py-3 text-center">1</td>
                  <td className="border border-kb-border px-3 py-3 text-center">{data.fromNumber}</td>
                  <td className="border border-kb-border px-3 py-3 text-center">
                    <p>{data.toBank}</p>
                    <p>{data.toAccount}</p>
                    <button className="text-[11px] border border-kb-border px-1 mt-1">📋</button>
                  </td>
                  <td className="border border-kb-border px-3 py-3 text-right">
                    <p className="text-kb-red font-semibold">{formatNumber(data.amount)}</p>
                    <p className="text-kb-text-muted">{data.fee === 0 ? '면제' : formatNumber(data.fee)}</p>
                  </td>
                  <td className="border border-kb-border px-3 py-3 text-center">{data.receiverName}</td>
                  <td className="border border-kb-border px-3 py-3 text-center text-green-600 font-semibold">정상</td>
                </tr>
              </tbody>
            </table>
          </div>

          {/* 보안매체 비밀번호 입력 */}
          <div className="border border-kb-border-dark rounded-xl p-6 mb-5">
            <p className="text-[14px] font-bold text-kb-text mb-4">보안매체 비밀번호 입력</p>
            <label className="flex items-center gap-2 text-[12px] text-kb-text-muted mb-4 cursor-pointer">
              <input type="checkbox" /> 마우스로 입력
            </label>
            <div className="flex items-start gap-8">
              <div className="space-y-3">
                <div className="flex items-center gap-3 text-[13px]">
                  <span className="text-gray-400">●●</span>
                  <input type="text" maxLength={2} value={cardInput1} onChange={e => { setCardInput1(e.target.value); setPasswordError(false) }}
                    className="border border-kb-border w-16 px-2 py-1 text-center text-[13px]" />
                  <span className="text-kb-text-muted">[33] 앞의 두자리</span>
                </div>
                <div className="flex items-center gap-3 text-[13px]">
                  <span className="text-gray-400">●●</span>
                  <input type="text" maxLength={2} value={cardInput2} onChange={e => { setCardInput2(e.target.value); setPasswordError(false) }}
                    className="border border-kb-border w-16 px-2 py-1 text-center text-[13px]" />
                  <span className="text-kb-text-muted">[10] 뒤의 두자리</span>
                </div>
              </div>

              {/* 보안카드 */}
              <div className="border border-gray-300 p-3 text-[11px]" style={{ minWidth: 260 }}>
                <div className="flex justify-between items-center mb-2 pb-1 border-b border-gray-200">
                  <span className="font-bold text-kb-text">✱ AX풀뱅크</span>
                  <span className="text-kb-text-muted">Number. 0123456789</span>
                </div>
                <div className="grid grid-cols-5 gap-1 text-center text-[10px] text-gray-500">
                  {Array.from({length:35},(_,i)=>(
                    <div key={i} className="py-0.5">
                      <span className="mr-0.5 text-gray-400">{i+1}</span>
                      <span>•••••</span>
                    </div>
                  ))}
                </div>
                <p className="mt-2 text-[9px] text-gray-400">※ 이 카드는 절대로 타인에게 보여 주거나 전화로 알려주지 마십시오. 분실하신 경우 즉시 신고하십시오. 전화 1588-0000</p>
              </div>
            </div>

            <div className="mt-4 border border-red-200 bg-red-50 p-3 text-[12px]">
              <p className="font-semibold text-kb-red">⚠ 3년 이상된 보안카드입니다.</p>
              <p className="text-kb-text-muted mt-1">· 지금 사용하시는 보안카드는 발급된지 3년 이상된 보안카드로 안전한 거래를 위해 영업점에서 교체해 주시기 바랍니다.</p>
            </div>
          </div>

          {/* 확인/취소 버튼 */}
          <div className="flex justify-center gap-3 mb-8">
            <button
              onClick={() => {
                if (!cardInput1 || !cardInput2) {
                  setPasswordError(true)
                  setErrorCount(c => c + 1)
                  return
                }
                setShowCertModal(true); setCertStep('info'); setPin([])
              }}
              className="bg-kb-yellow px-16 py-3 text-[15px] font-bold text-kb-text hover:brightness-95"
            >
              확인
            </button>
            <button
              onClick={() => router.push('/transfer/account')}
              className="border border-kb-border px-16 py-3 text-[15px] text-kb-text-body hover:bg-kb-beige-light"
            >
              취소
            </button>
          </div>
        </main>
      </div>

      {/* ===== 금융인증서 모달 ===== */}
      {showCertModal && data && (
        <div className="fixed inset-0 bg-black/50 z-[300] flex items-center justify-center">
          <div className="bg-white shadow-xl flex" style={{ width: 680, minHeight: 420 }}>
            {/* 왼쪽 - YESKEY */}
            <div className="w-[220px] bg-gray-50 border-r border-gray-200 flex flex-col items-center justify-center p-6">
              <div className="text-center mb-4">
                <div className="inline-block border-2 border-gray-300 rounded px-3 py-1 mb-2">
                  <span className="text-[15px] font-black tracking-wider text-gray-600">YESKEY</span>
                  <span className="text-[9px] text-gray-400 block">금융인증원</span>
                </div>
                <div className="border border-gray-300 rounded-full w-10 h-10 flex items-center justify-center mx-auto">
                  <span className="text-[10px] text-gray-500">TRUST<br/>CA</span>
                </div>
              </div>
            </div>

            {/* 오른쪽 - 내용 */}
            <div className="flex-1 flex flex-col">
              {/* 헤더 */}
              <div className="flex items-center justify-between px-5 py-3 border-b border-gray-200 bg-gray-50">
                <span className="text-[13px] font-bold text-kb-text-muted">금융인증서비스</span>
                <button onClick={() => setShowCertModal(false)} className="text-gray-400 hover:text-gray-600">✕</button>
              </div>

              <div className="flex-1 p-6">
                {certStep === 'info' ? (
                  <div>
                    <p className="text-[13px] font-bold text-kb-text mb-4">전자서명 원문</p>
                    <div className="bg-gray-50 p-4 text-[13px] text-kb-text-body space-y-1 border border-gray-200 mb-6">
                      <p>이체금액 : {formatNumber(data.amount)}</p>
                      <p>입금계좌번호 : {data.toAccount}</p>
                      <p>출금계좌번호 : {data.fromNumber}</p>
                      <p>입금은행명 : {data.toBank}</p>
                      <p>의화인성명 :</p>
                      <p>수화인 : {data.receiverName}</p>
                      <p>출금계좌도 :</p>
                      <p>이체수수료 : 0</p>
                    </div>
                    <div className="flex justify-center">
                      <button
                        onClick={() => setCertStep('pin')}
                        className="bg-kb-yellow px-16 py-3 text-[14px] font-bold text-kb-text hover:brightness-95"
                      >
                        확인
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="flex flex-col items-center">
                    <p className="text-[13px] text-kb-blue mb-1">홍길동님의 금융인증서</p>
                    <p className="text-[16px] font-bold text-kb-text mb-5">비밀번호를 입력해주세요</p>
                    {/* PIN 표시 */}
                    <div className="flex gap-2 mb-6">
                      {Array.from({length:6}).map((_,i) => (
                        <div key={i}
                          className={`w-8 h-8 rounded-full border-2 flex items-center justify-center ${
                            i < pin.length ? 'bg-kb-text border-kb-text' : 'border-gray-300'
                          }`}>
                          {i < pin.length && <span className="w-2 h-2 bg-white rounded-full" />}
                        </div>
                      ))}
                    </div>
                    <Link href="#" className="text-[12px] text-kb-blue underline mb-6">비밀번호를 잊으셨나요?</Link>
                    {/* 숫자 패드 */}
                    <div className="grid grid-cols-3 gap-3 w-48">
                      {PIN_PAD.map((row, ri) =>
                        row.map((key, ci) => (
                          <button
                            key={`${ri}-${ci}`}
                            onClick={() => handlePinKey(key)}
                            className={`h-10 text-[16px] font-semibold rounded hover:bg-kb-beige transition-colors ${
                              typeof key === 'string' ? 'text-gray-400' : 'text-kb-text'
                            }`}
                          >
                            {key}
                          </button>
                        ))
                      )}
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
