'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import {
  ensureCurrentDevice,
  listAuthMethods,
  registerPin,
  revokePin,
  savePinDeviceId,
  loadPinDeviceId,
  authErrorMessage,
  type AuthMethod,
} from '@/lib/customer-auth-api'

const GREEN = '#0D5C47'

export default function PinSettingsPage() {
  const [authMethods, setAuthMethods] = useState<AuthMethod[]>([])
  const [loadingMethods, setLoadingMethods] = useState(true)
  const [registeredDeviceId, setRegisteredDeviceId] = useState<number | null>(null)

  const [pin, setPin] = useState('')
  const [pinConfirm, setPinConfirm] = useState('')
  const [currentPassword, setCurrentPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [msg, setMsg] = useState('')

  useEffect(() => {
    setRegisteredDeviceId(loadPinDeviceId())
    listAuthMethods()
      .then((list) => setAuthMethods(list.filter((m) => m.authMethodStatusCode !== 'INACTIVE')))
      .catch(() => setAuthMethods([]))
      .finally(() => setLoadingMethods(false))
  }, [])

  const primaryAuthMethod = authMethods.find((m) => m.primary) ?? authMethods[0]

  async function handleRegister() {
    if (!/^\d{6}$/.test(pin)) return setError('PIN은 6자리 숫자로 입력해주세요.')
    if (pin !== pinConfirm) return setError('PIN이 일치하지 않습니다.')
    if (!currentPassword) return setError('본인 확인을 위해 현재 비밀번호를 입력해주세요.')
    if (!primaryAuthMethod) return setError('등록된 인증수단이 없습니다. 먼저 인증서를 발급해주세요.')

    setError('')
    setMsg('')
    setLoading(true)
    try {
      const device = await ensureCurrentDevice()
      await registerPin({
        deviceId: device.deviceId,
        authMethodId: primaryAuthMethod.authMethodId,
        pin,
        currentPassword,
      })
      savePinDeviceId(device.deviceId)
      setRegisteredDeviceId(device.deviceId)
      setPin('')
      setPinConfirm('')
      setCurrentPassword('')
      setMsg('이 기기에 간편비밀번호(PIN)가 등록되었습니다. 다음부터 PIN으로 로그인할 수 있습니다.')
    } catch (err) {
      setError(authErrorMessage(err, 'PIN 등록에 실패했습니다.'))
    } finally {
      setLoading(false)
    }
  }

  async function handleRevoke() {
    if (registeredDeviceId == null) return
    if (!confirm('이 기기의 간편비밀번호를 해제하시겠습니까?')) return
    setLoading(true)
    setError('')
    setMsg('')
    try {
      await revokePin(registeredDeviceId)
      localStorage.removeItem('pinDeviceId')
      setRegisteredDeviceId(null)
      setMsg('간편비밀번호가 해제되었습니다.')
    } catch (err) {
      setError(authErrorMessage(err, 'PIN 해제에 실패했습니다.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-white">
      <div className="max-w-kb-container mx-auto px-6 py-8">
        <div className="flex items-center gap-1 text-[12px] text-kb-text-muted mb-6">
          <Link href="/" className="hover:underline">홈</Link>
          <span>›</span>
          <Link href="/settings" className="hover:underline">설정</Link>
          <span>›</span>
          <span className="font-semibold text-kb-text">간편비밀번호(PIN)</span>
        </div>

        <main className="max-w-xl">
          <h1 className="text-[22px] font-bold text-kb-text mb-2">간편비밀번호(PIN) 설정</h1>
          <p className="text-[13px] text-kb-text-muted mb-6">
            현재 사용 중인 기기에 6자리 PIN을 등록하면, 아이디·비밀번호 대신 PIN으로 간편하게 로그인할 수 있습니다.
          </p>

          {/* 등록 상태 */}
          {registeredDeviceId != null ? (
            <div className="border border-kb-border bg-[#F0FAF7] px-5 py-5">
              <p className="text-[14px] font-semibold mb-1" style={{ color: GREEN }}>✓ 이 기기에 PIN이 등록되어 있습니다.</p>
              <p className="text-[13px] text-kb-text-body mb-4">로그인 화면에서 간편비밀번호 로그인을 이용할 수 있습니다.</p>
              <button
                onClick={handleRevoke}
                disabled={loading}
                className="border border-kb-border px-6 py-2.5 text-[13px] font-semibold text-kb-text-body hover:bg-white disabled:opacity-50"
              >
                PIN 해제
              </button>
            </div>
          ) : (
            <div className="border border-kb-border px-5 py-6 space-y-3">
              {loadingMethods ? (
                <p className="text-[13px] text-kb-text-muted">인증수단을 확인하는 중...</p>
              ) : !primaryAuthMethod ? (
                <div className="text-[13px] text-kb-text-body">
                  <p className="mb-2">PIN을 등록하려면 먼저 인증서를 발급해 인증수단을 등록해야 합니다.</p>
                  <Link href="/cert" className="font-semibold underline" style={{ color: GREEN }}>인증센터로 이동 ›</Link>
                </div>
              ) : (
                <>
                  <FormRow label="PIN 6자리">
                    <input type="password" inputMode="numeric" value={pin} onChange={(e) => setPin(e.target.value.replace(/\D/g, '').slice(0, 6))} placeholder="숫자 6자리" className="input w-full" maxLength={6} />
                  </FormRow>
                  <FormRow label="PIN 확인">
                    <input type="password" inputMode="numeric" value={pinConfirm} onChange={(e) => setPinConfirm(e.target.value.replace(/\D/g, '').slice(0, 6))} placeholder="PIN 재입력" className="input w-full" maxLength={6} />
                  </FormRow>
                  <FormRow label="현재 비밀번호">
                    <input type="password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} placeholder="본인 확인용 로그인 비밀번호" className="input w-full" autoComplete="current-password" />
                  </FormRow>
                  <div className="pt-2">
                    <button onClick={handleRegister} disabled={loading} className="w-full py-3 text-[14px] font-bold text-white rounded-lg hover:opacity-85 disabled:opacity-50" style={{ backgroundColor: GREEN }}>
                      {loading ? '등록 중...' : 'PIN 등록'}
                    </button>
                  </div>
                </>
              )}
            </div>
          )}

          {msg && <p className="text-[13px] mt-4 font-semibold" style={{ color: GREEN }}>{msg}</p>}
          {error && <p className="text-[13px] text-red-500 mt-4">{error}</p>}
        </main>
      </div>
    </div>
  )
}

function FormRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center gap-3">
      <label className="w-28 text-[13px] text-kb-text-body text-right flex-shrink-0">{label}</label>
      <div className="flex-1">{children}</div>
    </div>
  )
}
