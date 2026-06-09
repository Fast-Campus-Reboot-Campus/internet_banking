'use client'
import { KB_PRIMARY } from '@/lib/theme'

import { useEffect, useRef, useState } from 'react'
import {
  TELECOM_CARRIERS,
  sendMobileAuth,
  verifyMobileAuth,
  authErrorMessage,
  type MobileAuthPurpose,
} from '@/lib/customer-auth-api'

const GREEN = KB_PRIMARY
const OTP_SECONDS = 180

type Props = {
  purpose: MobileAuthPurpose
  /** 주민번호 기반 본인확인이 필요한 경우(가입) 성명·주민번호 13자리를 전달한다. */
  identity?: { name: string; rrn: string }
  /** 검증 성공 시 호출. 인증된 전화번호와 verificationId(주민번호 본인확인 시)를 전달한다. */
  onVerified: (phoneNumber: string, verificationId?: number | null) => void
}

/**
 * 휴대폰 본인인증 위젯 — 인증번호 발송 → 입력 → 검증.
 * MVP 백엔드는 실제 SMS 대신 서버 로그로 인증번호를 출력한다.
 */
export default function MobileAuthField({ purpose, identity, onVerified }: Props) {
  const [carrier, setCarrier] = useState<string>(TELECOM_CARRIERS[0].code)
  const [phone, setPhone] = useState('')
  const [code, setCode] = useState('')
  const [sent, setSent] = useState(false)
  const [verified, setVerified] = useState(false)
  const [secondsLeft, setSecondsLeft] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  useEffect(() => {
    return () => {
      if (timerRef.current) clearInterval(timerRef.current)
    }
  }, [])

  function startCountdown() {
    setSecondsLeft(OTP_SECONDS)
    if (timerRef.current) clearInterval(timerRef.current)
    timerRef.current = setInterval(() => {
      setSecondsLeft((s) => {
        if (s <= 1) {
          if (timerRef.current) clearInterval(timerRef.current)
          return 0
        }
        return s - 1
      })
    }, 1000)
  }

  const phoneDigits = phone.replace(/\D/g, '')

  async function handleSend() {
    if (!/^010\d{8}$/.test(phoneDigits)) {
      setError('휴대폰 번호를 정확히 입력해주세요. (010으로 시작하는 11자리)')
      return
    }
    setError('')
    setLoading(true)
    try {
      await sendMobileAuth({ phoneNumber: phoneDigits, telecomCarrierCode: carrier, purposeCode: purpose })
      setSent(true)
      setVerified(false)
      setCode('')
      startCountdown()
    } catch (err) {
      setError(authErrorMessage(err, '인증번호 발송에 실패했습니다.'))
    } finally {
      setLoading(false)
    }
  }

  async function handleVerify() {
    if (!/^\d{6}$/.test(code)) {
      setError('인증번호 6자리를 입력해주세요.')
      return
    }
    if (secondsLeft === 0) {
      setError('인증번호가 만료되었습니다. 다시 발송해주세요.')
      return
    }
    if (identity && (!identity.name.trim() || !/^\d{13}$/.test(identity.rrn))) {
      setError('성명과 주민등록번호를 먼저 정확히 입력해주세요.')
      return
    }
    setError('')
    setLoading(true)
    try {
      const verificationId = await verifyMobileAuth({
        phoneNumber: phoneDigits, purposeCode: purpose, code,
        name: identity?.name, rrn: identity?.rrn,
      })
      setVerified(true)
      if (timerRef.current) clearInterval(timerRef.current)
      onVerified(phoneDigits, verificationId)
    } catch (err) {
      setError(authErrorMessage(err, '인증번호가 올바르지 않습니다.'))
    } finally {
      setLoading(false)
    }
  }

  const mm = String(Math.floor(secondsLeft / 60)).padStart(2, '0')
  const ss = String(secondsLeft % 60).padStart(2, '0')

  return (
    <div className="space-y-3">
      {/* 통신사 + 번호 */}
      <div className="flex items-center gap-2">
        <select
          value={carrier}
          onChange={(e) => setCarrier(e.target.value)}
          disabled={verified}
          className="input w-32 flex-shrink-0"
        >
          {TELECOM_CARRIERS.map((c) => (
            <option key={c.code} value={c.code}>
              {c.label}
            </option>
          ))}
        </select>
        <input
          type="tel"
          inputMode="numeric"
          placeholder="휴대폰 번호 (- 없이)"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
          disabled={verified}
          className="input flex-1"
          maxLength={13}
        />
        <button
          type="button"
          onClick={handleSend}
          disabled={loading || verified}
          className="px-4 py-2 text-[13px] font-bold text-white rounded-lg whitespace-nowrap disabled:opacity-50"
          style={{ backgroundColor: GREEN }}
        >
          {sent ? '재발송' : '인증번호 발송'}
        </button>
      </div>

      {/* 인증번호 입력 */}
      {sent && !verified && (
        <div className="flex items-center gap-2">
          <div className="relative flex-1">
            <input
              type="text"
              inputMode="numeric"
              placeholder="인증번호 6자리"
              value={code}
              onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
              className="input w-full pr-14"
              maxLength={6}
            />
            <span className="absolute right-3 top-1/2 -translate-y-1/2 text-[13px] font-semibold text-red-500">
              {mm}:{ss}
            </span>
          </div>
          <button
            type="button"
            onClick={handleVerify}
            disabled={loading}
            className="px-4 py-2 text-[13px] font-bold text-white rounded-lg whitespace-nowrap disabled:opacity-50"
            style={{ backgroundColor: GREEN }}
          >
            확인
          </button>
        </div>
      )}

      {verified && (
        <p className="text-[13px] font-semibold" style={{ color: GREEN }}>
          ✓ 휴대폰 본인인증이 완료되었습니다.
        </p>
      )}
      {error && <p className="text-[13px] text-red-500">{error}</p>}
    </div>
  )
}
