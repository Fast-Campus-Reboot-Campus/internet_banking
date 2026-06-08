import { api } from '@/lib/api'

/**
 * 고객계·인증보안계 인증 API 클라이언트.
 * 게이트웨이(@/lib/api)를 통해 customer-service 엔드포인트를 호출한다.
 * - 법인 신규가입 (/auth/register/corporate)
 * - 휴대폰 본인인증 (/mobile-auth/send · /verify)
 * - 등록기기 (/customers/me/devices)
 * - 인증수단 (/customers/me/auth-methods)
 * - 간편비밀번호 PIN (/customers/me/pin · /auth/pin-login)
 */

/* ── 공통 코드 ── */
export const TELECOM_CARRIERS = [
  { code: 'SKT', label: 'SKT' },
  { code: 'KT', label: 'KT' },
  { code: 'LGU', label: 'LG U+' },
  { code: 'SKT_MVNO', label: 'SKT 알뜰폰' },
  { code: 'KT_MVNO', label: 'KT 알뜰폰' },
  { code: 'LGU_MVNO', label: 'LG U+ 알뜰폰' },
] as const

export const TAX_TYPES = [
  { code: 'GENERAL', label: '일반과세자' },
  { code: 'SIMPLE', label: '간이과세자' },
  { code: 'EXEMPT', label: '면세사업자' },
] as const

/** 휴대폰 인증 목적 코드 (백엔드 MobileAuth.PURPOSE_*) */
export type MobileAuthPurpose = 'SIGNUP' | 'PASSWORD_RESET' | 'IDENTITY_VERIFY'

/* ── 법인 신규가입 ── */
export type CorporateRegisterRequest = {
  corpName: string
  corpEnglishName: string
  corpRegNo: string // 6-7 형식 (\d{6}-\d{7})
  bizRegNo: string // 3-2-5 형식 (\d{3}-\d{2}-\d{5})
  tradeName: string
  openingDate: string // YYYYMMDD
  ntsIndustryCode: string
  ksicCode: string
  bizItemCode: string
  taxTypeCode: string
  loginId: string
  password: string
  email: string
  phone: string
}

export type RegisterResponse = { customerId: number; loginId: string }

export async function registerCorporate(req: CorporateRegisterRequest): Promise<RegisterResponse> {
  const { data } = await api.post('/api/v1/auth/register/corporate', req)
  return data.data
}

/* ── 휴대폰 본인인증 ── */
export async function sendMobileAuth(params: {
  phoneNumber: string
  telecomCarrierCode: string
  purposeCode: MobileAuthPurpose
  methodTypeCode?: 'SMS' | 'PASS'
}): Promise<number> {
  const { data } = await api.post('/api/v1/mobile-auth/send', {
    methodTypeCode: 'SMS',
    ...params,
  })
  return data.data // mobileAuthId
}

/**
 * 휴대폰 인증 검증. name·rrn(주민번호 13자리)을 함께 보내면 주민번호 기반 본인확인까지 수행되고
 * 가입에 쓸 verificationId 를 돌려준다. (미제공 시 단순 전화인증 → null)
 */
export async function verifyMobileAuth(params: {
  phoneNumber: string
  purposeCode: MobileAuthPurpose
  code: string
  name?: string
  rrn?: string
}): Promise<number | null> {
  const { data } = await api.post('/api/v1/mobile-auth/verify', params)
  return data.data?.verificationId ?? null
}

/* ── 등록기기 ── */
export type RegisteredDevice = {
  deviceId: number
  deviceName: string
  deviceTypeCode: string
  trusted: boolean
  designatedPc: boolean
  deviceStatusCode: string
}

export async function listDevices(): Promise<RegisteredDevice[]> {
  const { data } = await api.get('/api/v1/customers/me/devices')
  return data.data
}

export async function registerDevice(): Promise<RegisteredDevice> {
  const ua = typeof navigator !== 'undefined' ? navigator.userAgent : 'unknown'
  const { data } = await api.post('/api/v1/customers/me/devices', {
    deviceTypeCode: 'PC',
    deviceName: '웹 브라우저',
    deviceOsName: typeof navigator !== 'undefined' ? navigator.platform : 'web',
    deviceOsVersion: '',
    deviceIdentifier: ua,
  })
  return data.data
}

/**
 * 현재 브라우저 기기를 확보한다.
 * 이미 등록된 동일 기기가 있으면 재사용하고, 없으면 신규 등록한다.
 */
export async function ensureCurrentDevice(): Promise<RegisteredDevice> {
  const ua = typeof navigator !== 'undefined' ? navigator.userAgent : 'unknown'
  try {
    const existing = await listDevices()
    const match = existing.find((d) => d.deviceName === '웹 브라우저' && d.deviceStatusCode !== 'REVOKED')
    if (match) return match
  } catch {
    // 목록 조회 실패 시 신규 등록으로 진행
  }
  void ua
  return registerDevice()
}

/* ── 인증수단 ── */
export type AuthMethod = {
  authMethodId: number
  authMethodTypeCode: string
  authMethodAliasName: string
  authMethodStatusCode: string
  primary: boolean
}

export async function listAuthMethods(): Promise<AuthMethod[]> {
  const { data } = await api.get('/api/v1/customers/me/auth-methods')
  return data.data
}

/* ── 간편비밀번호(PIN) ── */
const PIN_DEVICE_KEY = 'pinDeviceId'

export function savePinDeviceId(deviceId: number) {
  if (typeof window !== 'undefined') localStorage.setItem(PIN_DEVICE_KEY, String(deviceId))
}

export function loadPinDeviceId(): number | null {
  if (typeof window === 'undefined') return null
  const raw = localStorage.getItem(PIN_DEVICE_KEY)
  return raw ? Number(raw) : null
}

export async function registerPin(params: {
  deviceId: number
  pin: string
  currentPassword: string
}): Promise<void> {
  // 인증수단(PIN AuthMethod)은 백엔드가 직접 생성·연결한다.
  await api.post('/api/v1/customers/me/pin', params)
}

export async function revokePin(deviceId: number): Promise<void> {
  await api.delete(`/api/v1/customers/me/pin?deviceId=${deviceId}`)
}

export type LoginResponse = { customerId: number; accessToken: string; refreshToken?: string }

export async function pinLogin(params: { loginId: string; deviceId: number; pin: string }): Promise<LoginResponse> {
  const { data } = await api.post('/api/v1/auth/pin-login', params)
  return data.data
}

/** 로그인 응답을 localStorage에 반영하고 사용자 정보를 캐싱한다 (로그인 페이지와 동일 규약). */
export async function persistLogin(res: LoginResponse): Promise<void> {
  localStorage.setItem('accessToken', res.accessToken)
  localStorage.setItem('access_token', res.accessToken)
  localStorage.setItem('customerId', String(res.customerId))
  if (res.refreshToken) localStorage.setItem('refreshToken', res.refreshToken)
  try {
    const me = await api.get('/api/v1/customers/me')
    localStorage.setItem(
      'user',
      JSON.stringify({ name: me.data.data.name, email: me.data.data.email ?? '', customer_id: me.data.data.customerId }),
    )
  } catch {
    // 보조 호출 실패는 무시
  }
}

/** axios 에러에서 백엔드 메시지를 추출한다. */
export function authErrorMessage(err: unknown, fallback: string): string {
  const axiosErr = err as { response?: { data?: { message?: string } } }
  return axiosErr.response?.data?.message ?? fallback
}
