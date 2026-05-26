# KB Clone — 실제 KB 텍스트 매핑

> 생성일: 2026-05-22  
> 목적: kb-clone 프로젝트에서 실제 KB 국민은행 관련 텍스트를 grep해 리스트업. 퍼블릭 공개 전 대체·삭제 대상 식별용.

---

## 1. "KB" / "국민은행" 브랜드명

| 파일 | 라인 | 내용 |
|------|------|------|
| `app/not-found.tsx` | 13 | `<span>국민은행</span>` — 404 페이지 로고 텍스트 |
| `app/cert-cps/page.tsx` | 33 | "국민은행은 전자서명인증체계를 안전하고 신뢰성 있게 운영하기 위한..." |
| `app/cert-cps/page.tsx` | 36 | `국민은행 최상위 인증기관(KB ROOT CA)` |
| `app/cert-cps/page.tsx` | 44 | `국민은행 인증기관(KB CA)` |
| `app/cert-cps/page.tsx` | 99 | `서울특별시 영등포구 국제금융로8길 26(여의도동) KB국민은행` (실제 주소) |
| `components/layout/Header.tsx` | 21 | `{ label: '국민은행 계좌조회', href: '/inquiry/accounts' }` |
| `app/(personal)/logout/page.tsx` | 32 | `국민은행 kbstar.com을 방문해주셔서 감사합니다.` |
| `app/(personal)/inquiry/accounts/page.tsx` | 13, 125, 131 | "국민은행 계좌조회" (복수) |
| `app/(personal)/banking/first-visit/page.tsx` | 183, 236, 248, 291 | KB국민은행 서비스 안내 문구 |

---

## 2. "kbstar" / "KB Star" — 서비스 브랜드명

| 파일 | 라인 | 내용 |
|------|------|------|
| `app/(personal)/logout/page.tsx` | 32 | `kbstar.com을 방문해주셔서 감사합니다.` |
| `app/(personal)/login/page.tsx` | 197, 470–471 | "KB스타뱅킹 앱" 참조 |
| `app/(personal)/login/page.tsx` | 211, 223, 250, 524 | `KBStarModal` 컴포넌트 및 KB Star 모달 |
| `app/biz/cert/joint-cert-issue/page.tsx` | 72 | `kbstar.com 내용:` |
| `components/home/ProductShowcase.tsx` | 32 | "KB Star 정기예금" |
| `components/home/HeroCarousel.tsx` | 16 | "KB Star 정기예금" |
| `components/layout/Header.tsx` | 426 | "KB Star FX (외환매매플랫폼)" |

---

## 3. 전화번호

| 파일 | 라인 | 번호 | 용도 |
|------|------|------|------|
| `app/not-found.tsx` | 46 | **1588-9999** | 고객센터 |
| `app/biz/layout.tsx` | 14 | **1588-9999** | 대표전화 |
| `app/(personal)/layout.tsx` | 14 | **1588-9999** | 대표전화 |
| `app/(personal)/transfer/confirm/page.tsx` | 210 | **1588-9999** | 보안카드 경고 |
| `components/layout/FloatingSidebar.tsx` | 68 | **1599-9999** | 고객센터 |
| `app/(personal)/personal/page.tsx` | 95 | **1599-4477** | 외국인 상담 |
| `app/cert-cps/layout.tsx` | 41 | **1588-9999**, **1599-9999**, **1644-9999** | 고객센터 (개인·기업·해외) |
| `app/cert-cps/layout.tsx` | 41 | **+82-2-6300-9999** | 해외 전화 |
| `app/(personal)/settings/page.tsx` | 11 | **010-9012-9900** | 목 사용자 전화번호 |
| `app/biz/page.tsx` | 164 | 1599- (partial) | 기업뱅킹서비스 |

---

## 4. 계좌번호

| 파일 | 라인 | 계좌번호 | 컨텍스트 |
|------|------|---------|---------|
| `lib/mock-data.ts` | 29 | **897001-00-057616** | KB국민 계좌 (account 객체) |
| `lib/mock-data.ts` | 39 | **467001-04-012345** | 타행 계좌 (account 객체) |
| `lib/mock-data.ts` | 65 | **897001-00-111111** | 홍길동 최근계좌 |

---

## 5. 도메인 / 이메일

| 파일 | 라인 | 값 | 용도 |
|------|------|----|------|
| `app/(personal)/logout/page.tsx` | 32 | **kbstar.com** | 로그아웃 안내 문구 |
| `app/biz/cert/joint-cert-issue/page.tsx` | 72 | **kbstar.com** | 콘텐츠 참조 |
| `app/api/auth/login/route.ts` | 4 | **test@kb.com** | 목 사용자 이메일 |
| `app/api/auth/cert-login/route.ts` | 4 | **test@kb.com** | 목 인증 이메일 |
| `app/(personal)/settings/page.tsx` | 12 | **test@kb.com** | 목 프로필 이메일 |

> `kbcard.com`, `kb.co.kr` 패턴은 프로젝트 내 미사용 확인됨.

---

## 6. 한국인 이름 (목 데이터)

| 파일 | 라인 | 이름 | 용도 |
|------|------|------|------|
| `lib/mock-data.ts` | 54 | **홍길동** | 거래 발신인 |
| `lib/mock-data.ts` | 64 | **문수현** | 최근 계좌 (실제 사용자명) |
| `lib/mock-data.ts` | 65 | **홍길동** | 이체 수취인 |
| `app/api/auth/login/route.ts` | 4 | **홍길동** | 목 로그인 사용자 |
| `app/api/auth/cert-login/route.ts` | 4 | **홍길동** | 목 인증서 사용자 |
| `app/(personal)/login/page.tsx` | 266 | **홍길동** | 공동인증서 사용자 표시 |
| `app/(personal)/settings/page.tsx` | 10 | **홍길동** | 목 프로필 이름 |

---

## 7. 디자인 토큰 (tailwind.config.ts)

KB 브랜드 컬러 토큰 — `tailwind.config.ts` 라인 12–89 범위에 정의됨.

| 토큰 | 값 | 비고 |
|------|----|------|
| `kb-yellow` | `#5BC9A8` | 메인 민트 |
| `kb-yellow-dark` | `#3FA889` | 다크 민트 |
| `kb-yellow-light` | `#A8E5D4` | 라이트 민트 |
| `kb-taupe` | `#5A7569` | 슬레이트 그린 |
| `kb-taupe-dark` | `#3D4F47` | — |
| `kb-beige` | `#F5F0E8` | 베이지 배경 |
| `kb-beige-light` | `#FAFAF7` | — |
| `kb-gnb-personal` | `#5BC9A8` | 개인 GNB |
| `kb-text` | `#1A1A1A` | 기본 텍스트 |
| `kb-text-body` | `#333333` | 본문 텍스트 |
| `kb-text-muted` | `#777777` | 비활성 텍스트 |
| `kb-border` | `#C5D5CD` | 보더 |
| `kb-red` | `#D0021B` | 에러/경고 |
| `kb-blue` | `#0066CC` | 링크 |
| `kb-green` | `#2E8B6F` | 성공 |

> 디자인 토큰은 클래스명(`kb-*`) 자체가 브랜드 의존적이므로, 범용 이름으로 리네이밍이 필요한 경우 전체 코드베이스 치환 필요.

---

## 요약 — 대체 우선순위

| 우선순위 | 항목 | 파일 수 | 비고 |
|---------|------|--------|------|
| 🔴 높음 | 실제 전화번호 (1588-9999 등) | 6 | 실존 KB 고객센터 번호 |
| 🔴 높음 | 실제 주소 (`cert-cps/page.tsx:99`) | 1 | KB여의도 사옥 주소 |
| 🟠 중간 | 국민은행·kbstar.com 브랜드 문구 | 10+ | 퍼블릭 공개 시 상표 이슈 |
| 🟠 중간 | 계좌번호 목 데이터 | 3 | 가상 번호이지만 형식 동일 |
| 🟡 낮음 | 홍길동 / 이름 목 데이터 | 7 | 일반 placeholder, 무해 |
| 🟡 낮음 | `kb-*` 디자인 토큰 | tailwind 전역 | 기능에 영향 없음, 리네이밍 선택사항 |
