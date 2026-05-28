# AX풀뱅크 — 인터넷뱅킹 프론트엔드 클론

국민은행 인터넷뱅킹 UI를 벤치마킹한 Next.js 프론트엔드 프로젝트입니다.  
브랜드명·컬러·문구는 **AX풀뱅크(AXFULL BANK)** 로 픽션화되어 있으며, KB국민은행 표기는 사용하지 않습니다.

---

## 목차

1. [기술 스택](#기술-스택)
2. [시작하기](#시작하기)
3. [디렉토리 구조](#디렉토리-구조)
4. [페이지 라우트](#페이지-라우트)
5. [디자인 토큰](#디자인-토큰)
6. [컴포넌트](#컴포넌트)
7. [개발 규칙](#개발-규칙)

---

## 기술 스택

| 항목 | 버전 / 도구 |
|------|------------|
| Framework | Next.js 15 (App Router) |
| Language | TypeScript |
| Styling | Tailwind CSS v3 (JIT) |
| Font | Noto Sans KR |
| 상태관리 | React `useState` / `useEffect` (로컬) |
| 인증 시뮬레이션 | `localStorage` JWT 파싱 |
| 패키지 매니저 | npm |

---

## 백엔드 서비스 연동

프론트엔드는 다음 백엔드 서비스와 실제 API로 연결되어 있습니다.

| lib 파일 | 연결 서비스 | 기본 포트 | 연결된 페이지 |
|---|---|---|---|
| `deposit-api.ts` | deposit-service | 8082 | 개인홈, 대시보드, 계좌조회, 거래내역, 이체, 예금 상품 전체, 신규내역, 해지, 전환, 챗봇 |
| `consultation-api.ts` | consultation-service | 8090 | 챗봇 위젯(현금흐름·상품추천·상담사 연결), 실시간 상담 채팅 |
| `loan-api.ts` | loan-service | 8083 | (서비스 실행 시 연동 가능) |
| `advisory-api.ts` | advisory-service | 8084 | 어드민 감사 리포트 |
| `master-api.ts` | master-service | 8085 | 공통 코드 관리 |
| `payment-api.ts` | payment-service | 8087 | 결제 처리 |
| `ai-api.ts` | ai-service | 8086 | RAG 검색·문서 관리 |

`.env.local` 설정 (프로젝트 루트에 생성):

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_DEPOSIT_API_URL=http://localhost:8082/api
NEXT_PUBLIC_CONSULTATION_API_URL=http://localhost:8090
NEXT_PUBLIC_LOAN_API_URL=http://localhost:8083
NEXT_PUBLIC_ADVISORY_API_URL=http://localhost:8084
NEXT_PUBLIC_MASTER_API_URL=http://localhost:8085
NEXT_PUBLIC_AI_API_URL=http://localhost:8086
NEXT_PUBLIC_PAYMENT_API_URL=http://localhost:8087
```

---

## 시작하기

```bash
# 의존성 설치
npm install

# 개발 서버 실행 (포트 3001)
npm run dev

# 빌드
npm run build
```

개발 서버 실행 후 [http://localhost:3001](http://localhost:3001) 접속.

> **deposit-service(8082)와 consultation-service(8090)가 먼저 실행 중이어야** 계좌·거래·챗봇 기능이 동작합니다.

### 테스트 계정

로그인 페이지(`/login`)에서 아래 계정으로 로그인:

| 아이디 | 비밀번호 | 고객번호 |
|--------|----------|----------|
| (customer-service 계정) | — | `CUST001` |

> 로그인 후 `localStorage`에 `customerId: CUST001`이 저장되어야 deposit-service 계좌 조회가 됩니다.

---

## 디렉토리 구조

```
kb-clone/
├── app/
│   ├── (personal)/          # 개인뱅킹 라우트 그룹 (공통 레이아웃 적용)
│   │   ├── layout.tsx       # Header + FloatingSidebar 포함
│   │   ├── login/
│   │   ├── personal/        # 개인홈
│   │   ├── dashboard/
│   │   ├── mypage/
│   │   ├── accounts/
│   │   ├── inquiry/         # 계좌조회
│   │   ├── transfer/        # 이체
│   │   ├── banking/         # 뱅킹 설정
│   │   ├── cert/            # 인증센터 (개인)
│   │   ├── cert-biz/        # 인증센터 (기업)
│   │   ├── products/
│   │   │   ├── deposit/     # 예금
│   │   │   └── loan/        # 대출
│   │   ├── loans/           # 대출 신청 플로우
│   │   ├── support/         # 고객센터
│   │   ├── manage/          # 증명서 발급 등
│   │   └── settings/
│   ├── page.tsx             # 루트 → /personal 리다이렉트
│   └── globals.css          # Tailwind 베이스 + KB 공통 컴포넌트 클래스
│
├── components/
│   ├── layout/
│   │   ├── Header.tsx       # 글로벌 헤더 (GNB 포함)
│   │   ├── PageLayout.tsx
│   │   ├── AuthGuard.tsx
│   │   ├── FloatingSidebar.tsx
│   │   └── ConsultModal.tsx # 상담신청 공용 모달
│   ├── inquiry/
│   │   ├── LoanSidebar.tsx  # 대출 사이드바 (usePathname 기반 동적 active)
│   │   ├── InquirySidebar.tsx
│   │   ├── TransferSidebar.tsx
│   │   ├── ManageSidebar.tsx
│   │   └── ProductSidebar.tsx
│   ├── products/
│   │   ├── DepositSidebar.tsx # 예금 사이드바 (usePathname 기반 동적 active)
│   │   ├── CartModal.tsx    # 장바구니 공용 모달
│   │   └── RateModal.tsx    # 금리 상세 공용 모달
│   ├── home/
│   │   ├── HeroCarousel.tsx
│   │   ├── HeroWithQuickMenu.tsx
│   │   └── ProductShowcase.tsx
│   ├── biz/                 # 기업홈 전용 컴포넌트
│   ├── admin/               # 어드민 전용
│   └── ui/
│       └── button.tsx
│
├── tailwind.config.ts       # 디자인 토큰 정의
└── app/globals.css          # 공통 컴포넌트 클래스 (.btn, .input, .card 등)
```

---

## 페이지 라우트

### 인증 / 로그인

| 경로 | 설명 |
|------|------|
| `/login` | 로그인 |
| `/logout` | 로그아웃 처리 |
| `/cert` | 인증센터 (개인) |
| `/cert/cert-management` | 인증서 관리 |
| `/cert/fin-cert-issue` | 금융인증서 발급 |
| `/cert/joint-cert-issue` | 공동인증서 발급 |
| `/cert/joint-cert-management` | 공동인증서 관리 |
| `/cert-biz` | 인증센터 (기업) |
| `/cert-biz/kb-cert-issue` | 기업 인증서 발급 |

### 개인홈 / 마이페이지

| 경로 | 설명 |
|------|------|
| `/personal` | 개인홈 메인 |
| `/dashboard` | 대시보드 |
| `/mypage` | 마이페이지 |
| `/settings` | 설정 |

### 계좌 / 이체

| 경로 | 설명 |
|------|------|
| `/accounts/[accountId]` | 계좌 상세 |
| `/inquiry/accounts` | 계좌 조회 |
| `/inquiry/transactions` | 거래내역 조회 |
| `/transfer/account` | 계좌이체 |
| `/transfer/other-bank` | 타행 이체 |
| `/transfer/other-bank/register` | 타행 계좌 등록 |
| `/transfer/other-bank/register/terms` | 타행 등록 약관 |
| `/transfer/confirm` | 이체 확인 |
| `/transfer/result` | 이체 결과 |
| `/transfer/inquiry` | 이체 내역 조회 |
| `/transfer/auto-service/change` | 자동이체 변경 |
| `/banking/first-visit` | 첫 방문 설정 |
| `/banking/transfer-limit` | 이체 한도 설정 |
| `/manage/certificates/year-end` | 연말정산 증명서 |

### 예금

| 경로 | 설명 |
|------|------|
| `/products/deposit` | 예금 메인 |
| `/products/deposit/list` | 예금 상품 목록 |
| `/products/deposit/[id]` | 예금 상품 상세 (장바구니·상담·금리 모달 포함) |
| `/products/deposit/join/[id]` | 예금 가입 플로우 |
| `/products/deposit/inquiry/new` | 신규결과/내역 조회 |
| `/products/deposit/inquiry/terminate` | 예금해지 |
| `/products/deposit/inquiry/terminate-result` | 해지결과/내역 조회 |
| `/products/deposit/manage/convert` | 예금전환 |

**예금 상품 ID**

| id | 상품명 |
|----|--------|
| `axful-regular` | AXful 정기예금 |
| `axful-super` | AXful 수퍼정기예금(개인) |
| `regular` | 일반정기예금 |
| `axful-youth` | AXful 청년도약계좌 |

### 대출

| 경로 | 설명 |
|------|------|
| `/products/loan` | 대출 메인 |
| `/products/loan/credit` | 신용대출 목록 |
| `/products/loan/credit/[id]` | 신용대출 상세 (계산기·장바구니·상담 모달 포함) |
| `/products/loan/mortgage` | 담보대출 |
| `/products/loan/jeonse` | 전월세/반환보증 |
| `/products/loan/auto` | 자동차대출 |
| `/products/loan/group` | 집단중도금/이주비대출 |
| `/products/loan/khfc` | 주택도시기금대출 |
| `/products/loan/status` | 대출진행현황 |
| `/products/loan/status/[slug]` | 진행현황 상세 |
| `/products/loan/manage/[slug]` | 대출관리 (금리조회·이자입금·상환 등) |
| `/products/loan/guide/[slug]` | 대출 가이드 |
| `/products/loan/credit-eval/[slug]` | 신용평가 자료제출 |
| `/loans/apply` | 대출 신청 |
| `/loans/apply/result` | 대출 신청 결과 |

**대출관리 slug**

| slug | 기능 |
|------|------|
| `rate` | 적용금리조회 |
| `payment` | 이자/월부금입금 |
| `repay` | 대출상환 |
| `withdraw` | 대출계약철회 예상조회/완제 |
| `limit` | 대출한도변경/해지 |
| `extend` | 기한연장 |
| `rate-cut` | 금리인하요구권 |
| `closed` | 해지계좌조회 |
| `rate-detail` | 금리산정내역서 조회 |
| `debt-relief` | 소멸시효 채무면제 결과조회 |
| `auto-interest` | 통장자동대출 이자납입내역 조회 |
| `notify` | 통지서비스 변경 |
| `payment-method` | 할부금 납입방법 변경 |

### 고객센터

| 경로 | 설명 |
|------|------|
| `/support/consultation/branch` | 지점 상담 예약서비스 |

> `/support` 진입 시 Header가 자동으로 "고객센터" 타이틀 + 고객센터 전용 GNB를 표시합니다.

---

## 디자인 토큰

모든 토큰은 `tailwind.config.ts`에 정의되어 있습니다.

### 컬러

#### 메인 브랜드 (민트)

| 토큰 | 값 | 용도 |
|------|----|------|
| `kb-yellow` | `#5BC9A8` | CTA 버튼, active 상태, 탭 강조 |
| `kb-yellow-dark` | `#3FA889` | 호버 |
| `kb-yellow-light` | `#A8E5D4` | 옅은 강조 배경 |

#### 텍스트

| 토큰 | 값 | 용도 |
|------|----|------|
| `kb-text` | `#1A1A1A` | 제목·강조 텍스트 |
| `kb-text-body` | `#333333` | 본문 텍스트 |
| `kb-text-muted` | `#777777` | 보조·비활성 텍스트 |
| `kb-text-light` | `#AAAAAA` | placeholder |

#### 배경 / 테두리

| 토큰 | 값 | 용도 |
|------|----|------|
| `kb-border` | `#C5D5CD` | 기본 테두리 |
| `kb-border-dark` | `#9AAEA4` | 강조 테두리 |
| `kb-beige` | `#F5F0E8` | 배경 베이지 |
| `kb-beige-light` | `#FAFAF7` | 크림 흰 배경, 테이블 헤더 |

#### 시맨틱

| 토큰 | 값 | 용도 |
|------|----|------|
| `kb-red` | `#D0021B` | 에러·경고 |
| `kb-blue` | `#0066CC` | 링크 |
| `kb-blue-dark` | `#1A56DB` | 인터넷뱅킹 뱃지, 상담 전화번호 |
| `kb-green` | `#2E8B6F` | 성공·긍정 |
| `kb-orange` | `#FF6B35` | 예금 금리 강조 |
| `kb-tab-line` | `#E8A020` | 예금 상세 탭 active 언더라인 |
| `kb-error-soft` | `#C05050` | 계산기 결과 강조 |

#### 고객센터

| 토큰 | 값 | 용도 |
|------|----|------|
| `kb-brown` | `#5D3D2B` | 고객센터 탭바 배경 |
| `kb-brown-dark` | `#3D2B1F` | 고객센터 다크 브라운 |

#### 대출 전용

| 토큰 | 값 | 용도 |
|------|----|------|
| `kb-gold` | `#C09B3A` | 스타뱅킹 뱃지 |
| `kb-teal` | `#4A90D9` | 대출 상세 정보 아이콘 |
| `kb-btn-dark` | `#5A504A` | 계산기 결과보기 버튼 |
| `kb-btn-gray` | `#5C5C5C` | 예금 계산기 결과보기 버튼 |

### 레이아웃

| 토큰 | 값 | 용도 |
|------|----|------|
| `max-w-kb-container` | `1280px` | 페이지 최대 너비 |
| `w-sidebar` | `200px` | 사이드바 너비 |
| `h-header` | `60px` | 글로벌 헤더 높이 |
| `h-gnb` | `130px` | GNB 높이 |

### 타이포그래피

| 토큰 | 크기 | 용도 |
|------|------|------|
| `text-display` | 22px / bold | 대형 제목 |
| `text-h1` | 20px / bold | 페이지 제목 |
| `text-h2` | 18px / bold | 섹션 제목 |
| `text-h3` | 16px / bold | 서브 섹션 |
| `text-body` | 14px | 본문 |
| `text-caption` | 13px | 보조 텍스트 |
| `text-small` | 12px | 주석·라벨 |

### 공통 컴포넌트 클래스 (globals.css)

```html
<!-- 버튼 -->
<button class="btn-primary">CTA 버튼 (민트)</button>
<button class="btn-secondary">조회·실행 버튼</button>
<button class="btn-outline">보조 버튼</button>
<button class="btn-ghost">텍스트 버튼</button>
<button class="btn-amount">빠른 금액 버튼</button>

<!-- 입력 -->
<input class="input" />

<!-- 카드 -->
<div class="card">테두리 카드</div>
<div class="card-bordered">패딩 포함 카드</div>

<!-- 폼 테이블 (KB 핵심 패턴) -->
<div class="form-table">
  <div class="form-table-row">
    <div class="form-table-label">라벨</div>
    <div class="form-table-field">입력 영역</div>
  </div>
</div>

<!-- 안내 박스 -->
<div class="notice-box">일반 안내</div>
<div class="notice-box-yellow">민트 강조 안내</div>
```

---

## 컴포넌트

### 공용 모달

| 컴포넌트 | 경로 | props |
|---------|------|-------|
| `CartModal` | `components/products/CartModal.tsx` | `productName: string`, `onClose: () => void` |
| `ConsultModal` | `components/layout/ConsultModal.tsx` | `onClose: () => void` |
| `RateModal` | `components/products/RateModal.tsx` | `productName`, `rates`, `rateDate`, `onClose` |

### 사이드바

| 컴포넌트 | 경로 | 특징 |
|---------|------|------|
| `DepositSidebar` | `components/products/DepositSidebar.tsx` | `usePathname` 기반 자동 active, 아코디언 |
| `LoanSidebar` | `components/inquiry/LoanSidebar.tsx` | `usePathname` 기반 자동 active, 아코디언 |

> 두 사이드바 모두 `props` 없이 사용합니다. 현재 경로를 자동으로 감지하여 해당 섹션을 열고 active 상태를 표시합니다.

```tsx
// 사용 예시
import DepositSidebar from '@/components/products/DepositSidebar'
import LoanSidebar from '@/components/inquiry/LoanSidebar'

<DepositSidebar />
<LoanSidebar />
```

### Header 동작 규칙

경로에 따라 Header가 다르게 동작합니다.

| 경로 prefix | 타이틀 | GNB |
|------------|--------|-----|
| `/` (기본) | 개인 | 민트 GNB 표시 |
| `/support` | 고객센터 | GNB 숨김 |
| `/cert` | 인증센터(개인) | GNB 숨김 |
| `/cert-biz` | 인증센터(기업) | GNB 숨김 |
| `/login` | — | 전체 숨김 |

---

## 개발 규칙

### 브랜드 규칙

- **"KB국민은행"** 문구는 절대 사용하지 않습니다.
- 브랜드명은 **AX풀뱅크 / AXFULL BANK** 로 통일합니다.
- 뱅킹 앱 이름은 **AXful 스타뱅킹** 입니다.
- 전화번호: `1588-9999` (대표), `1800-9500` (신규상담)

### 컬러 규칙

- 새 컬러를 추가할 때는 임의 hex(`bg-[#xxx]`) 대신 `tailwind.config.ts`에 토큰을 먼저 등록합니다.
- 메인 CTA / active 상태는 반드시 `kb-yellow`(`#5BC9A8`) 사용합니다.
- 에러·경고는 `kb-red`, 링크는 `kb-blue` 사용합니다.

### 사이드바 규칙

- 새 섹션 페이지를 추가할 때 `LoanSidebar` 또는 `DepositSidebar`의 `NAV` 배열에 경로를 등록합니다.
- `href: '#'`은 미구현 페이지를 의미합니다.

### 모달 규칙

- 장바구니 → `CartModal` 컴포넌트 재사용
- 상담신청 → `ConsultModal` 컴포넌트 재사용
- 인라인 모달 작성 금지 (위 두 경우)
