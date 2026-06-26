import { NextRequest, NextResponse } from 'next/server'

const MOCK_CUSTOMERS = [
  { loginId: 'user01', password: 'Test1234', customerId: 1, customerNo: 'CUST001', name: '김고객' },
  { loginId: 'user02', password: 'Test1234', customerId: 2, customerNo: 'CUST002', name: '이고객' },
  { loginId: 'user03', password: 'Test1234', customerId: 3, customerNo: 'CUST003', name: '박고객' },
]

const MOCK_EMPLOYEES = [
  { loginId: 'super01', password: '1234', name: 'super01', roles: ['ROLE_ADMIN'] },
  { loginId: 'agent01', password: '1234', name: 'agent01', roles: ['ROLE_TELLER'] },
  { loginId: 'agent02', password: '1234', name: 'agent02', roles: ['ROLE_TELLER'] },
]

export async function POST(req: NextRequest) {
  const { loginId, password } = await req.json()

  const emp = MOCK_EMPLOYEES.find((u) => u.loginId === loginId && u.password === password)
  if (emp) {
    const payload = { name: emp.name, roles: emp.roles }
    const accessToken = `mock.${Buffer.from(JSON.stringify(payload)).toString('base64')}.${Date.now()}`
    return NextResponse.json({ status: 'SUCCESS', data: { accessToken, name: emp.name } })
  }

  const user = MOCK_CUSTOMERS.find((u) => u.loginId === loginId && u.password === password)
  if (!user) {
    return NextResponse.json(
      { status: 'FAIL', message: '아이디 또는 비밀번호가 올바르지 않습니다.' },
      { status: 401 },
    )
  }

  const payload = { customerId: user.customerId, customerNo: user.customerNo, name: user.name }
  const accessToken = `mock.${Buffer.from(JSON.stringify(payload)).toString('base64')}.${Date.now()}`

  return NextResponse.json({
    status: 'SUCCESS',
    data: {
      accessToken,
      customerId: user.customerId,
      customerNo: user.customerNo,
      name: user.name,
    },
  })
}
