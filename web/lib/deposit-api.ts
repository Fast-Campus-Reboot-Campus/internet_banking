import axios from 'axios'
import type { Account } from '@/lib/mock-data'

function depositBaseUrl() {
  const raw = process.env.NEXT_PUBLIC_DEPOSIT_API_URL || 'http://localhost:8082/api'
  return raw.replace(/\/$/, '').endsWith('/api') ? raw.replace(/\/$/, '') : `${raw.replace(/\/$/, '')}/api`
}

const depositApi = axios.create({
  baseURL: depositBaseUrl(),
  headers: {
    'Content-Type': 'application/json',
  },
})

depositApi.interceptors.request.use((config) => {
  if (typeof window === 'undefined') return config

  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export type DepositProductType = 'DEPOSIT' | 'SAVINGS' | 'SUBSCRIPTION'
export type SavingType = 'REGULAR' | 'FREE'

export type DepositProduct = {
  productId: number
  productType: DepositProductType
  productName: string
  description?: string
  baseInterestRate?: number | string
  minJoinAmount?: number | string
  maxJoinAmount?: number | string
  minPeriodMonth?: number
  maxPeriodMonth?: number
  productStatus?: string
}

export type DepositContract = {
  contractId: number
  contractNumber: string
  customerId: string
  productId: number
  joinAmount: number | string
  contractPeriodMonth: number
  maturityAt?: string
  startedAt?: string
  autoTransferDay?: number
  contractStatus?: string
  terminatedAt?: string
  sourceAccountId?: number
}

export type DepositAccount = {
  accountId: number
  accountNumber: string
  customerId: string
  contractId: number
  accountType: DepositProductType
  savingType?: SavingType
  accountAlias?: string
  balance: number | string
  totalPaidAmount?: number | string
  openedAt?: string
  maturityAt?: string
  accountStatus?: string
}

export type DepositViewAccount = Account & {
  apiAccountId?: number
  contractId?: number
  accountStatus?: string
  savingType?: string
}

export type DepositRecommendProduct = {
  product_id?: number
  productId?: number
  product_name?: string
  productName?: string
  product_type?: string
  productType?: string
  base_interest_rate?: number
  baseInterestRate?: number
  bestRate?: number
  minJoinAmount?: number
  maxJoinAmount?: number
  minPeriodMonth?: number
  maxPeriodMonth?: number
  reason?: string
}

export type DepositRecommendResponse = {
  customerId: string
  periodMonth?: number
  analysisPeriodMonth?: number
  cashFlow?: {
    totalInflow?: number
    totalOutflow?: number
    netCashFlow?: number
    estimatedSavingsAmount?: number
  }
  products?: DepositRecommendProduct[]
  recommendations?: DepositRecommendProduct[]
}

export type CreateDepositContractInput = {
  slug: string
  productName: string
  amount: number
  periodMonth: number
  accountPassword: string
  isSavings: boolean
  isHousing: boolean
  isChecking: boolean
  isRegularSavings: boolean
  autoTransferEnabled: boolean
  autoTransferDay?: number
  taxExempt?: boolean
}

const PRODUCT_ID_BY_SLUG: Record<string, number> = {
  'axful-regular': 1,
  'axful-super': 2,
  regular: 3,
  'axful-youth': 4,
  'axful-free': 5,
  'axful-dollar': 6,
  'axful-green': 7,
  'axful-soldier': 8,
  'axful-star-savings': 9,
  'housing-savings': 10,
  'youth-housing': 11,
  'axful-sok': 12,
  election: 13,
  'axful-living': 14,
  'axful-gs': 15,
  'monimo-daily': 16,
  'axful-moim': 17,
  'axful-star-account': 18,
  'axful-wallet': 19,
  'axful-free-account': 20,
  'axful-youth-account': 21,
}

const SLUG_BY_PRODUCT_ID = Object.fromEntries(
  Object.entries(PRODUCT_ID_BY_SLUG).map(([slug, productId]) => [productId, slug])
) as Record<number, string>

const SAVING_TYPE_BY_SLUG: Record<string, SavingType> = {
  'axful-free': 'FREE',
  'axful-dollar': 'FREE',
  'axful-green': 'FREE',
  'axful-star-savings': 'FREE',
  'axful-soldier': 'REGULAR',
  'axful-work': 'REGULAR',
  'axful-dream': 'REGULAR',
  'axful-together': 'REGULAR',
}

function headers(customerId: string) {
  return { 'X-Customer-Id': customerId }
}

function toNumber(value: number | string | undefined, fallback: number | undefined = 0) {
  if (value === undefined || value === null || value === '') return fallback
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

function toDateText(value?: string) {
  if (!value) return undefined
  return value.replace(/-/g, '.')
}

export function getCurrentDepositCustomerId() {
  if (typeof window === 'undefined') return 'CUST001'

  const direct = localStorage.getItem('customerId')
  if (direct) return direct

  try {
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    return user.customerId || user.customer_id || user.id || 'CUST001'
  } catch {
    return 'CUST001'
  }
}

export async function fetchDepositProducts(params?: {
  productType?: DepositProductType
  productStatus?: string
}) {
  const { data } = await depositApi.get<DepositProduct[]>('/products', { params })
  return data
}

export async function fetchDepositProduct(productId: number) {
  const { data } = await depositApi.get<DepositProduct>(`/products/${productId}`)
  return data
}

export function getDepositProductIdBySlug(slug: string) {
  return PRODUCT_ID_BY_SLUG[slug]
}

export function getDepositSlugByProductId(productId: number) {
  return SLUG_BY_PRODUCT_ID[productId] || `product-${productId}`
}

export function toDepositProductCard(product: DepositProduct) {
  const minMonth = product.minPeriodMonth
  const maxMonth = product.maxPeriodMonth
  const period =
    minMonth && maxMonth
      ? minMonth === maxMonth
        ? `${minMonth}개월`
        : `${minMonth}~${maxMonth}개월`
      : undefined

  return {
    id: getDepositSlugByProductId(product.productId),
    name: product.productName,
    channel: '인터넷·스타뱅킹',
    desc: product.description,
    period,
    rate:
      product.baseInterestRate !== undefined
        ? `연 ${Number(product.baseInterestRate).toLocaleString('ko-KR')}%`
        : undefined,
    canApply: product.productStatus ? product.productStatus === 'SELLING' : true,
  }
}

export async function fetchDepositContracts(customerId: string) {
  const { data } = await depositApi.get<DepositContract[]>('/contracts', {
    params: { customerId },
    headers: headers(customerId),
  })
  return data
}

export async function fetchDepositAccounts(customerId: string) {
  const { data } = await depositApi.get<DepositAccount[]>('/accounts', {
    params: { customerId },
    headers: headers(customerId),
  })
  return data
}

export async function terminateDepositContract(contractId: number, reason = 'ONLINE_TERMINATION') {
  const { data } = await depositApi.patch<DepositContract>(`/contracts/${contractId}/terminate`, {
    terminationReason: reason,
  })
  return data
}

export async function fetchDepositRecommendAgent(customerId: string, periodMonth = 3) {
  const { data } = await depositApi.get<DepositRecommendResponse>('/products/recommend-agent', {
    params: { customerId, periodMonth },
    headers: headers(customerId),
  })
  return data
}

async function resolveProductId(slug: string, productName: string) {
  if (PRODUCT_ID_BY_SLUG[slug]) return PRODUCT_ID_BY_SLUG[slug]

  const products = await fetchDepositProducts()
  const found = products.find((product) => product.productName === productName)
  if (!found) {
    throw new Error(`가입할 상품을 찾을 수 없습니다: ${productName}`)
  }
  return found.productId
}

export async function createDepositContract(customerId: string, input: CreateDepositContractInput) {
  const productId = await resolveProductId(input.slug, input.productName)
  const joinAmount = input.amount > 0 ? input.amount : 1
  const contractPeriodMonth = input.periodMonth > 0 ? input.periodMonth : 1

  const payload = {
    customerId,
    productId,
    joinAmount,
    contractPeriodMonth,
    joinChannel: 'WEB',
    totalPreferentialRate: 0,
    taxBenefitType: input.taxExempt ? 'NON_TAXABLE' : 'GENERAL',
    isAutoRenewal: false,
    autoTransferEnabled: input.autoTransferEnabled,
    autoTransferDay: input.autoTransferDay,
    savingType: input.isSavings ? SAVING_TYPE_BY_SLUG[input.slug] : undefined,
    accountPassword: input.accountPassword,
  }

  const { data } = await depositApi.post<DepositContract>('/contracts', payload, {
    headers: headers(customerId),
  })
  return data
}

function accountTypeLabel(account: DepositAccount, product?: DepositProduct): Account['type'] {
  if (account.accountType === 'SAVINGS') return '적금'
  if (account.accountType === 'SUBSCRIPTION') return '청약'
  if (product?.productName?.includes('통장')) return '입출금'
  return '예금'
}

function fallbackName(account: DepositAccount) {
  if (account.accountType === 'SAVINGS') return '가입 적금'
  if (account.accountType === 'SUBSCRIPTION') return '가입 청약'
  return '가입 예금'
}

export type DepositTransaction = {
  transactionId: number
  accountNumber?: string
  transactionType: string
  directionType: 'IN' | 'OUT'
  amount: number | string
  status: string
  transactionAt: string
  transactionSummary?: string
  transactionMemo?: string
  counterpartyAccountNo?: string
  counterpartyBankName?: string
  counterpartyName?: string
}

export async function fetchTransactions(params: { customerId?: string; accountId?: number }): Promise<DepositTransaction[]> {
  const { data } = await depositApi.get<DepositTransaction[]>('/transactions', { params })
  return data
}

export async function fetchTransaction(transactionId: number): Promise<DepositTransaction> {
  const { data } = await depositApi.get<DepositTransaction>(`/transactions/${transactionId}`)
  return data
}

export async function fetchDepositAccountViewModels(customerId: string): Promise<DepositViewAccount[]> {
  const accounts = await fetchDepositAccounts(customerId)
  const [contractsResult, productsResult] = await Promise.allSettled([
    fetchDepositContracts(customerId),
    fetchDepositProducts(),
  ])

  const contracts = contractsResult.status === 'fulfilled' ? contractsResult.value : []
  const products = productsResult.status === 'fulfilled' ? productsResult.value : []
  const contractById = new Map(contracts.map((contract) => [contract.contractId, contract]))
  const productById = new Map(products.map((product) => [product.productId, product]))

  return accounts
    .filter((account) => account.accountStatus !== 'CLOSED')
    .map((account) => {
      const contract = contractById.get(account.contractId)
      const product = contract ? productById.get(contract.productId) : undefined
      const balance = toNumber(account.balance)

      return {
        id: `deposit-${account.accountId}`,
        apiAccountId: account.accountId,
        contractId: account.contractId,
        accountStatus: account.accountStatus,
        savingType: account.savingType,
        number: account.accountNumber,
        type: accountTypeLabel(account, product),
        name: account.accountAlias || product?.productName || fallbackName(account),
        balance,
        availableBalance: balance,
        createdAt: toDateText(account.openedAt) || '',
        maturityDate: toDateText(account.maturityAt),
        monthlyAmount:
          account.accountType === 'SAVINGS'
            ? toNumber(account.totalPaidAmount || contract?.joinAmount, undefined)
            : undefined,
      }
    })
}
