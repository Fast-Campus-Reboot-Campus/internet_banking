export type Account = {
  id: string
  number: string
  type: '입출금' | '적금' | '예금' | '청약'
  name: string
  badge?: string
  balance: number
  availableBalance: number
  createdAt: string
  maturityDate?: string
  monthlyAmount?: number
}

export type Transaction = {
  id: string
  accountId: string
  datetime: string
  description: string
  sender?: string
  amount: number
  balance: number
  memo?: string
  branch?: string
}

export const MOCK_ACCOUNTS: Account[] = [
  {
    id: 'acc1',
    number: '531089-04-274618',
    type: '입출금',
    name: 'AXful ONE통장-보통예금',
    badge: '한도제한계좌',
    balance: 1_000_000,
    availableBalance: 1_000_000,
    createdAt: '2026.01.15',
  },
]

export const MOCK_TRANSACTIONS: Transaction[] = [
  { id: 'tx1',  accountId: 'acc1', datetime: '2026.05.21 09:15', description: '카카오페이',          amount: -12_000,    balance: 3_850_457, branch: '인터넷' },
  { id: 'tx2',  accountId: 'acc1', datetime: '2026.05.20 18:00', description: '급여',                sender: '(주)AXful테크', amount: 3_000_000,  balance: 3_862_457, branch: '인터넷' },
  { id: 'tx3',  accountId: 'acc1', datetime: '2026.05.20 11:05', description: 'AXful카드대금',        amount: -450_000,   balance: 862_457,   branch: '인터넷' },
  { id: 'tx4',  accountId: 'acc1', datetime: '2026.05.19 16:30', description: '박지우',               sender: '박지우',     amount: -50_000,    balance: 1_312_457, memo: '저녁값', branch: '인터넷' },
  { id: 'tx5',  accountId: 'acc1', datetime: '2026.05.18 10:00', description: '이자',                                     amount: 2_000,      balance: 1_362_457, branch: '인터넷' },
  { id: 'tx6',  accountId: 'acc1', datetime: '2026.05.17 13:45', description: 'CU편의점',              amount: -8_500,     balance: 1_360_457, branch: '인터넷' },
  { id: 'tx7',  accountId: 'acc1', datetime: '2026.05.15 09:00', description: 'AXful청약저축',         amount: -200_000,   balance: 1_368_957, branch: '인터넷' },
  { id: 'tx8',  accountId: 'acc1', datetime: '2026.05.10 12:00', description: '김수현',               sender: '김수현',     amount: 200_000,    balance: 1_568_957, memo: '책값', branch: '인터넷' },
  { id: 'tx9',  accountId: 'acc1', datetime: '2026.05.05 08:30', description: '자동이체-통신비',        amount: -55_000,    balance: 1_368_957, branch: '인터넷' },
  { id: 'tx10', accountId: 'acc1', datetime: '2026.05.01 00:05', description: '자동이체-월세',          amount: -700_000,   balance: 1_423_957, branch: '인터넷' },
]

export const MOCK_RECENT_ACCOUNTS = [
  { bank: '신한', name: '김수현', number: '302-7823-4501-02', amount: 30_000, date: '2026.02.26 20:23' },
  { bank: '하나', name: '박지우', number: '178-910034-82657', amount: 50_000, date: '2026.01.15 14:10' },
]

export const MOCK_BANKS = [
  { code: 'KB',  name: 'AXful' },
  { code: 'IBK', name: '기업' },
  { code: 'NH',  name: '농협' },
  { code: 'IBD', name: '산업' },
  { code: 'SH',  name: '수협' },
  { code: 'SHB', name: '신한' },
  { code: 'WR',  name: '우리' },
  { code: 'KP',  name: '우체국' },
  { code: 'HN',  name: '하나' },
  { code: 'CT',  name: '한국씨티' },
  { code: 'SC',  name: 'SC제일' },
  { code: 'KB2', name: '카카오뱅크' },
  { code: 'K',   name: '케이뱅크' },
  { code: 'TS',  name: '토스뱅크' },
  { code: 'KN',  name: '경남' },
  { code: 'GJ',  name: '광주' },
  { code: 'IM',  name: '아이엠뱅크(구 대구)' },
  { code: 'BS',  name: '부산' },
  { code: 'JB',  name: '전북' },
  { code: 'JJ',  name: '제주' },
  { code: 'SV',  name: '저축' },
  { code: 'SF',  name: '산림조합' },
  { code: 'SM',  name: '새마을' },
  { code: 'CU',  name: '신협' },
  { code: 'DZ',  name: '도이지' },
  { code: 'BA',  name: '뱅크오브아메리카' },
  { code: 'CCB', name: '중국건설' },
  { code: 'ICB', name: '중국공상' },
  { code: 'BOC', name: '중국' },
  { code: 'HS',  name: 'HSBC' },
  { code: 'BN',  name: 'BNP파리바' },
  { code: 'JP',  name: 'JP모간체이스' },
  { code: 'DAON', name: '다온' },
]

export function formatNumber(n: number) {
  return n.toLocaleString('ko-KR')
}
