'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useState, useEffect, useRef } from 'react'
import { api } from '@/lib/api'

// ============================================================
// GNB 메뉴 데이터
// megaMenu: 드롭다운 패널용 (카테고리 + 소분류 링크)
// ============================================================
export const GNB_MENUS = [
  {
    id: 'inquiry',
    label: '조회',
    href: '/inquiry/accounts',
    megaMenu: [
      {
        title: '계좌조회',
        href: '/inquiry/accounts',
        items: [
          { label: 'AX풀뱅크 계좌조회', href: '/inquiry/accounts' },
          { label: '다른금융 조회', href: '/inquiry/accounts/other' },
          { label: 'AXful금융그룹통합 조회', href: '/inquiry/accounts/kb-group' },
          { label: '휴면계좌 조회', href: '/inquiry/accounts/dormant' },
          { label: '전자통장 조회', href: '/inquiry/accounts/e-passbook' },
          { label: '장기미거래신탁계좌 조회', href: '/inquiry/accounts/trust' },
          { label: '계좌종합관리 계좌조회', href: '/inquiry/accounts/comprehensive' },
          { label: '착오송금 반환 동의', href: '/inquiry/accounts/erroneous' },
          { label: '오픈뱅킹 착오송금 반환 신청', href: '/inquiry/accounts/open-banking' },
          { label: '공채 본인부담금 조회', href: '/inquiry/accounts/bond' },
        ],
      },
      {
        title: '거래내역 조회',
        href: '/inquiry/transactions',
        items: [
          { label: '거래내역 조회', href: '/inquiry/transactions' },
          { label: '다른금융 통합거래내역조회', href: '/inquiry/transactions/other' },
        ],
      },
      {
        title: '전자어음 조회',
        href: '/inquiry/notes',
        items: [
          { label: '수취어음 조회', href: '/inquiry/notes/receive' },
          { label: '배서어음 조회', href: '/inquiry/notes/endorse' },
          { label: '반환관련어음 조회', href: '/inquiry/notes/return' },
          { label: '부도어음반환', href: '/inquiry/notes/dishonor' },
          { label: '상환청구', href: '/inquiry/notes/recourse' },
          { label: '보증전자어음', href: '/inquiry/notes/guarantee' },
          { label: '배서보증요청/결과내역 조회', href: '/inquiry/notes/endorse-guarantee' },
          { label: '만기결제어음 조회', href: '/inquiry/notes/maturity' },
          { label: '부도어음 조회', href: '/inquiry/notes/dishonor-list' },
          { label: '상환청구 정보조회', href: '/inquiry/notes/recourse-info' },
          { label: '사고신고 정보조회', href: '/inquiry/notes/accident' },
          { label: '확인증', href: '/inquiry/notes/certificate' },
        ],
      },
      {
        title: '에스크로 조회',
        href: '/inquiry/escrow',
        items: [],
      },
      {
        title: '수표어음 조회',
        href: '/inquiry/checks',
        items: [
          { label: '자기앞수표 조회', href: '/inquiry/checks/cashier' },
          { label: 'My자기앞수표 발행내역 조회', href: '/inquiry/checks/my-cashier' },
          { label: '당좌수표/어음 조회', href: '/inquiry/checks/check' },
          { label: '당좌수표/어음번호별 조회', href: '/inquiry/checks/check-no' },
        ],
      },
      {
        title: '어카운트인포',
        href: '/inquiry/account-info',
        items: [],
      },
      {
        title: '계약서류 관리',
        href: '/inquiry/contracts',
        items: [
          { label: '예금', href: '/inquiry/contracts/deposit' },
          { label: '펀드', href: '/inquiry/contracts/fund' },
          { label: '신탁/ISA', href: '/inquiry/contracts/trust-isa' },
          { label: '외화', href: '/inquiry/contracts/fx' },
          { label: '골드', href: '/inquiry/contracts/gold' },
          { label: '퇴직연금', href: '/inquiry/contracts/pension' },
        ],
      },
    ],
  },
  {
    id: 'transfer',
    label: '이체',
    href: '/transfer/account',
    megaMenu: [
      {
        title: '계좌이체',
        href: '/transfer/account',
        items: [
          { label: '계좌이체', href: '/transfer/account' },
          { label: '다른금융 계좌이체', href: '/transfer/account/other' },
          { label: '다계좌이체', href: '/transfer/account/multi' },
          { label: '잔액 모으기', href: '/transfer/account/gather' },
          { label: '잔액 모으기 예약', href: '/transfer/account/gather-reserve' },
          { label: '잔액 모으기 예약 관리', href: '/transfer/account/gather-manage' },
          { label: '퇴직급여(개인형IRP)이체', href: '/transfer/account/irp' },
          { label: '계좌종합관리 이체', href: '/transfer/account/comprehensive' },
        ],
      },
      {
        title: '이체결과 조회',
        href: '/transfer/result',
        items: [
          { label: '계좌이체결과 조회', href: '/transfer/result' },
          { label: '다른금융계좌 이체결과조회', href: '/transfer/result/other' },
          { label: '잔액 모으기 예약 결과조회', href: '/transfer/result/gather' },
          { label: '전화승인결과 조회', href: '/transfer/result/phone' },
          { label: '계좌종합관리 이체결과 조회', href: '/transfer/result/comprehensive' },
        ],
      },
      {
        title: '자동이체',
        href: '/transfer/auto',
        items: [
          { label: '자동이체내역 조회/해지/변경', href: '/transfer/auto' },
          { label: '자동이체 등록', href: '/transfer/auto/register' },
          { label: '자동이체결과확인증', href: '/transfer/auto/certificate' },
        ],
      },
      {
        title: '에스크로 이체',
        href: '/transfer/escrow',
        items: [
          { label: '에스크로 이체', href: '/transfer/escrow' },
          { label: '에스크로 이체결과 조회', href: '/transfer/escrow/result' },
        ],
      },
      {
        title: '자동이체 서비스',
        href: '/transfer/auto-service',
        items: [
          { label: '자동이체통합관리 서비스란?', href: '/transfer/auto-service' },
          { label: '자동이체 계좌변경', href: '/transfer/auto-service/change' },
          { label: '자동납부 계좌조회/출금계좌변경', href: '/transfer/auto-service/autopay' },
          { label: '자동납부 변경신청결과 조회', href: '/transfer/auto-service/autopay-result' },
          { label: '자동송금 계좌조회/출금계좌변경', href: '/transfer/auto-service/autosend' },
          { label: '자동송금 변경신청결과 조회', href: '/transfer/auto-service/autosend-result' },
        ],
      },
    ],
  },
  {
    id: 'utility',
    label: '공과금',
    href: '/utility',
    megaMenu: [
      {
        title: 'MY 공과금',
        href: '/utility/my',
        items: [
          { label: '공과금 등록/관리', href: '/utility/my/manage' },
          { label: '공과금 조회/납부', href: '/utility/my/pay' },
          { label: '납부확인증 보관함', href: '/utility/my/receipt' },
          { label: '예약납부등록', href: '/utility/my/reserve' },
          { label: '예약납부조회/취소', href: '/utility/my/reserve-cancel' },
        ],
      },
      {
        title: '지로/생활요금/CMS/펌뱅킹',
        href: '/utility/giro',
        items: [
          { label: '지로 납부/관리', href: '/utility/giro' },
          { label: '생활요금 납부', href: '/utility/giro/living' },
          { label: '납부내역 조회/취소', href: '/utility/giro/history' },
          { label: '자동납부 등록/해지', href: '/utility/giro/auto' },
        ],
      },
      {
        title: '지방세',
        href: '/utility/local-tax',
        items: [
          { label: '지방세 납부', href: '/utility/local-tax' },
          { label: '세외수입납부', href: '/utility/local-tax/non-tax' },
          { label: '기타세입금', href: '/utility/local-tax/other' },
          { label: '환경개선부담금', href: '/utility/local-tax/environment' },
          { label: '납부내역조회', href: '/utility/local-tax/history' },
        ],
      },
      {
        title: '4대보험료',
        href: '/utility/insurance',
        items: [
          { label: '통합사회보험료납부(연금/건강/고용/산재)', href: '/utility/insurance/total' },
          { label: '국민연금납부(반납금/주납보험료)', href: '/utility/insurance/pension' },
          { label: '산재보험료납부(연납/분기납)', href: '/utility/insurance/industrial' },
          { label: '고용보험료납부(연납/분기납)', href: '/utility/insurance/employment' },
          { label: '고용/산재자진납부', href: '/utility/insurance/self' },
          { label: '납부내역조회', href: '/utility/insurance/history' },
        ],
      },
      {
        title: '국세/관세',
        href: '/utility/tax',
        items: [
          { label: '국세', href: '/utility/tax/national' },
          { label: '관세', href: '/utility/tax/customs' },
          { label: '납부내역조회', href: '/utility/tax/history' },
        ],
      },
      {
        title: '기금/국고',
        href: '/utility/fund',
        items: [
          { label: '기금/국고납부', href: '/utility/fund' },
          { label: '납부내역조회', href: '/utility/fund/history' },
        ],
      },
      {
        title: '범칙/벌과금',
        href: '/utility/fine',
        items: [
          { label: '범칙/벌과금 납부', href: '/utility/fine' },
          { label: '납부내역조회', href: '/utility/fine/history' },
        ],
      },
      {
        title: '대학등록금',
        href: '/utility/tuition',
        items: [
          { label: '등록금 납부', href: '/utility/tuition' },
          { label: '납부조회/취소', href: '/utility/tuition/history' },
        ],
      },
      {
        title: '법원업무',
        href: '/utility/court',
        items: [
          { label: '공탁금 납부', href: '/utility/court/deposit' },
          { label: '송달료 납부', href: '/utility/court/delivery' },
          { label: '법원보관금 납부', href: '/utility/court/custody' },
          { label: '집행관보관금 납부', href: '/utility/court/execution' },
          { label: '등기신청수수료 납부', href: '/utility/court/registry' },
          { label: '소송등인지대 납부', href: '/utility/court/stamp' },
          { label: '납부내역조회', href: '/utility/court/history' },
          { label: '환급내역조회', href: '/utility/court/refund' },
        ],
      },
      {
        title: '도시철도채권 만기상환',
        href: '/utility/metro-bond',
        items: [
          { label: '채권상환', href: '/utility/metro-bond' },
          { label: '상환내역조회', href: '/utility/metro-bond/history' },
        ],
      },
    ],
  },
  {
    id: 'products',
    label: '금융상품',
    href: '/products/loan',
    megaMenu: [
      {
        title: '예금',
        href: '/products/deposit',
        items: [
          { label: '예금 상품/가입', href: '/products/deposit/list' },
          { label: '판매중지상품', href: '/products/deposit/discontinued' },
          { label: '예금 조회/해지', href: '/products/deposit/manage' },
          { label: '예금 관리', href: '/products/deposit/admin' },
          { label: '예금 가이드', href: '/products/deposit/guide' },
        ],
      },
      {
        title: '펀드',
        href: '/products/fund',
        items: [
          { label: '펀드 상품/가입', href: '/products/fund' },
          { label: '펀드 입금/환매/조회', href: '/products/fund/manage' },
          { label: '펀드 관리', href: '/products/fund/admin' },
          { label: '연금저축펀드', href: '/products/fund/pension' },
          { label: '펀드 투자정보', href: '/products/fund/info' },
          { label: '연금계좌 가져오기', href: '/products/fund/transfer' },
          { label: 'RIA 세제혜택 관련 유의사항 안내', href: '/products/fund/ria' },
        ],
      },
      {
        title: '대출',
        href: '/products/loan',
        items: [
          { label: '대출상품/신청', href: '/products/loan/credit' },
          { label: '대출진행현황', href: '/products/loan/status' },
          { label: '대출관리', href: '/products/loan/manage/rate' },
          { label: '대출 가이드', href: '/products/loan/guide' },
          { label: '신용평가 및 여신심사 자료제출', href: '/products/loan/credit' },
        ],
      },
      {
        title: '신탁',
        href: '/products/trust',
        items: [
          { label: '신탁 상품', href: '/products/trust' },
          { label: '신탁 조회/입금/해지', href: '/products/trust/manage' },
          { label: '신탁 관리', href: '/products/trust/admin' },
          { label: '신탁 관련 공시', href: '/products/trust/notice' },
          { label: '스튜어드십 코드', href: '/products/trust/stewardship' },
          { label: '채권상품 안내', href: '/products/trust/bond' },
          { label: '기타 안내사항', href: '/products/trust/etc' },
        ],
      },
      {
        title: 'ISA',
        href: '/products/isa',
        items: [
          { label: 'ISA 상품/가입', href: '/products/isa' },
          { label: 'ISA 조회/입금', href: '/products/isa/manage' },
          { label: 'ISA 해지(일임형)', href: '/products/isa/cancel' },
          { label: 'ISA 관리', href: '/products/isa/admin' },
          { label: 'ISA 가이드', href: '/products/isa/guide' },
          { label: 'RIA 세제혜택 관련 유의사항 안내', href: '/products/isa/ria' },
        ],
      },
      {
        title: '보험/공제',
        href: '/products/insurance',
        items: [
          { label: '보험 조회', href: '/products/insurance' },
          { label: '노란우산', href: '/products/insurance/yellow' },
          { label: '보험 가이드', href: '/products/insurance/guide' },
        ],
      },
      {
        title: '골드',
        href: '/products/gold',
        items: [
          { label: 'AXful골드뱅킹', href: '/products/gold' },
          { label: '골드바', href: '/products/gold/bar' },
          { label: '실버바', href: '/products/gold/silver' },
          { label: '기념주화/기념메달', href: '/products/gold/coin' },
        ],
      },
      {
        title: '외화예금',
        href: '/products/fx-deposit',
        items: [
          { label: '외화예금상품/가입', href: '/products/fx-deposit' },
          { label: '외화예금 주가입금', href: '/products/fx-deposit/add' },
          { label: '외화예금 조회/해지', href: '/products/fx-deposit/manage' },
          { label: '외화예금 금리안내', href: '/products/fx-deposit/rate' },
        ],
      },
    ],
  },
  {
    id: 'fx',
    label: '외환',
    href: '/fx',
    megaMenu: [
      {
        title: '외환정보관리',
        href: '/fx/info',
        items: [
          { label: '영문정보관리', href: '/fx/info/english' },
          { label: '외화송금주소록', href: '/fx/info/address' },
          { label: '외화알림서비스', href: '/fx/info/alert' },
          { label: '거래외국환은행지정', href: '/fx/info/bank' },
          { label: 'SWIFT전신문조회', href: '/fx/info/swift' },
          { label: '월드종합서비스(외환프라자)', href: '/fx/info/world' },
        ],
      },
      {
        title: '환율',
        href: '/fx/rate',
        items: [
          { label: '환율조회', href: '/fx/rate' },
          { label: '환율동향정보', href: '/fx/rate/trend' },
          { label: '환율알림서비스', href: '/fx/rate/alert' },
          { label: '환율배너', href: '/fx/rate/banner' },
          { label: '환율계산기', href: '/fx/rate/calculator' },
        ],
      },
      {
        title: '환전',
        href: '/fx/exchange',
        items: [
          { label: '환전신청', href: '/fx/exchange' },
          { label: '환전조회/관리', href: '/fx/exchange/manage' },
          { label: '마일리지 제휴 항공사 정보관리', href: '/fx/exchange/mileage' },
        ],
      },
      {
        title: '해외송금보내기',
        href: '/fx/remit-out',
        items: [
          { label: '바로송금', href: '/fx/remit-out' },
          { label: '정기예약송금', href: '/fx/remit-out/scheduled' },
          { label: '웨스턴유니온송금', href: '/fx/remit-out/western' },
          { label: '보낸송금내용변경/반환신청', href: '/fx/remit-out/change' },
          { label: '보낸송금내역조회', href: '/fx/remit-out/history' },
          { label: '무증빙 해외송금 내역조회', href: '/fx/remit-out/no-doc' },
          { label: '해외송금수수료납부/조회', href: '/fx/remit-out/fee' },
        ],
      },
      {
        title: '해외송금받기',
        href: '/fx/remit-in',
        items: [
          { label: '받은송금내역조회', href: '/fx/remit-in' },
          { label: '받은송금조회/관리(미입금분)', href: '/fx/remit-in/pending' },
          { label: '외국환매입(예치)증명서', href: '/fx/remit-in/certificate' },
          { label: '해외송금수취정보(통장사본)', href: '/fx/remit-in/account-copy' },
        ],
      },
      {
        title: '국내외화이체/외화예금입출금',
        href: '/fx/domestic',
        items: [
          { label: '외화이체/외화예금입출금', href: '/fx/domestic' },
          { label: '외화자동이체', href: '/fx/domestic/auto' },
          { label: '외화이체내역조회', href: '/fx/domestic/history' },
        ],
      },
      {
        title: 'AXful FX (외환매매플랫폼)',
        href: '/fx/star-fx',
        items: [
          { label: 'AXful FX', href: '/fx/star-fx' },
          { label: '마이딜링룸PRO', href: '/fx/star-fx/pro' },
        ],
      },
      {
        title: '외화수표',
        href: '/fx/check',
        items: [
          { label: '외화송금수표보낸내역조회', href: '/fx/check/sent' },
          { label: '외화수표환전내역조회', href: '/fx/check/exchange' },
          { label: '외화수표수수료납부/조회', href: '/fx/check/fee' },
        ],
      },
      {
        title: '해외투자',
        href: '/fx/overseas-invest',
        items: [
          { label: '해외투자사후보고대상', href: '/fx/overseas-invest/report' },
          { label: '해외직접투자안내', href: '/fx/overseas-invest/direct' },
          { label: '해외부동산투자안내', href: '/fx/overseas-invest/realestate' },
        ],
      },
    ],
  },
  {
    id: 'manage',
    label: '뱅킹관리',
    href: '/manage',
    megaMenu: [
      {
        title: '제증명발급',
        href: '/manage/certificates',
        items: [
          { label: '연말정산증명서', href: '/manage/certificates/year-end' },
          { label: '통장사본', href: '/manage/certificates/passbook' },
          { label: '예금잔액증명서', href: '/manage/certificates/balance' },
          { label: '예금잔액증명서 영업점 발급 신청', href: '/manage/certificates/balance-branch' },
          { label: '금융거래확인서', href: '/manage/certificates/transaction' },
          { label: '금융거래확인서 예약내역 조회/발급', href: '/manage/certificates/transaction-reserved' },
          { label: '부채증명서', href: '/manage/certificates/debt' },
          { label: '원천징수영수증 발급', href: '/manage/certificates/withholding' },
          { label: '금융소득종합과세 조회', href: '/manage/certificates/income-tax' },
          { label: '증명서재출력', href: '/manage/certificates/reprint' },
          { label: '통장/인감분실 재발행 및 예금잔액증명서 발급내역 조회', href: '/manage/certificates/loss' },
          { label: '통장/인감분실 재발행 신청', href: '/manage/certificates/loss-apply' },
          { label: '펀드판매사이동 계좌확인서', href: '/manage/certificates/fund-move' },
          { label: '계좌 개설 안내장(BDO Unibank)', href: '/manage/certificates/bdo' },
          { label: '외국환매입(예치)증명서', href: '/manage/certificates/fx' },
          { label: '비대면 위임장 제출 서비스', href: '/manage/certificates/proxy' },
        ],
      },
      {
        title: '계좌관리',
        href: '/manage/accounts',
        items: [
          { label: '출금계좌 등록/삭제/순위변경', href: '/manage/accounts/withdraw' },
          { label: '계좌비밀번호 신규/변경', href: '/manage/accounts/password' },
          { label: '빠른조회 서비스 등록/해지', href: '/manage/accounts/quick' },
          { label: '전자금융거래 제한계좌등록', href: '/manage/accounts/restrict' },
          { label: 'AXful 내맘대로 계좌번호서비스', href: '/manage/accounts/custom-no' },
          { label: '자동화기기 이용번호 변경', href: '/manage/accounts/atm' },
          { label: '계좌승기기 관리', href: '/manage/accounts/device' },
          { label: '오픈뱅킹 장기미사용 이체제한 조회/해제', href: '/manage/accounts/open-banking' },
          { label: '자주쓰는계좌 등록/삭제', href: '/manage/accounts/favorite' },
          { label: '단축이체 등록/삭제', href: '/manage/accounts/shortcut' },
          { label: '입금계좌 등록/삭제', href: '/manage/accounts/deposit' },
          { label: '즉시입금계좌 등록/삭제', href: '/manage/accounts/instant' },
          { label: '장기미사용이체제한 조회/해제', href: '/manage/accounts/long-unused' },
          { label: '이체한도 조회/감액', href: '/manage/accounts/limit' },
        ],
      },
      {
        title: '인터넷 뱅킹관리',
        href: '/manage/internet',
        items: [
          { label: 'ID조회/사용자암호 설정', href: '/manage/internet/id' },
          { label: '인터넷뱅킹 해지', href: '/manage/internet/cancel' },
          { label: 'AXful 뱅킹 알림서비스', href: '/manage/internet/alert' },
          { label: '입출금내역 자동통지 서비스', href: '/manage/internet/auto-notice' },
        ],
      },
      {
        title: '계좌종합관리서비스',
        href: '/manage/comprehensive',
        items: [
          { label: '계좌종합관리 신청/변경/해지', href: '/manage/comprehensive' },
          { label: '계좌종합관리 출금계좌등록/해제', href: '/manage/comprehensive/withdraw' },
        ],
      },
      {
        title: '이용안내',
        href: '/manage/guide',
        items: [
          { label: '첫 방문 고객을 위한 안내', href: '/manage/guide/first' },
          { label: '인터넷뱅킹 FAQ', href: '/manage/guide/faq' },
          { label: '이용시간 안내', href: '/manage/guide/hours' },
          { label: '인터넷뱅킹 이용안내', href: '/manage/guide/usage' },
          { label: '이용수수료 안내', href: '/manage/guide/fee' },
          { label: '이용약관 안내', href: '/manage/guide/terms' },
        ],
      },
    ],
  },
]

type TabMenuColumn = { title: string; href: string; items: { label: string; href: string }[] }
const PRODUCT_TAB_MENUS: Record<string, TabMenuColumn[]> = {
  '/products/deposit': [
    { title: '예금 상품/가입', href: '/products/deposit/list', items: [] },
    { title: '판매중지상품', href: '/products/deposit/discontinued', items: [
      { label: '일반 예금상품', href: '#' },
      { label: '지수연동예금', href: '#' },
    ]},
    { title: '예금 조회/해지', href: '/products/deposit/manage', items: [
      { label: '신규결과/내역 조회', href: '#' },
      { label: '현금인출카드 조회', href: '#' },
      { label: '예금해지', href: '#' },
      { label: '분할인출', href: '#' },
      { label: '해지예상 조회', href: '#' },
      { label: '해지결과/내역 조회', href: '#' },
      { label: '청약예·부금 이자지급 조회/이체', href: '#' },
    ]},
    { title: '예금 관리', href: '/products/deposit/admin', items: [
      { label: '예금전환', href: '#' },
      { label: '통장자동전환 서비스', href: '#' },
      { label: '세금우대/비과세종합 저축한도 조회/변경', href: '#' },
      { label: '자동재예치(재가입) 및 통지여부 변경', href: '#' },
      { label: '만기 자동해지 신청', href: '#' },
      { label: '만기해지방법 변경', href: '#' },
      { label: '예금잔액조회 통보', href: '#' },
      { label: '재형저축한도 변경', href: '#' },
      { label: '상품만기알림서비스 신청/해지', href: '#' },
      { label: '비대면 예적금 해지 제한', href: '#' },
    ]},
    { title: '예금 가이드', href: '/products/deposit/guide', items: [
      { label: '예금금리 안내', href: '#' },
      { label: '예금관련 수수료', href: '#' },
    ]},
  ],
  '/products/fund': [
    { title: '펀드 상품/가입', href: '/products/fund', items: [
      { label: '펀드검색', href: '#' },
      { label: '이달의 추천펀드', href: '#' },
      { label: '수익률/판매량 BEST 펀드', href: '#' },
      { label: '펀드 포트폴리오', href: '#' },
      { label: '최신 테마펀드', href: '#' },
      { label: '이벤트 진행중인 펀드', href: '#' },
    ]},
    { title: '펀드 입금/환매/조회', href: '/products/fund/manage', items: [
      { label: '펀드입금', href: '#' },
      { label: '펀드환매', href: '#' },
      { label: '환매예상 조회', href: '#' },
      { label: '펀드전환', href: '#' },
      { label: '펀드예약취소', href: '#' },
      { label: '펀드 해지계좌 조회', href: '#' },
      { label: '펀드기준가 조회', href: '#' },
      { label: '펀드 수익률 시뮬레이션', href: '#' },
      { label: '펀드청약취소', href: '#' },
    ]},
    { title: '펀드 관리', href: '/products/fund/admin', items: [
      { label: 'My펀드진단', href: '#' },
      { label: 'SMS/보고수신', href: '#' },
      { label: '계약기간 변경', href: '#' },
      { label: '입금한도 조회/변경', href: '#' },
      { label: '펀드CARE서비스', href: '#' },
      { label: '펀드계좌이동 신청', href: '#' },
      { label: '펀드자산분석', href: '#' },
      { label: '실질수익률 보고서', href: '#' },
      { label: '승낙확인', href: '#' },
    ]},
    { title: '연금저축펀드', href: '/products/fund/pension', items: [
      { label: '연금저축펀드상품', href: '#' },
      { label: '연금펀드 통합 신규', href: '#' },
      { label: '연금펀드 입금', href: '#' },
      { label: '운용펀드 매매/전환', href: '#' },
      { label: '연금펀드 해약', href: '#' },
      { label: '운용비율 등록/변경', href: '#' },
      { label: '연금지금 신청/변경', href: '#' },
      { label: '라페연금 관리', href: '#' },
      { label: 'My연금포탈', href: '#' },
    ]},
    { title: '펀드 투자정보', href: '/products/fund/info', items: [
      { label: '시황분석자료', href: '#' },
      { label: '펀드뉴스', href: '#' },
      { label: '펀드 가이드', href: '#' },
      { label: '수시공시', href: '#' },
      { label: '자산운용보고서', href: '#' },
      { label: '공지사항', href: '#' },
    ]},
    { title: '연금계좌 가져오기', href: '/products/fund/transfer', items: [
      { label: 'KB연금저축펀드 계좌이체신규', href: '#' },
      { label: 'KB연금저축펀드로 계좌이체신청', href: '#' },
      { label: '계좌이체 신청 조회', href: '#' },
    ]},
    { title: 'RIA 세제혜택 관련 유의사항 안내', href: '/products/fund/ria', items: [] },
  ],
  '/products/loan': [
    { title: '대출상품/신청', href: '/products/loan', items: [
      { label: '신용대출', href: '/products/loan/credit' },
      { label: '담보대출', href: '#' },
      { label: '전월세/반환보증', href: '#' },
      { label: '자동차대출', href: '#' },
      { label: '집단중도금/이주비대출', href: '#' },
      { label: '주택도시기금대출', href: '#' },
      { label: '개인사업자대출', href: '#' },
    ]},
    { title: '대출진행현황', href: '/products/loan/status', items: [
      { label: '진행현황조회/실행/예약', href: '/products/loan/status' },
      { label: '사후서류제출', href: '/products/loan/status/docs' },
      { label: '배우자정보제공동의', href: '#' },
      { label: '세대원정보제공동의', href: '#' },
      { label: '제3자담보정보제공동의', href: '#' },
      { label: '부동산담보대출 전자서명', href: '#' },
    ]},
    { title: '대출관리', href: '/products/loan/manage/rate', items: [
      { label: '적용금리조회', href: '#' },
      { label: '이자/월부금입금', href: '#' },
      { label: '대출상환', href: '#' },
      { label: '대출계약철회 예상조회/완제', href: '#' },
      { label: '대출한도변경/해지', href: '#' },
      { label: '기한연장', href: '#' },
      { label: '개인대출 금리인하요구권', href: '#' },
      { label: '해지계좌조회', href: '#' },
      { label: '금리산정내역서 조회', href: '#' },
      { label: '소멸시효완성에 따른 채무면제 결과조회', href: '#' },
      { label: '통장자동대출 이자납입내역 조회', href: '#' },
      { label: '개인대출 통지서비스 변경', href: '#' },
      { label: '개인대출 활부금(이자) 납입방법 변경', href: '#' },
      { label: '리브간편대출 계좌숨김 해제', href: '#' },
    ]},
    { title: '대출 가이드', href: '/products/loan/guide', items: [
      { label: '가계대출금리', href: '#' },
      { label: '대출관련 수수료', href: '#' },
      { label: '금리인하요구권', href: '#' },
      { label: '대출연체시 지연배상금액 예시', href: '#' },
      { label: '부가서비스', href: '#' },
      { label: '내용증명 우편미수신 주요정보 안내', href: '#' },
      { label: '추심관련 권리의무 및 권리구제방법 안내', href: '#' },
    ]},
    { title: '신용평가 및 여신심사 자료제출', href: '/products/loan/credit', items: [
      { label: '「업체현황 및 사업계획서」조회 및 작성', href: '#' },
      { label: '「FATI (재무 및 세무자료)」제출', href: '#' },
      { label: '「FATI (재무 및 세무자료)」제출내역조회', href: '#' },
    ]},
  ],
  '/products/trust': [
    { title: '신탁 상품', href: '/products/trust', items: [] },
    { title: '신탁 조회/입금/해지', href: '/products/trust/manage', items: [
      { label: '특정금전신탁 추가입금', href: '#' },
      { label: '기준가/수익률 조회', href: '#' },
      { label: '신탁자산운용현황', href: '#' },
      { label: '신탁재산운용보고서 신청', href: '#' },
      { label: '장기미거래신탁계좌 조회', href: '#' },
      { label: '미수령연금계좌 조회', href: '#' },
      { label: '신탁/ISA해지결과 조회', href: '#' },
      { label: '특정금전신탁 해지/해지예약', href: '#' },
      { label: '신탁 해지예약 취소', href: '#' },
      { label: '연금신탁 해지/해지예약', href: '#' },
      { label: '해지예상 조회', href: '#' },
      { label: '청약철회', href: '#' },
      { label: '신탁 체결내역 조회', href: '#' },
    ]},
    { title: '신탁 관리', href: '/products/trust/admin', items: [
      { label: '목표달성자동매도 등록/해제', href: '#' },
      { label: '월말수익률 SMS통보 등록/해제', href: '#' },
      { label: '안내희망수익률 SMS통보 등록/해제', href: '#' },
      { label: '연금신탁 연금수령 신청', href: '#' },
      { label: '연금신탁 연금수령신청 취소', href: '#' },
      { label: '연금신탁 한도 변경', href: '#' },
      { label: '연금신탁 계약기간 변경', href: '#' },
      { label: '신탁승낙확인', href: '#' },
    ]},
    { title: '신탁 관련 공시', href: '/products/trust/notice', items: [
      { label: '연금저축신탁 가입시 유의사항', href: '#' },
      { label: '연금저축신탁 계좌이체제도 안내', href: '#' },
      { label: '연금저축신탁 비교공시', href: '#' },
      { label: '신탁보수 공시', href: '#' },
    ]},
    { title: '스튜어드십 코드', href: '/products/trust/stewardship', items: [] },
    { title: '채권상품 안내', href: '/products/trust/bond', items: [] },
    { title: '기타 안내사항', href: '/products/trust/etc', items: [] },
  ],
  '/products/isa': [
    { title: 'ISA 상품/가입', href: '/products/isa', items: [
      { label: '일임형 ISA 신규', href: '#' },
      { label: '일임형 ISA 신규결과 조회', href: '#' },
      { label: '일임형 ISA 계좌이전 신청', href: '#' },
      { label: '일임형 ISA 계좌이전 진행현황 조회', href: '#' },
    ]},
    { title: 'ISA 조회/입금', href: '/products/isa/manage', items: [
      { label: '보유자산/운용현황 조회', href: '#' },
      { label: 'ISA 입금/이체', href: '#' },
    ]},
    { title: 'ISA 해지(일임형)', href: '/products/isa/cancel', items: [
      { label: '일임형 ISA 해지/일부해지 예상 조회 및 신청', href: '#' },
      { label: '일임형 ISA 해지예약취소', href: '#' },
    ]},
    { title: 'ISA 관리', href: '/products/isa/admin', items: [
      { label: '일임형 ISA 자동이체', href: '#' },
      { label: '일임형 ISA 모델 포트폴리오 변경', href: '#' },
      { label: '신탁형 ISA 운용지시', href: '#' },
      { label: '투자일임보고서 수령방법 변경', href: '#' },
      { label: '일임형 ISA 수익률 SMS 알림 신청', href: '#' },
      { label: 'ISA 만기연장', href: '#' },
    ]},
    { title: 'ISA 가이드', href: '/products/isa/guide', items: [
      { label: 'ISA 개요', href: '#' },
      { label: '일임형 vs 신탁형', href: '#' },
      { label: '세제혜택', href: '#' },
      { label: '모델 포트폴리오(MP)', href: '#' },
      { label: '보수 및 수수료 안내', href: '#' },
      { label: '공시자료', href: '#' },
    ]},
    { title: 'RIA 세제혜택 관련 유의사항 안내', href: '/products/isa/ria', items: [] },
  ],
  '/products/insurance': [
    { title: '보험 조회', href: '/products/insurance', items: [
      { label: '보험계약 조회', href: '#' },
      { label: '거래내역 조회', href: '#' },
      { label: '보험 해약환급금 조회', href: '#' },
    ]},
    { title: '노란우산', href: '/products/insurance/yellow', items: [
      { label: '노란우산 안내', href: '#' },
      { label: '노란우산 조회', href: '#' },
    ]},
    { title: '보험 가이드', href: '/products/insurance/guide', items: [
      { label: '방카슈랑스란?', href: '#' },
      { label: 'KB방카 가입안내', href: '#' },
      { label: '제휴사별 연락처안내', href: '#' },
      { label: '보험민원해결사', href: '#' },
      { label: '이용시간 안내', href: '#' },
      { label: 'FAQ/상담', href: '#' },
      { label: '모집수수료 안내', href: '#' },
      { label: '제휴사별 판매비중 안내', href: '#' },
    ]},
  ],
  '/products/gold': [
    { title: 'KB골드뱅킹', href: '/products/gold', items: [
      { label: 'KB골드뱅킹 상품안내', href: '#' },
      { label: 'KB골드투자통장 신규', href: '#' },
      { label: 'KB골드투자통장 입금', href: '#' },
      { label: 'KB골드투자통장 해지계좌조회', href: '#' },
      { label: 'KB골드투자통장 출금', href: '#' },
      { label: '포인트 금(Gold)전환 서비스(포인트리 포함)', href: '#' },
      { label: '골드 가격조회 및 시장동향', href: '#' },
      { label: '골드뱅킹 계산기', href: '#' },
    ]},
    { title: '골드바', href: '/products/gold/bar', items: [
      { label: '골드바 안내', href: '#' },
      { label: '골드바 가격조회', href: '#' },
      { label: '골드바 구매', href: '#' },
    ]},
    { title: '실버바', href: '/products/gold/silver', items: [
      { label: '실버바 안내', href: '#' },
      { label: '실버바 가격조회', href: '#' },
      { label: '실버바 구매', href: '#' },
    ]},
    { title: '기념주화/기념메달', href: '/products/gold/coin', items: [
      { label: '기념주화/기념메달 구매', href: '#' },
    ]},
  ],
  '/products/fx-deposit': [
    { title: '외화예금상품/가입', href: '/products/fx-deposit', items: [
      { label: '전체예금상품', href: '#' },
      { label: '판매중지상품', href: '#' },
    ]},
    { title: '외화예금 추가입금', href: '/products/fx-deposit/add', items: [] },
    { title: '외화예금 조회/해지', href: '/products/fx-deposit/manage', items: [
      { label: '외화예금자산조회', href: '#' },
      { label: '신규결과조회', href: '#' },
      { label: '자동연장결과조회', href: '#' },
      { label: '외화예금해지', href: '#' },
      { label: '해지결과조회', href: '#' },
    ]},
    { title: '외화예금 금리안내', href: '/products/fx-deposit/rate', items: [] },
  ],
}

const ALL_SERVICES = [
  {
    header: '금융서비스',
    items: [
      { label: '에스크로이체', href: '#' },
      { label: '보안센터', href: '#' },
      { label: '인증센터(개인)', href: '/cert' },
      { label: '인증센터(기업)', href: '#' },
    ],
  },
  {
    header: '특화서비스',
    items: [
      { label: '주택청약', href: '#' },
      { label: '국민주택채권', href: '#' },
      { label: '주택도시기금', href: '#' },
      { label: '스마트금융서비스', href: '#' },
      { label: 'AXful M', href: '#' },
      { label: 'AXful인증서', href: '#' },
    ],
  },
  {
    header: '멤버십서비스',
    items: [
      { label: 'AXful고객우대제도', href: '#' },
      { label: 'GOLD&WISE', href: '#' },
    ],
  },
  {
    header: 'AXful과 함께',
    items: [
      { label: '은행소개', href: '#' },
      { label: '지점안내', href: '#' },
      { label: '고객센터', href: '#' },
      { label: 'AXful정보광장', href: '#' },
      { label: '이벤트', href: '#' },
      { label: '희망금융클리닉', href: '#' },
      { label: 'AXful굿잡', href: '#' },
      { label: 'AXful의 생각', href: '#' },
    ],
  },
]

interface StoredUser { name: string; email: string; customer_id: number }

const SESSION_SECONDS = 10 * 60 // 10분

function formatTime(sec: number) {
  const m = String(Math.floor(sec / 60)).padStart(2, '0')
  const s = String(sec % 60).padStart(2, '0')
  return `${m}:${s}`
}

export default function Header() {
  const pathname = usePathname()
  const [activeMenu, setActiveMenu] = useState<string | null>(null)
  const [user, setUser] = useState<StoredUser | null>(null)
  const [remaining, setRemaining] = useState(SESSION_SECONDS)
  const [showAllServices, setShowAllServices] = useState(false)
  const allServicesRef = useRef<HTMLDivElement>(null)
  const [showCertMenu, setShowCertMenu] = useState(false)
  const certMenuRef = useRef<HTMLDivElement>(null)
  const [activeProductTab, setActiveProductTab] = useState<string | null>(null)

  useEffect(() => {
    try {
      const stored = localStorage.getItem('user')
      if (stored) setUser(JSON.parse(stored))
    } catch {}
  }, [])

  // 카운트다운 + 자동 로그아웃
  useEffect(() => {
    if (!user) return

    const stored = localStorage.getItem('sessionExpiry')
    const expiry = stored ? parseInt(stored) : Date.now() + SESSION_SECONDS * 1000
    if (!stored) localStorage.setItem('sessionExpiry', String(expiry))

    let seconds = Math.max(0, Math.round((expiry - Date.now()) / 1000))
    setRemaining(seconds)

    const tick = setInterval(() => {
      const storedExpiry = localStorage.getItem('sessionExpiry')
      if (!storedExpiry) {
        clearInterval(tick)
        window.location.href = '/logout'
        return
      }
      const remaining = Math.max(0, Math.round((parseInt(storedExpiry) - Date.now()) / 1000))
      setRemaining(remaining)
      if (remaining <= 0) {
        clearInterval(tick)
        localStorage.removeItem('access_token')
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('sessionExpiry')
        localStorage.removeItem('user')
        localStorage.removeItem('customerId')
        window.location.href = '/logout'
      }
    }, 1000)

    return () => {
      clearInterval(tick)
    }
  }, [user])

  useEffect(() => {
    if (!showAllServices) return
    function onMouseDown(e: MouseEvent) {
      if (allServicesRef.current && !allServicesRef.current.contains(e.target as Node)) {
        setShowAllServices(false)
      }
    }
    document.addEventListener('mousedown', onMouseDown)
    return () => document.removeEventListener('mousedown', onMouseDown)
  }, [showAllServices])

  useEffect(() => {
    if (!showCertMenu) return
    function onMouseDown(e: MouseEvent) {
      if (certMenuRef.current && !certMenuRef.current.contains(e.target as Node)) {
        setShowCertMenu(false)
      }
    }
    document.addEventListener('mousedown', onMouseDown)
    return () => document.removeEventListener('mousedown', onMouseDown)
  }, [showCertMenu])

  function handleLogout() {
    localStorage.removeItem('access_token')
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('sessionExpiry')
    localStorage.removeItem('user')
    localStorage.removeItem('customerId')
    window.location.href = '/logout?reason=manual'
  }

  async function handleExtend() {
    const refreshToken = localStorage.getItem('refreshToken')
    if (!refreshToken) {
      window.location.href = '/logout'
      return
    }
    try {
      const { data } = await api.post('/api/v1/auth/refresh', { refreshToken })
      localStorage.setItem('accessToken', data.data.accessToken)
      localStorage.setItem('access_token', data.data.accessToken)
      if (data.data.refreshToken) {
        localStorage.setItem('refreshToken', data.data.refreshToken)
      }
      const newExpiry = Date.now() + SESSION_SECONDS * 1000
      localStorage.setItem('sessionExpiry', String(newExpiry))
      setRemaining(SESSION_SECONDS)
    } catch {
      window.location.href = '/logout'
    }
  }

  const currentMenu = GNB_MENUS.find((m) => m.id === activeMenu)
  const isLoginPage = pathname === '/login'
  const hideGnb = pathname.startsWith('/cert') || pathname.startsWith('/cert-biz') || isLoginPage || pathname.startsWith('/support')

  const productsMenu = GNB_MENUS.find((m) => m.id === 'products')
  const activeProductCategory = activeProductTab
    ? productsMenu?.megaMenu.find((cat) => cat.href === activeProductTab)
    : null
  const activeProductTabMenu = activeProductTab ? PRODUCT_TAB_MENUS[activeProductTab] ?? null : null

  return (
    <header className="w-full bg-white relative z-50">
      {/* ===== 1. 글로벌 헤더 ===== */}
      <div className="max-w-kb-container mx-auto px-6 flex items-center h-[70px]">
        <Link href="/" className="flex items-center gap-3">
          <div className="w-[3px] self-stretch bg-kb-yellow" />
          <div className="flex flex-col leading-none gap-1.5">
            <span className="text-[22px] font-bold text-kb-text tracking-[0.1em]">AX풀뱅크</span>
            <span className="text-[10px] font-medium text-kb-text-muted tracking-[0.22em] uppercase">AXFULL BANK</span>
          </div>
        </Link>
        <nav className="flex items-center ml-auto">
          {['개인', '기업'].map((item, i) => (
            <span key={item} className="flex items-center">
              <Link href={item === '개인' ? '/personal' : '/biz'}
                className={`text-[15px] px-2 hover:text-kb-text transition-colors
                  ${item === '개인' ? 'text-kb-text font-semibold' : 'text-kb-text-muted'}`}>
                {item}
              </Link>
              {i === 0 && <span className="text-kb-border text-[15px]">|</span>}
            </span>
          ))}
          <span className="text-kb-border text-[15px] mx-1">|</span>
          {['자산관리', '부동산', '퇴직연금', '카드'].map((item) => (
            <Link key={item} href="#"
              className="text-[15px] text-kb-text-muted px-2 hover:text-kb-text transition-colors">
              {item}
            </Link>
          ))}
          <span className="text-kb-border text-[15px] mx-1">|</span>
          <div className="relative" ref={allServicesRef}>
            <button
              onClick={() => setShowAllServices((v) => !v)}
              className={`text-[15px] px-2 flex items-center gap-0.5 transition-colors
                ${showAllServices ? 'text-kb-text' : 'text-kb-text-muted hover:text-kb-text'}`}
            >
              전체서비스 <span className="text-[10px]">{showAllServices ? '▴' : '▾'}</span>
            </button>

            {showAllServices && (
              <div className="absolute right-0 top-full mt-2 bg-white border border-kb-border shadow-xl z-[200]" style={{ width: 720 }}>
                <div className="grid grid-cols-4 gap-0 divide-x divide-kb-border p-6">
                  {ALL_SERVICES.map((col) => (
                    <div key={col.header} className="px-5 first:pl-0 last:pr-0">
                      <p className="text-base font-semibold text-kb-text mb-5">{col.header}</p>
                      <ul className="space-y-3">
                        {col.items.map((item) => (
                          <li key={item.label}>
                            <Link
                              href={item.href}
                              onClick={() => setShowAllServices(false)}
                              className="text-sm text-kb-text-body hover:text-kb-text hover:underline"
                            >
                              {item.label}
                            </Link>
                          </li>
                        ))}
                      </ul>
                    </div>
                  ))}
                </div>
                <div className="border-t border-kb-border px-6 py-3 flex gap-6">
                  <span className="text-caption text-kb-text-muted">• 장기미사용이체제한 조회/해제</span>
                  <span className="text-caption text-kb-text-muted">• 해외IP차단 서비스</span>
                </div>
              </div>
            )}
          </div>
          <button className="text-[15px] text-kb-text-muted px-2 hover:text-kb-text flex items-center gap-0.5">
            GLOBAL <span className="text-[10px]">▾</span>
          </button>
        </nav>
        <div className="flex items-center gap-2 ml-3">
          <button className="text-kb-text-muted hover:text-kb-text">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
            </svg>
          </button>
          <button className="text-kb-text-muted hover:text-kb-text ml-1">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/>
            </svg>
          </button>
        </div>
      </div>

      {/* ===== 2. 사용자 영역 바 ===== */}
      {!isLoginPage && <div className="border-t border-kb-border">
        <div className="max-w-kb-container mx-auto px-6 h-[60px] flex items-center justify-between">
          <div className="flex items-center gap-4">
            {pathname.startsWith('/cert-biz')
              ? <span className="text-[28px] font-bold text-kb-text pl-8">인증센터(기업)</span>
              : pathname.startsWith('/cert')
              ? <span className="text-[28px] font-bold text-kb-text pl-8">인증센터(개인)</span>
              : pathname.startsWith('/support')
              ? <span className="text-[28px] font-bold text-kb-text pl-8">고객센터</span>
              : <span className="text-[28px] font-bold text-kb-text pl-8">개인</span>
            }
            {user ? (
              <>
                <Link href="/mypage" className="text-base text-kb-text-body ml-4 hover:underline hover:text-kb-text">{user.name}님</Link>
                <Link href="/mypage" className="flex items-center gap-1 text-base text-kb-text-body hover:text-kb-text">👤 My AXful</Link>
                <button className="flex items-center gap-1 text-base text-kb-text-body">
                  🔒 {formatTime(remaining)}
                </button>
                <button onClick={handleExtend} className="btn-outline !py-1.5 !px-4 !text-sm">연장</button>
                <button onClick={handleLogout} className="btn-outline !py-1.5 !px-4 !text-sm">로그아웃</button>
                <div className="relative" ref={certMenuRef}>
                  <button
                    onClick={() => setShowCertMenu((v) => !v)}
                    className="btn-outline !py-1.5 !px-4 !text-sm"
                  >
                    인증센터
                  </button>
                  {showCertMenu && <CertDropdown onClose={() => setShowCertMenu(false)} />}
                </div>
              </>
            ) : (
              <div className="ml-auto flex items-center gap-2">
                <Link href="/login" className="px-3 py-1 border border-kb-border-dark text-sm text-kb-text-body hover:bg-kb-beige-light transition-colors rounded-lg">로그인</Link>
                <div className="relative" ref={certMenuRef}>
                  <button
                    onClick={() => setShowCertMenu((v) => !v)}
                    className="px-3 py-1 border border-kb-border-dark text-sm text-kb-text-body hover:bg-kb-beige-light transition-colors rounded-lg"
                  >
                    인증센터
                  </button>
                  {showCertMenu && <CertDropdown onClose={() => setShowCertMenu(false)} />}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>}

      {/* ===== 3. GNB + 메가메뉴 ===== */}
      {!hideGnb && <nav
        className="bg-kb-gnb-personal relative"
        onMouseLeave={() => setActiveMenu(null)}
      >
        <div className="max-w-kb-container mx-auto px-6">
          <ul className="flex items-stretch w-full" style={{ height: '60px' }}>
            {GNB_MENUS.map((menu) => {
              const isActive = pathname === menu.href || pathname.startsWith(menu.href + '/')
              const isOpen = activeMenu === menu.id
              return (
                <li
                  key={menu.id}
                  className="flex grow"
                  onMouseEnter={() => setActiveMenu(menu.id)}
                >
                  <Link
                    href={menu.href}
                    onClick={() => setActiveMenu(null)}
                    className={`
                      flex items-center justify-center w-full px-5
                      text-[19px] font-semibold transition-colors duration-kb whitespace-nowrap
                      ${isActive
                        ? 'bg-kb-gnb-personal-active text-white font-bold'
                        : isOpen
                        ? 'bg-kb-gnb-personal-hover text-[#052e20]'
                        : 'text-[#052e20] hover:bg-kb-gnb-personal-hover'}
                    `}
                  >
                    {menu.label}
                  </Link>
                </li>
              )
            })}
          </ul>
        </div>

        {/* ===== 메가메뉴 드롭다운 ===== */}
        {activeMenu && currentMenu && (
          <div className="absolute top-full left-0 right-0 bg-white border-b border-kb-border shadow-md z-50">
            <div className="max-w-kb-container mx-auto px-6 py-6">
              <div className="grid grid-cols-5 gap-y-6">
                {currentMenu.megaMenu.map((category, ci) => (
                  <div key={category.title}
                    className={ci % 5 !== 0 ? 'border-l border-kb-border pl-6' : 'pr-6'}>
                    <Link
                      href={category.href}
                      onClick={() => setActiveMenu(null)}
                      className="block text-[17px] font-bold text-kb-text mb-1 hover:text-kb-taupe"
                    >
                      {category.title}
                    </Link>
                    {category.items.length > 0 && (
                      <ul className="space-y-0.5">
                        {category.items.map((item) => (
                          <li key={item.href} className="rounded hover:bg-[#e8f7f2] transition-colors duration-100">
                            <Link
                              href={item.href}
                              onClick={() => setActiveMenu(null)}
                              className="block text-[15px] px-4 py-2 text-kb-text-body hover:text-kb-text"
                            >
                              {item.label}
                            </Link>
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </nav>}

      {/* ===== 4. 금융상품 서브탭 ===== */}
      {!hideGnb && pathname.startsWith('/products') && (
        <div
          className="bg-white border-b border-kb-border relative z-40"
          onMouseLeave={() => setActiveProductTab(null)}
        >
          <div className="max-w-kb-container mx-auto px-6">
            <ul className="flex w-full">
              {[
                { label: '예금',    href: '/products/deposit' },
                { label: '펀드',    href: '/products/fund' },
                { label: '대출',    href: '/products/loan' },
                { label: '신탁',    href: '/products/trust' },
                { label: 'ISA',     href: '/products/isa' },
                { label: '보험/공제', href: '/products/insurance' },
                { label: '골드',    href: '/products/gold' },
                { label: '외화예금', href: '/products/fx-deposit' },
              ].map(tab => {
                const isActive = pathname.startsWith(tab.href)
                return (
                  <li key={tab.label} className="flex-1" onMouseEnter={() => setActiveProductTab(tab.href)}>
                    <Link
                      href={tab.href}
                      onClick={() => setActiveProductTab(null)}
                      className={`flex justify-center py-3 text-[15px] transition-colors ${
                        isActive
                          ? 'font-bold text-kb-text'
                          : 'text-kb-text-muted hover:text-kb-text'
                      }`}
                    >
                      {tab.label}
                    </Link>
                  </li>
                )
              })}
            </ul>
          </div>

          {activeProductTabMenu && (
            <div className="absolute top-full left-0 right-0 bg-white border-b border-kb-border shadow-md z-50">
              <div className="max-w-kb-container mx-auto px-6 py-6">
                <div className="grid grid-cols-5 gap-y-6">
                  {activeProductTabMenu.map((col, ci) => (
                    <div key={col.title} className={ci % 5 !== 0 ? 'border-l border-kb-border pl-6' : 'pr-6'}>
                      <Link
                        href={col.href}
                        onClick={() => setActiveProductTab(null)}
                        className="block text-[17px] font-bold text-kb-text mb-1 hover:text-kb-taupe"
                      >
                        {col.title}
                      </Link>
                      {col.items.length > 0 && (
                        <ul className="space-y-0.5">
                          {col.items.map(item => (
                            <li key={item.label} className="rounded hover:bg-[#e8f7f2] transition-colors duration-100">
                              <Link
                                href={item.href}
                                onClick={() => setActiveProductTab(null)}
                                className="block text-[15px] px-4 py-2 text-kb-text-body hover:text-kb-text"
                              >
                                {item.label}
                              </Link>
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
          {!activeProductTabMenu && activeProductCategory && activeProductCategory.items.length > 0 && (
            <div className="absolute top-full left-0 right-0 bg-white border-b border-kb-border shadow-md z-50">
              <div className="max-w-kb-container mx-auto px-6 py-4">
                <ul className="flex flex-wrap gap-x-6 gap-y-0.5">
                  {activeProductCategory.items.map(item => (
                    <li key={item.href} className="rounded hover:bg-[#e8f7f2] transition-colors duration-100">
                      <Link
                        href={item.href}
                        onClick={() => setActiveProductTab(null)}
                        className="block text-[15px] px-4 py-2 text-kb-text-body hover:text-kb-text whitespace-nowrap"
                      >
                        {item.label}
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ===== 5. 검색바 (메인홈 전용) ===== */}
      {!hideGnb && (pathname === '/' || pathname === '/personal') && (
        <div className="bg-white">
          <div className="max-w-kb-container mx-auto px-6 py-3 flex items-center gap-4">
            <div className="flex items-center border border-kb-border rounded-full px-5 py-2 gap-2" style={{ width: 480 }}>
              <input
                type="text"
                placeholder="검색어를 입력하세요."
                className="flex-1 text-[17px] text-kb-text-body outline-none bg-transparent"
              />
              <button className="flex-shrink-0" style={{ color: '#6B4C35' }}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                  <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
                </svg>
              </button>
            </div>
            <button className="px-3 text-[15px] text-white font-medium rounded-full leading-6" style={{ backgroundColor: '#2D6A4F' }}>
              인기
            </button>
          </div>
        </div>
      )}
    </header>
  )
}

function CertDropdown({ onClose }: { onClose: () => void }) {
  return (
    <div className="absolute left-0 top-full mt-1 bg-white border border-kb-border shadow-md z-[200] flex gap-1 p-2">
      <Link
        href="/cert"
        onClick={onClose}
        className="px-3 py-1 text-caption text-kb-text hover:bg-kb-yellow transition-colors whitespace-nowrap rounded-lg"
      >
        개인
      </Link>
      <Link
        href="/cert-biz"
        onClick={onClose}
        className="px-3 py-1 text-caption text-kb-text hover:bg-kb-yellow transition-colors whitespace-nowrap rounded-lg"
      >
        기업
      </Link>
    </div>
  )
}
