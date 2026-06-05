'use client'
import { KB_PRIMARY as GREEN, KB_PRIMARY_DARK } from '@/lib/theme'

import { useEffect, useState } from 'react'
import {
  ensureCurrentDevice,
  registerPin,
  revokePin,
  savePinDeviceId,
  loadPinDeviceId,
  authErrorMessage,
} from '@/lib/customer-auth-api'


/**
 * 간편비밀번호(PIN) 등록/해제 모달.
 * 설정 > 인증수단 탭에서 열린다(로그인 상태). 다른 인증 모달과 동일한 패턴.
 */
export default function PinManageModal({ onClose, onChanged }: { onClose: () => void; onChanged?: () => void }) {
  const [registeredDeviceId, setRegisteredDeviceId] = useState<number | null>(null)
  const [pin, setPin] = useState('')
  const [pinConfirm, setPinConfirm] = useState('')
  const [currentPassword, setCurrentPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [msg, setMsg] = useState('')

  useEffect(() => {
    setRegisteredDeviceId(loadPinDeviceId())
  }, [])

  async function handleRegister() {
    if (!/^\d{6}$/.test(pin)) return setError('PIN은 6자리 숫자로 입력해주세요.')
    if (pin !== pinConfirm) return setError('PIN이 일치하지 않습니다.')
    if (!currentPassword) return setError('본인 확인을 위해 현재 비밀번호를 입력해주세요.')

    setError('')
    setMsg('')
    setLoading(true)
    try {
      const device = await ensureCurrentDevice()
      await registerPin({ deviceId: device.deviceId, pin, currentPassword })
      savePinDeviceId(device.deviceId)
      setRegisteredDeviceId(device.deviceId)
      setPin('')
      setPinConfirm('')
      setCurrentPassword('')
      setMsg('이 기기에 간편비밀번호(PIN)가 등록되었습니다.')
      onChanged?.()
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
      onChanged?.()
    } catch (err) {
      setError(authErrorMessage(err, 'PIN 해제에 실패했습니다.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 z-[300] flex items-center justify-center bg-black/40">
      <div className="bg-white w-[420px] shadow-lg">
        {/* 헤더 */}
        <div className="flex items-center justify-between px-6 py-4" style={{ backgroundColor: KB_PRIMARY_DARK }}>
          <p className="text-body font-bold text-white">간편비밀번호(PIN) 설정</p>
          <button onClick={onClose} className="text-white/80 hover:text-white text-xl leading-none">✕</button>
        </div>

        <div className="px-6 py-5">
          <p className="text-[12px] text-kb-text-muted mb-4">
            이 기기에 6자리 PIN을 등록하면 아이디·비밀번호 없이 간편하게 로그인할 수 있습니다.
          </p>

          {registeredDeviceId != null ? (
            <div className="border border-kb-border bg-kb-primary-bg px-5 py-4">
              <p className="text-[14px] font-semibold mb-1" style={{ color: GREEN }}>✓ 이 기기에 PIN이 등록되어 있습니다.</p>
              <p className="text-[12px] text-kb-text-body mb-3">로그인 화면에서 간편비밀번호 로그인을 이용할 수 있습니다.</p>
              <button
                onClick={handleRevoke}
                disabled={loading}
                className="border border-kb-border px-5 py-2 text-[13px] font-semibold text-kb-text-body hover:bg-white disabled:opacity-50"
              >
                PIN 해제
              </button>
            </div>
          ) : (
            <div className="space-y-2.5">
              <Field label="PIN 6자리">
                <input type="password" inputMode="numeric" value={pin} onChange={(e) => setPin(e.target.value.replace(/\D/g, '').slice(0, 6))} placeholder="숫자 6자리" className="input w-full" maxLength={6} />
              </Field>
              <Field label="PIN 확인">
                <input type="password" inputMode="numeric" value={pinConfirm} onChange={(e) => setPinConfirm(e.target.value.replace(/\D/g, '').slice(0, 6))} placeholder="PIN 재입력" className="input w-full" maxLength={6} />
              </Field>
              <Field label="현재 비밀번호">
                <input type="password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} placeholder="본인 확인용 로그인 비밀번호" className="input w-full" autoComplete="current-password" />
              </Field>
              <div className="pt-2">
                <button onClick={handleRegister} disabled={loading} className="w-full py-2.5 text-[14px] font-bold text-white rounded-lg hover:opacity-85 disabled:opacity-50" style={{ backgroundColor: GREEN }}>
                  {loading ? '등록 중...' : 'PIN 등록'}
                </button>
              </div>
            </div>
          )}

          {msg && <p className="text-[13px] mt-3 font-semibold" style={{ color: GREEN }}>{msg}</p>}
          {error && <p className="text-[13px] mt-3 text-red-500">{error}</p>}
        </div>
      </div>
    </div>
  )
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center gap-3">
      <label className="w-24 text-[13px] text-kb-text-body text-right flex-shrink-0">{label}</label>
      <div className="flex-1">{children}</div>
    </div>
  )
}
