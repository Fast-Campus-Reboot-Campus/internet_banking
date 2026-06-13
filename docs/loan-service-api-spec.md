# loan-service API 명세서

여신(대출) 서비스의 전체 REST 엔드포인트 상세 명세. 컨트롤러·DTO·에러코드 소스에서 추출해 정리한다.

> 형식 참고: [deposit-payment-api-spec.md](deposit-payment-api-spec.md)
> 엔드포인트 전체 목록은 [api-spec.md](api-spec.md) 참조.

---

## 공통 사항

### 인증·인가

loan-service 는 **API Gateway 가 JWT 를 검증**한 뒤 주입하는 헤더를 신뢰한다. 서비스가 JWT 를 직접 파싱하지 않는다(로컬 개발 시 `jwt.secret` 설정 시에만 폴백 파싱).

| 헤더 | 설명 | 비고 |
|---|---|---|
| `X-User-Id` | 사용자(customerId) 숫자 | Gateway 주입 |
| `X-User-Role` | 역할(콤마구분 멀티롤) | 예: `ROLE_CUSTOMER` |
| `X-User-Branch` | 직원 소속 지점코드 | 직원 스코프 판정 |
| `X-User-Grade` | 직원 직급코드 | 직원 스코프 판정 |
| `X-Internal-Token` | 서비스 간 호출 토큰 | `/api/internal/**` → `ROLE_INTERNAL` |
| `Idempotency-Key` | 멱등 키 | 신청 등 일부 POST |

#### 역할(LoanRole)

| 역할 | Spring Authority | 설명 |
|---|---|---|
| CUSTOMER | `ROLE_CUSTOMER` | 고객 |
| TELLER | `ROLE_TELLER` | 창구 직원 |
| DEPUTY_MANAGER | `ROLE_DEPUTY_MANAGER` | 부지점장(심사 실행·확정) |
| BRANCH_MANAGER | `ROLE_BRANCH_MANAGER` | 지점장(최종 결재·정정·상신) |
| HQ_REVIEWER | `ROLE_HQ_REVIEWER` | 본사 심사역(편향 우회·상신 처리) |
| COMPLIANCE | `ROLE_COMPLIANCE` | 준법감시(감사로그) |
| OPS | `ROLE_OPS` | 운영팀(배치·자동심사) |
| INTERNAL | `ROLE_INTERNAL` | 서비스 간 호출 |
| ADMIN | `ROLE_ADMIN` | 관리자 |

#### 공개(비로그인) 엔드포인트
- `GET /api/loan-products`, `GET /api/loan-products/*`, `GET /api/loan-products/*/preferential-rate-policies`
- `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/{health,info,prometheus}`

### 응답 envelope

모든 응답은 공통 `ApiResponse<T>` 로 감싼다.

```json
{ "code": "OK", "message": "OK", "data": { } }
```

- 성공: `code = "OK"`
- 실패: `code` 에 오류코드, `message` 에 사유, `data` 는 `null`(검증 오류 시 `{ "errors": [{ "field", "message" }] }`)

### 공통 에러코드 (CommonErrorCode)

| 코드 | HTTP | 설명 |
|---|---|---|
| `COMMON_400` | 400 | 잘못된 요청 / 검증 실패 / 필수 헤더 누락 |
| `COMMON_401` | 401 | 인증 필요 |
| `COMMON_403` | 403 | 접근 권한 없음 |
| `COMMON_404` | 404 | 리소스 없음 |
| `COMMON_409` | 409 | 리소스 충돌 |
| `COMMON_422` | 422 | 처리 불가 |
| `COMMON_500` | 500 | 서버 오류 |

### 도메인 에러코드 (LoanErrorCode)

`LOAN_<NNN>` 규칙. 구간별 의미: 상품(001–009) / 신청(010–029) / 심사·평가(030–049) / 담보·LTV·서류(050–059) / 약정·실행(060–079) / 상환(080–099) / 연체(100–109) / 금리·종결·만기(110–131) / 증명서·신용신고(140–153) / 캘린더(160–163) / 보증·보증보험(170–187) / 알림·편향·승인(190–207) / RAG(210–211).

전체 코드표는 소스 [`LoanErrorCode.java`](../services/loan-service/src/main/java/com/bank/loan/support/LoanErrorCode.java) 참조. 주요 코드:

| 코드 | HTTP | 설명 |
|---|---|---|
| `LOAN_001` | 409 | 이미 존재하는 상품 코드입니다. |
| `LOAN_002` | 404 | 상품을 찾을 수 없습니다. |
| `LOAN_003` | 400 | 상품 금리/금액/기간 범위가 유효하지 않습니다. |
| `LOAN_004` | 409 | 이미 단종된 상품입니다. |
| `LOAN_005` | 409 | 동일 상품/조건의 활성 우대금리 정책이 이미 존재합니다. |
| `LOAN_010` | 400 | 판매 중인 상품이 아닙니다. |
| `LOAN_011` | 400 | 요청 금액 또는 기간이 상품 범위를 벗어났습니다. |
| `LOAN_012` | 404 | 대출 신청을 찾을 수 없습니다. |
| `LOAN_013` | 409 | 현재 상태에서는 신청을 취소할 수 없습니다. |
| `LOAN_020` | 400 | 본인확인에 실패했습니다. |
| `LOAN_021` | 404 | 본인확인 내역을 찾을 수 없습니다. |
| `LOAN_029` | 503 | 외부 신용평가 엔진 일시 장애로 가심사를 수행할 수 없습니다. 잠시 후 다시 시도해 주세요. |
| `LOAN_030` | 404 | 신용조회 동의 내역을 찾을 수 없습니다. |
| `LOAN_031` | 409 | 이미 철회된 동의입니다. |
| `LOAN_032` | 422 | 신용평가 사전조건이 충족되지 않았습니다. (가심사 PASS 필요) |
| `LOAN_033` | 409 | 이미 신용평가가 수행되었습니다. (신청당 1건) |
| `LOAN_034` | 404 | 신용평가 내역을 찾을 수 없습니다. |
| `LOAN_035` | 422 | DSR 산정 사전조건이 충족되지 않았습니다. (신용평가 완료 필요) |
| `LOAN_036` | 409 | 이미 DSR 산정이 수행되었습니다. (신청당 1건) |
| `LOAN_037` | 404 | DSR 산정 내역을 찾을 수 없습니다. |
| `LOAN_038` | 422 | 본심사 사전조건이 충족되지 않았습니다. (PRESCREENED + CB(APPROVE/REVIEW) + DSR PASS 필요) |
| `LOAN_039` | 409 | 이미 본심사가 수행되었습니다. (신청당 1건) |
| `LOAN_040` | 400 | 서류 업로드에 실패했습니다. |
| `LOAN_041` | 404 | 서류를 찾을 수 없습니다. |
| `LOAN_042` | 404 | 본심사 내역을 찾을 수 없습니다. |
| `LOAN_043` | 400 | 수동 체크 항목 코드가 유효하지 않습니다. (자동 적재 항목은 직접 추가 불가) |
| `LOAN_044` | 422 | 본심사 정정 가능 상태가 아닙니다. (신청 APPROVED/REJECTED 필요, 약정 진입 후 불가) |
| `LOAN_045` | 404 | 가심사 내역을 찾을 수 없습니다. |
| `LOAN_046` | 409 | 이미 가심사가 수행되었습니다. (신청당 1건) |
| `LOAN_047` | 422 | 현재 상태에서는 가심사를 수행할 수 없습니다. (SUBMITTED 필요) |
| `LOAN_048` | 422 | 본심사 자동 결정 불가. CB 결정이 REVIEW 인 경우 수동 본심사가 필요합니다. |
| `LOAN_049` | 422 | 본심사가 권고(PENDING_APPROVAL) 상태가 아닙니다. (확정 불가) |
| `LOAN_050` | 404 | 담보를 찾을 수 없습니다. |
| `LOAN_051` | 409 | 이미 해제된 담보입니다. |
| `LOAN_052` | 422 | LTV 산정 사전조건이 충족되지 않았습니다. (담보 감정평가 DONE 필요 또는 담보 상태 위반) |
| `LOAN_053` | 409 | 이미 LTV 산정이 수행되었습니다. (담보당 1건) |
| `LOAN_054` | 404 | LTV 산정 내역을 찾을 수 없습니다. |
| `LOAN_055` | 422 | 서류 검증이 완료되지 않았습니다. NEEDS_RESUBMIT 또는 HOLD 상태의 서류가 남아있습니다. |
| `LOAN_056` | 503 | 서류 검증 서비스(doc-agent) 일시 장애입니다. 잠시 후 다시 시도해 주세요. |
| `LOAN_060` | 422 | 약정 가능한 신청 상태가 아닙니다. (APPROVED 필요) |
| `LOAN_061` | 400 | 약정 조건이 신청 범위를 벗어났습니다. |
| `LOAN_062` | 404 | 대출 계약을 찾을 수 없습니다. |
| `LOAN_063` | 422 | 현재 계약 상태에서는 자금 인출이 불가합니다. |
| `LOAN_064` | 400 | 약정한도를 초과하여 인출할 수 없습니다. |
| `LOAN_080` | 404 | 상환계좌를 찾을 수 없습니다. |
| `LOAN_081` | 409 | 이미 등록된 상환계좌입니다. (계약당 1건) |
| `LOAN_082` | 422 | 현재 상태에서는 상환계좌를 검증할 수 없습니다. (REGISTERED 필요) |
| `LOAN_083` | 422 | 상환계좌가 검증되지 않았습니다. (drawdown 사전조건) |
| `LOAN_084` | 422 | 지원하지 않는 상환방식입니다. (현재 EQUAL 만 지원) |
| `LOAN_090` | 404 | 상환 회차를 찾을 수 없습니다. |
| `LOAN_091` | 409 | 이미 납부되었거나 상환 가능한 상태가 아닌 회차입니다. |
| `LOAN_092` | 422 | 현재 계약 상태에서는 중도상환이 불가합니다. (ACTIVE 필요) |
| `LOAN_093` | 400 | 중도상환 금액이 유효하지 않습니다. (1 이상) |
| `LOAN_094` | 422 | 중도상환 금액이 잔여 원금을 초과합니다. |
| `LOAN_095` | 404 | 상환 거래를 찾을 수 없습니다. |
| `LOAN_096` | 422 | 역분개 대상 조건을 충족하지 않습니다. (SUCCESS + SCHEDULED 필요) |
| `LOAN_097` | 409 | 이미 역분개된 상환 거래입니다. |
| `LOAN_098` | 422 | 부분상환 금액이 회차 잔액을 초과합니다. |
| `LOAN_099` | 422 | EARLY 역분개 사전조건 미충족. (최신 EARLY 만 지원 + V_new 회차 모두 DUE/OVERDUE 필요) |
| `LOAN_100` | 404 | 활성 연체 정보가 없습니다. |
| `LOAN_110` | 400 | 금리 변경 값이 유효하지 않습니다. |
| `LOAN_120` | 422 | 종결 가능한 계약 상태가 아닙니다. (ACTIVE 필요) |
| `LOAN_121` | 422 | 잔여 원금이 남아있어 정상 종결할 수 없습니다. |
| `LOAN_122` | 422 | 활성 회차(DUE/OVERDUE)가 남아있어 정상 종결할 수 없습니다. |
| `LOAN_123` | 409 | 이미 종결된 계약입니다. |
| `LOAN_124` | 404 | 종결 정보가 없습니다. |
| `LOAN_125` | 422 | 대위변제 사전조건이 충족되지 않았습니다. (활성 보증보험 ISSUED 또는 SIGNED 보증인 1명 이상 필요) |
| `LOAN_126` | 409 | 이미 WRITE_OFF 또는 SUBROGATION 종결된 계약입니다. |
| `LOAN_130` | 404 | 만기 정보를 찾을 수 없습니다. |
| `LOAN_131` | 422 | 현재 상태에서는 만기 연장이 불가합니다. (ACTIVE/MATURED 필요) |
| `LOAN_140` | 404 | 증명서를 찾을 수 없습니다. |
| `LOAN_150` | 404 | 신용정보 신고 내역을 찾을 수 없습니다. |
| `LOAN_151` | 422 | 현재 상태에서는 ACK 처리할 수 없습니다. (SENT 필요) |
| `LOAN_152` | 409 | 이미 ACKED 된 신고는 재전송할 수 없습니다. |
| `LOAN_153` | 400 | ACK 페이로드가 유효하지 않습니다. |
| `LOAN_160` | 404 | 영업일 캘린더 항목을 찾을 수 없습니다. |
| `LOAN_161` | 409 | 이미 등록된 캘린더 일자입니다. |
| `LOAN_162` | 400 | 캘린더 일자 형식이 유효하지 않습니다. (YYYYMMDD) |
| `LOAN_163` | 422 | 지정한 범위 안에서 영업일을 찾지 못했습니다. |
| `LOAN_170` | 404 | 보증 약정을 찾을 수 없습니다. |
| `LOAN_171` | 422 | 현재 상태에서는 서명할 수 없습니다. (REGISTERED 필요) |
| `LOAN_172` | 422 | 현재 상태에서는 취소할 수 없습니다. |
| `LOAN_173` | 422 | 보증 약정 등록 가능한 신청 상태가 아닙니다. (SUBMITTED/PRESCREENED/REVIEWING/APPROVED 필요) |
| `LOAN_174` | 409 | 동일 신청에 동일 보증인의 활성 약정이 이미 존재합니다. |
| `LOAN_175` | 422 | 미서명(REGISTERED) 보증 약정이 남아있어 약정 체결이 불가합니다. |
| `LOAN_176` | 422 | 보증 필수 상품의 활성 SIGNED 보증인이 부족합니다. (실행 사전조건 미충족) |
| `LOAN_180` | 404 | 보증보험 정보를 찾을 수 없습니다. |
| `LOAN_181` | 409 | 이미 발급된 활성 보증보험이 존재합니다. (계약당 1건) |
| `LOAN_182` | 422 | 현재 상태에서는 보증보험을 취소할 수 없습니다. (ISSUED 필요) |
| `LOAN_183` | 422 | 보증보험 발급 가능한 계약 상태가 아닙니다. (SIGNED/ACTIVE 필요) |
| `LOAN_184` | 422 | 보증보험이 등록된 계약은 활성 ISSUED 보증보험이 필요합니다. (drawdown 사전조건) |
| `LOAN_185` | 422 | 대출실행 출금 요청이 실패했습니다. |
| `LOAN_186` | 422 | 역분개 환급 이체 요청이 실패했습니다. |
| `LOAN_187` | 422 | 온라인 상환 결제 요청이 실패했습니다. |
| `LOAN_190` | 404 | 알림 outbox 를 찾을 수 없습니다. |
| `LOAN_191` | 422 | 현재 상태에서는 재전송할 수 없습니다. (FAILED/DEAD 필요) |
| `LOAN_192` | 422 | 본심사가 편향 검증(BIAS_REVIEWING) 상태가 아닙니다. |
| `LOAN_193` | 422 | 편향 검증 리포트가 아직 생성되지 않았습니다. 리포트 생성 후 다시 시도하세요. |
| `LOAN_194` | 422 | 편향 검증 결과가 BLOCKED 입니다. 결정을 정정하거나 상급자 우회 승인을 받으세요. |
| `LOAN_195` | 422 | 본심사가 승인자 대기(PENDING_APPROVER) 상태가 아닙니다. |
| `LOAN_196` | 422 | 승인자와 심사원이 동일합니다. 4-eye 원칙에 따라 다른 사람이 승인해야 합니다. |
| `LOAN_197` | 400 | 결정 변경(override) 시 사유 코드(overrideReasonCd)가 필요합니다. |
| `LOAN_198` | 400 | OVERRIDE_APPROVED 시 승인 금액·금리·기간이 필요합니다. |
| `LOAN_199` | 422 | 본심사가 편향 검증(BIAS_REVIEWING) 상태가 아니어서 편향 우회 승인이 불가합니다. |
| `LOAN_200` | 403 | 심사원 본인이 자신의 편향을 우회 승인할 수 없습니다. 다른 상급자가 승인해야 합니다. |
| `LOAN_201` | 422 | CRITICAL Advisory 리포트를 먼저 확인(ACK)해야 합니다. |
| `LOAN_202` | 403 | 해당 대출 건에 대한 조회 권한이 없습니다. |
| `LOAN_203` | 409 | 이미 본사에 상신된 건입니다. |
| `LOAN_204` | 422 | 현재 상태에서는 본사 상신이 불가합니다. 심사 진행 중인 건만 상신할 수 있습니다. |
| `LOAN_205` | 400 | break-glass 사유는 10자 이상이어야 합니다. |
| `LOAN_206` | 404 | break-glass 대상 건을 찾을 수 없습니다. |
| `LOAN_207` | 403 | 정정(재심사) 행위자를 인증할 수 없거나, 최종 승인자 본인이 자신의 승인 건을 단독 정정할 수 없습니다. 4-eye 원칙에 따라 다른 사람이 정정해야 합니다. |
| `LOAN_210` | 503 | 임베딩 API 호출에 실패했습니다. 잠시 후 재시도하세요. |
| `LOAN_211` | 502 | 임베딩 API 응답이 유효하지 않습니다. (차원 불일치 또는 빈 응답) |

---

## 엔드포인트

### 상품·우대금리

#### LoanProductController

##### `GET` `/api/loan-products`

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `loanTypeCd` | `String` | - |
| `prodStatusCd` | `String` | - |

**응답 data**: `LoanProductListResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `items` | `List<LoanProductListItem>` |  |
| `totalCount` | `long` |  |
| `page` | `int` |  |
| `size` | `int` |  |

##### `POST` `/api/loan-products`

**요청 본문**: `CreateLoanProductRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `prodCd` | `String` | 필수(공백불가), 길이제한 |
| `prodName` | `String` | 필수(공백불가), 길이제한 |
| `loanTypeCd` | `String` | 필수(공백불가), 길이제한 |
| `targetCustomerCd` | `String` | 길이제한 |
| `repaymentMethodCd` | `String` | 필수(공백불가), 길이제한 |
| `rateTypeCd` | `String` | 필수(공백불가), 길이제한 |
| `baseRateBps` | `Integer` | 필수, 최소값 |
| `minRateBps` | `Integer` | 최소값 |
| `maxRateBps` | `Integer` | 최소값 |
| `minAmount` | `Long` | 필수, 최소값 |
| `maxAmount` | `Long` | 필수, 최소값 |
| `minPeriodMo` | `Integer` | 필수, 최소값 |
| `maxPeriodMo` | `Integer` | 필수, 최소값 |
| `collateralRequiredYn` | `String` | 형식제약 |
| `guarantorRequiredYn` | `String` | 형식제약 |
| `minGuarantorCount` | `Integer` | 최소값 |
| `applicationValidityDays` | `Integer` | 최소값 |
| `saleStartDate` | `String` | 형식제약 |
| `saleEndDate` | `String` | 형식제약 |
| `prodTermsUrl` | `String` | 길이제한 |
| `prodTermsHash` | `String` | 길이제한 |
| `productId` | `Long` |  |

**응답 data**: `LoanProductResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `prodId` | `Long` |  |
| `prodCd` | `String` |  |
| `prodName` | `String` |  |
| `loanTypeCd` | `String` |  |
| `targetCustomerCd` | `String` |  |
| `repaymentMethodCd` | `String` |  |
| `rateTypeCd` | `String` |  |
| `baseRateBps` | `Integer` |  |
| `minRateBps` | `Integer` |  |
| `maxRateBps` | `Integer` |  |
| `minAmount` | `Long` |  |
| `maxAmount` | `Long` |  |
| `minPeriodMo` | `Integer` |  |
| `maxPeriodMo` | `Integer` |  |
| `collateralRequiredYn` | `String` |  |
| `guarantorRequiredYn` | `String` |  |
| `minGuarantorCount` | `Integer` |  |
| `applicationValidityDays` | `Integer` |  |
| `saleStartDate` | `String` |  |
| `saleEndDate` | `String` |  |
| `prodStatusCd` | `String` |  |
| `prodTermsUrl` | `String` |  |
| `prodTermsHash` | `String` |  |
| `productId` | `Long` |  |

##### `GET` `/api/loan-products/{prodId}`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `prodId` | `Long` |

**응답 data**: `LoanProductResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `prodId` | `Long` |  |
| `prodCd` | `String` |  |
| `prodName` | `String` |  |
| `loanTypeCd` | `String` |  |
| `targetCustomerCd` | `String` |  |
| `repaymentMethodCd` | `String` |  |
| `rateTypeCd` | `String` |  |
| `baseRateBps` | `Integer` |  |
| `minRateBps` | `Integer` |  |
| `maxRateBps` | `Integer` |  |
| `minAmount` | `Long` |  |
| `maxAmount` | `Long` |  |
| `minPeriodMo` | `Integer` |  |
| `maxPeriodMo` | `Integer` |  |
| `collateralRequiredYn` | `String` |  |
| `guarantorRequiredYn` | `String` |  |
| `minGuarantorCount` | `Integer` |  |
| `applicationValidityDays` | `Integer` |  |
| `saleStartDate` | `String` |  |
| `saleEndDate` | `String` |  |
| `prodStatusCd` | `String` |  |
| `prodTermsUrl` | `String` |  |
| `prodTermsHash` | `String` |  |
| `productId` | `Long` |  |

##### `PATCH` `/api/loan-products/{prodId}`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `prodId` | `Long` |

**요청 본문**: `UpdateLoanProductRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `prodName` | `String` | 길이제한 |
| `loanTypeCd` | `String` | 길이제한 |
| `targetCustomerCd` | `String` | 길이제한 |
| `repaymentMethodCd` | `String` | 길이제한 |
| `rateTypeCd` | `String` | 길이제한 |
| `baseRateBps` | `Integer` | 최소값 |
| `minRateBps` | `Integer` | 최소값 |
| `maxRateBps` | `Integer` | 최소값 |
| `minAmount` | `Long` | 최소값 |
| `maxAmount` | `Long` | 최소값 |
| `minPeriodMo` | `Integer` | 최소값 |
| `maxPeriodMo` | `Integer` | 최소값 |
| `collateralRequiredYn` | `String` | 형식제약 |
| `guarantorRequiredYn` | `String` | 형식제약 |
| `minGuarantorCount` | `Integer` | 최소값 |
| `applicationValidityDays` | `Integer` | 최소값 |
| `saleStartDate` | `String` | 형식제약 |
| `saleEndDate` | `String` | 형식제약 |
| `prodTermsUrl` | `String` | 길이제한 |
| `prodTermsHash` | `String` | 길이제한 |
| `prodStatusCd` | `String` | 길이제한 |

**응답 data**: `LoanProductResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `prodId` | `Long` |  |
| `prodCd` | `String` |  |
| `prodName` | `String` |  |
| `loanTypeCd` | `String` |  |
| `targetCustomerCd` | `String` |  |
| `repaymentMethodCd` | `String` |  |
| `rateTypeCd` | `String` |  |
| `baseRateBps` | `Integer` |  |
| `minRateBps` | `Integer` |  |
| `maxRateBps` | `Integer` |  |
| `minAmount` | `Long` |  |
| `maxAmount` | `Long` |  |
| `minPeriodMo` | `Integer` |  |
| `maxPeriodMo` | `Integer` |  |
| `collateralRequiredYn` | `String` |  |
| `guarantorRequiredYn` | `String` |  |
| `minGuarantorCount` | `Integer` |  |
| `applicationValidityDays` | `Integer` |  |
| `saleStartDate` | `String` |  |
| `saleEndDate` | `String` |  |
| `prodStatusCd` | `String` |  |
| `prodTermsUrl` | `String` |  |
| `prodTermsHash` | `String` |  |
| `productId` | `Long` |  |

##### `POST` `/api/loan-products/{prodId}/discontinue`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `prodId` | `Long` |

**요청 본문**: `DiscontinueLoanProductRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `saleEndDate` | `String` | 필수(공백불가), 형식제약 |
| `reasonCd` | `String` | 필수(공백불가), 길이제한 |
| `reasonRemark` | `String` | 길이제한 |

**응답 data**: `LoanProductResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `prodId` | `Long` |  |
| `prodCd` | `String` |  |
| `prodName` | `String` |  |
| `loanTypeCd` | `String` |  |
| `targetCustomerCd` | `String` |  |
| `repaymentMethodCd` | `String` |  |
| `rateTypeCd` | `String` |  |
| `baseRateBps` | `Integer` |  |
| `minRateBps` | `Integer` |  |
| `maxRateBps` | `Integer` |  |
| `minAmount` | `Long` |  |
| `maxAmount` | `Long` |  |
| `minPeriodMo` | `Integer` |  |
| `maxPeriodMo` | `Integer` |  |
| `collateralRequiredYn` | `String` |  |
| `guarantorRequiredYn` | `String` |  |
| `minGuarantorCount` | `Integer` |  |
| `applicationValidityDays` | `Integer` |  |
| `saleStartDate` | `String` |  |
| `saleEndDate` | `String` |  |
| `prodStatusCd` | `String` |  |
| `prodTermsUrl` | `String` |  |
| `prodTermsHash` | `String` |  |
| `productId` | `Long` |  |

#### PreferentialRatePolicyController

##### `GET` `/api/loan-products/{prodId}/preferential-rate-policies`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `prodId` | `Long` |

**응답 data**: `List<PreferentialRatePolicyResponse>`

| 필드 | 타입 | 제약 |
|---|---|---|
| `policyId` | `Long` |  |
| `prodId` | `Long` |  |
| `policyName` | `String` |  |
| `conditionCd` | `String` |  |
| `preferentialRateBps` | `Integer` |  |
| `maxStackBps` | `Integer` |  |
| `activeYn` | `String` |  |
| `effectiveStartDate` | `String` |  |
| `effectiveEndDate` | `String` |  |
| `policyRemark` | `String` |  |

##### `POST` `/api/loan-products/{prodId}/preferential-rate-policies`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `prodId` | `Long` |

**요청 본문**: `CreatePreferentialRatePolicyRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `policyName` | `String` | 필수(공백불가), 길이제한 |
| `conditionCd` | `String` | 필수(공백불가), 길이제한 |
| `preferentialRateBps` | `Integer` | 필수, 최소값 |
| `maxStackBps` | `Integer` | 최소값 |
| `effectiveStartDate` | `String` | 형식제약 |
| `effectiveEndDate` | `String` | 형식제약 |
| `policyRemark` | `String` | 길이제한 |

**응답 data**: `PreferentialRatePolicyResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `policyId` | `Long` |  |
| `prodId` | `Long` |  |
| `policyName` | `String` |  |
| `conditionCd` | `String` |  |
| `preferentialRateBps` | `Integer` |  |
| `maxStackBps` | `Integer` |  |
| `activeYn` | `String` |  |
| `effectiveStartDate` | `String` |  |
| `effectiveEndDate` | `String` |  |
| `policyRemark` | `String` |  |

### 대출 신청

#### ApplicationExpiryController

##### `POST` `/api/internal/application-expiry/run`

승인 만료 일배치 트리거 (운영자/스케줄러용).

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `baseDate` | `String` | O |

**응답 data**: `ApplicationExpiryRunResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `baseDate` | `String` |  |
| `threshold` | `OffsetDateTime` |  |
| `totalCandidates` | `int` |  |
| `processed` | `int` |  |

#### CreditConsentController

##### `POST` `/api/loan-applications/{applId}/credit-consents`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**요청 본문**: `CreateCreditConsentRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `consentTypeCd` | `String` | 필수(공백불가), 길이제한 |
| `consentScopeCd` | `String` | 필수(공백불가), 길이제한 |
| `consentTargetCd` | `String` | 필수(공백불가), 길이제한 |
| `consentMethodCd` | `String` | 길이제한 |
| `consentToken` | `String` | 길이제한 |
| `signedDocUrl` | `String` | 길이제한 |
| `signedDocHash` | `String` | 길이제한 |
| `retentionUntil` | `String` | 형식제약 |

**응답 data**: `CreditConsentResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `csntId` | `Long` |  |
| `applId` | `Long` |  |
| `customerId` | `Long` |  |
| `consentTypeCd` | `String` |  |
| `consentScopeCd` | `String` |  |
| `consentTargetCd` | `String` |  |
| `consentYn` | `String` |  |
| `consentedAt` | `OffsetDateTime` |  |
| `consentMethodCd` | `String` |  |
| `signedDocUrl` | `String` |  |
| `retentionUntil` | `String` |  |
| `withdrawnYn` | `String` |  |
| `withdrawnAt` | `OffsetDateTime` |  |

#### LoanApplicationController

##### `GET` `/api/loan-applications`

세부 접근 통제는 review 조회의 checkScope 가 담당한다.

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `customerId` | `Long` | - |

**응답 data**: `Map (동적 필드)`

##### `POST` `/api/loan-applications`

ROLE_OPS(관리자): 파라미터로 넘긴 customerId 그대로 사용

**요청 본문**: `CreateLoanApplicationRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `customerId` | `Long` | 필수 |
| `prodId` | `Long` | 필수 |
| `channelCd` | `String` | 필수(공백불가), 길이제한 |
| `requestedAmount` | `Long` | 필수, 최소값 |
| `requestedPeriodMo` | `Integer` | 필수, 최소값 |
| `loanPurposeCd` | `String` | 길이제한 |
| `repaymentMethodCd` | `String` | 길이제한 |
| `estimatedIncomeAmt` | `Long` | 최소값 |
| `employmentTypeCd` | `String` | 길이제한 |

**응답 data**: `LoanApplicationResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `applId` | `Long` |  |
| `applNo` | `String` |  |
| `customerId` | `Long` |  |
| `prodId` | `Long` |  |
| `channelCd` | `String` |  |
| `requestedAmount` | `Long` |  |
| `requestedPeriodMo` | `Integer` |  |
| `loanPurposeCd` | `String` |  |
| `repaymentMethodCd` | `String` |  |
| `estimatedIncomeAmt` | `Long` |  |
| `employmentTypeCd` | `String` |  |
| `applStatusCd` | `String` |  |
| `appliedAt` | `OffsetDateTime` |  |

##### `GET` `/api/loan-applications/{applId}`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**응답 data**: `LoanApplicationResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `applId` | `Long` |  |
| `applNo` | `String` |  |
| `customerId` | `Long` |  |
| `prodId` | `Long` |  |
| `channelCd` | `String` |  |
| `requestedAmount` | `Long` |  |
| `requestedPeriodMo` | `Integer` |  |
| `loanPurposeCd` | `String` |  |
| `repaymentMethodCd` | `String` |  |
| `estimatedIncomeAmt` | `Long` |  |
| `employmentTypeCd` | `String` |  |
| `applStatusCd` | `String` |  |
| `appliedAt` | `OffsetDateTime` |  |

##### `POST` `/api/loan-applications/{applId}/cancel`

요청 바디의 customerId 를 무시하고 JWT principal 로 덮어쓴다

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**요청 본문**: `CancelLoanApplicationRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `cancelReasonCd` | `String` | 필수(공백불가), 길이제한 |
| `cancelRemark` | `String` | 길이제한 |

**응답 data**: `LoanApplicationResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `applId` | `Long` |  |
| `applNo` | `String` |  |
| `customerId` | `Long` |  |
| `prodId` | `Long` |  |
| `channelCd` | `String` |  |
| `requestedAmount` | `Long` |  |
| `requestedPeriodMo` | `Integer` |  |
| `loanPurposeCd` | `String` |  |
| `repaymentMethodCd` | `String` |  |
| `estimatedIncomeAmt` | `Long` |  |
| `employmentTypeCd` | `String` |  |
| `applStatusCd` | `String` |  |
| `appliedAt` | `OffsetDateTime` |  |

#### LoanApplicationJourneyController

##### `GET` `/api/loan-applications/{applId}/journey`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**응답 data**: `LoanApplicationJourneyResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `application` | `LoanApplicationResponse` |  |
| `prescreening` | `LoanPrescreeningResponse` |  |
| `creditEvaluation` | `CreditEvaluationResponse` |  |
| `dsr` | `DsrCalculationResponse` |  |
| `ltv` | `List<LtvCalculationResponse>` |  |
| `review` | `LoanReviewResponse` |  |

#### LoanPrescreeningController

##### `GET` `/api/loan-applications/{applId}/prescreening`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**응답 data**: `LoanPrescreeningResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `prescId` | `Long` |  |
| `applId` | `Long` |  |
| `prescResultCd` | `String` |  |
| `estimatedLimitAmt` | `Long` |  |
| `estimatedRateBps` | `Integer` |  |
| `estimatedGrade` | `String` |  |
| `estimatedScore` | `Integer` |  |
| `rejectReasonCd` | `String` |  |
| `prescRemark` | `String` |  |
| `prescreenedAt` | `OffsetDateTime` |  |
| `prescEngineVersion` | `String` |  |
| `aiTrackCd` | `String` |  |

##### `POST` `/api/loan-applications/{applId}/prescreening`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**요청 본문**: `RunPrescreeningRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `prescResultCd` | `String` | 형식제약 |
| `estimatedLimitAmt` | `Long` | 최소값 |
| `estimatedRateBps` | `Integer` | 최소값 |
| `estimatedGrade` | `String` | 길이제한 |
| `estimatedScore` | `Integer` | 최소값 |
| `rejectReasonCd` | `String` | 길이제한 |
| `prescRemark` | `String` | 길이제한 |
| `prescEngineVersion` | `String` | 길이제한 |

**응답 data**: `LoanPrescreeningResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `prescId` | `Long` |  |
| `applId` | `Long` |  |
| `prescResultCd` | `String` |  |
| `estimatedLimitAmt` | `Long` |  |
| `estimatedRateBps` | `Integer` |  |
| `estimatedGrade` | `String` |  |
| `estimatedScore` | `Integer` |  |
| `rejectReasonCd` | `String` |  |
| `prescRemark` | `String` |  |
| `prescreenedAt` | `OffsetDateTime` |  |
| `prescEngineVersion` | `String` |  |
| `aiTrackCd` | `String` |  |

### 본인확인·신용

#### CreditEvaluationController

##### `GET` `/api/loan-applications/{applId}/credit-evaluation`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**응답 data**: `CreditEvaluationResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `cevalId` | `Long` |  |
| `applId` | `Long` |  |
| `customerId` | `Long` |  |
| `cevalEngine` | `String` |  |
| `cevalEngineVersion` | `String` |  |
| `cevalGrade` | `String` |  |
| `cevalScore` | `Integer` |  |
| `pdBps` | `Integer` |  |
| `cevalDecisionCd` | `String` |  |
| `evalLimitAmount` | `Long` |  |
| `evalRateBps` | `Integer` |  |
| `cevalStatusCd` | `String` |  |
| `cevalFactors` | `String` |  |
| `evaluatedAt` | `OffsetDateTime` |  |

##### `POST` `/api/loan-applications/{applId}/credit-evaluation`

- **인가**: 신용평가 — `ROLE_DEPUTY_MANAGER`/`ROLE_OPS`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**요청 본문**: `RunCreditEvaluationRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `cevalEngine` | `String` | 필수(공백불가), 길이제한 |
| `cevalEngineVersion` | `String` | 길이제한 |
| `cevalGrade` | `String` | 길이제한 |
| `cevalScore` | `Integer` | 최소값 |
| `pdBps` | `Integer` | 최소값 |
| `cevalDecisionCd` | `String` | 필수(공백불가), 형식제약 |
| `evalLimitAmount` | `Long` | 최소값 |
| `evalRateBps` | `Integer` | 최소값 |
| `cevalFactors` | `String` |  |

**응답 data**: `CreditEvaluationResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `cevalId` | `Long` |  |
| `applId` | `Long` |  |
| `customerId` | `Long` |  |
| `cevalEngine` | `String` |  |
| `cevalEngineVersion` | `String` |  |
| `cevalGrade` | `String` |  |
| `cevalScore` | `Integer` |  |
| `pdBps` | `Integer` |  |
| `cevalDecisionCd` | `String` |  |
| `evalLimitAmount` | `Long` |  |
| `evalRateBps` | `Integer` |  |
| `cevalStatusCd` | `String` |  |
| `cevalFactors` | `String` |  |
| `evaluatedAt` | `OffsetDateTime` |  |

#### CreditInfoReportController

##### `GET` `/api/loan-contracts/{cntrId}/credit-info-reports`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**응답 data**: `CreditInfoReportListResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `cntrId` | `Long` |  |
| `totalCount` | `int` |  |
| `items` | `List<CreditInfoReportResponse>` |  |

##### `POST` `/api/loan-contracts/{cntrId}/credit-info-reports`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**요청 본문**: `SubmitReportRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `reportTypeCd` | `String` | 필수(공백불가), 길이제한 |
| `agencyCd` | `String` | 필수(공백불가), 길이제한 |
| `reportTargetCd` | `String` | 필수(공백불가), 길이제한 |
| `reportReasonCd` | `String` | 길이제한 |
| `reportPayload` | `String` |  |

**응답 data**: `CreditInfoReportResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `crptId` | `Long` |  |
| `cntrId` | `Long` |  |
| `dlqId` | `Long` |  |
| `customerId` | `Long` |  |
| `crptTypeCd` | `String` |  |
| `crptAgencyCd` | `String` |  |
| `crptStatusCd` | `String` |  |
| `reportTargetCd` | `String` |  |
| `reportReasonCd` | `String` |  |
| `reportPayload` | `String` |  |
| `externalTxNo` | `String` |  |
| `reportedAt` | `OffsetDateTime` |  |
| `ackAt` | `OffsetDateTime` |  |

#### CreditInfoReportDirectController

##### `GET` `/api/credit-info-reports`

신고 ID 기반 직접 접근. 계약 경로 없이 crptId 단건 조회.

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `statusCd` | `String` | - |

**응답 data**: `AdminCreditInfoReportListResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `items` | `List<CreditInfoReportResponse>` |  |
| `totalCount` | `long` |  |
| `page` | `int` |  |
| `size` | `int` |  |

##### `GET` `/api/credit-info-reports/{crptId}`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `crptId` | `Long` |

**응답 data**: `CreditInfoReportResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `crptId` | `Long` |  |
| `cntrId` | `Long` |  |
| `dlqId` | `Long` |  |
| `customerId` | `Long` |  |
| `crptTypeCd` | `String` |  |
| `crptAgencyCd` | `String` |  |
| `crptStatusCd` | `String` |  |
| `reportTargetCd` | `String` |  |
| `reportReasonCd` | `String` |  |
| `reportPayload` | `String` |  |
| `externalTxNo` | `String` |  |
| `reportedAt` | `OffsetDateTime` |  |
| `ackAt` | `OffsetDateTime` |  |

##### `POST` `/api/credit-info-reports/{crptId}/ack`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `crptId` | `Long` |

**요청 본문**: `AckCallbackRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `externalAckNo` | `String` | 필수(공백불가), 길이제한 |
| `ackedAt` | `OffsetDateTime` | 필수 |

**응답 data**: `CreditInfoReportResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `crptId` | `Long` |  |
| `cntrId` | `Long` |  |
| `dlqId` | `Long` |  |
| `customerId` | `Long` |  |
| `crptTypeCd` | `String` |  |
| `crptAgencyCd` | `String` |  |
| `crptStatusCd` | `String` |  |
| `reportTargetCd` | `String` |  |
| `reportReasonCd` | `String` |  |
| `reportPayload` | `String` |  |
| `externalTxNo` | `String` |  |
| `reportedAt` | `OffsetDateTime` |  |
| `ackAt` | `OffsetDateTime` |  |

##### `POST` `/api/credit-info-reports/{crptId}/retry`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `crptId` | `Long` |

**응답 data**: `CreditInfoReportResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `crptId` | `Long` |  |
| `cntrId` | `Long` |  |
| `dlqId` | `Long` |  |
| `customerId` | `Long` |  |
| `crptTypeCd` | `String` |  |
| `crptAgencyCd` | `String` |  |
| `crptStatusCd` | `String` |  |
| `reportTargetCd` | `String` |  |
| `reportReasonCd` | `String` |  |
| `reportPayload` | `String` |  |
| `externalTxNo` | `String` |  |
| `reportedAt` | `OffsetDateTime` |  |
| `ackAt` | `OffsetDateTime` |  |

#### CreditInfoReportDispatchController

##### `POST` `/api/internal/credit-info-reports/dispatch`

**응답 data**: `CreditInfoReportDispatchSummary`

| 필드 | 타입 | 제약 |
|---|---|---|
| `processed` | `int` |  |
| `sent` | `int` |  |
| `failed` | `int` |  |
| `dead` | `int` |  |

#### CreditScorePreviewController

##### `POST` `/api/credit-score/preview`

**요청 본문**: `CreditScorePreviewRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `customerId` | `Long` | 필수 |
| `loanTypeCd` | `String` | 필수(공백불가), 길이제한 |
| `requestedAmount` | `Long` | 필수, 최소값 |
| `requestedPeriodMo` | `Integer` | 필수, 최소값 |
| `loanPurposeCd` | `String` | 길이제한 |
| `employmentTypeCd` | `String` | 길이제한 |
| `estimatedIncomeAmt` | `Long` | 최소값 |

**응답 data**: `CreditScorePreviewResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `decision` | `String` |  |
| `score` | `Integer` |  |
| `grade` | `String` |  |
| `pdBps` | `Integer` |  |
| `estimatedLimitAmt` | `Long` |  |
| `rejectReasonCd` | `String` |  |
| `engineVersion` | `String` |  |

#### DsrCalculationController

##### `GET` `/api/loan-applications/{applId}/dsr-calculation`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**응답 data**: `DsrCalculationResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `dsrId` | `Long` |  |
| `applId` | `Long` |  |
| `customerId` | `Long` |  |
| `annualIncomeAmt` | `Long` |  |
| `existingPrincipalTotal` | `Long` |  |
| `existingAnnualRepayAmt` | `Long` |  |
| `newAnnualRepayAmt` | `Long` |  |
| `totalAnnualRepayAmt` | `Long` |  |
| `dsrRatioBps` | `Integer` |  |
| `dsrLimitBps` | `Integer` |  |
| `dsrStatusCd` | `String` |  |
| `dsrRegTypeCd` | `String` |  |
| `calculatedAt` | `OffsetDateTime` |  |
| `calcEngineVersion` | `String` |  |
| `dsrDetail` | `String` |  |

##### `POST` `/api/loan-applications/{applId}/dsr-calculation`

- **인가**: DSR 산정 — `ROLE_DEPUTY_MANAGER`/`ROLE_OPS`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**요청 본문**: `RunDsrCalculationRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `annualIncomeAmt` | `Long` | 필수, 최소값 |
| `existingPrincipalTotal` | `Long` | 최소값 |
| `existingAnnualRepayAmt` | `Long` | 최소값 |
| `newAnnualRepayAmt` | `Long` | 최소값 |
| `dsrLimitBps` | `Integer` | 최소값 |
| `dsrRegTypeCd` | `String` | 길이제한 |
| `calcEngineVersion` | `String` | 길이제한 |
| `dsrDetail` | `String` |  |

**응답 data**: `DsrCalculationResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `dsrId` | `Long` |  |
| `applId` | `Long` |  |
| `customerId` | `Long` |  |
| `annualIncomeAmt` | `Long` |  |
| `existingPrincipalTotal` | `Long` |  |
| `existingAnnualRepayAmt` | `Long` |  |
| `newAnnualRepayAmt` | `Long` |  |
| `totalAnnualRepayAmt` | `Long` |  |
| `dsrRatioBps` | `Integer` |  |
| `dsrLimitBps` | `Integer` |  |
| `dsrStatusCd` | `String` |  |
| `dsrRegTypeCd` | `String` |  |
| `calculatedAt` | `OffsetDateTime` |  |
| `calcEngineVersion` | `String` |  |
| `dsrDetail` | `String` |  |

#### LoanIdentityVerificationController

##### `POST` `/api/loan-applications/{applId}/identity-verifications`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**요청 본문**: `VerifyIdentityRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `idvMethodCd` | `String` | 필수(공백불가), 길이제한 |
| `idvTargetCd` | `String` | 필수(공백불가), 길이제한 |
| `mobileNo` | `String` | 필수(공백불가), 형식제약 |

**응답 data**: `IdentityVerificationResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `idvId` | `Long` |  |
| `applId` | `Long` |  |
| `customerId` | `Long` |  |
| `idvMethodCd` | `String` |  |
| `idvStatusCd` | `String` |  |
| `idvResultCd` | `String` |  |
| `idvTargetCd` | `String` |  |
| `ciHash` | `String` |  |
| `diHash` | `String` |  |
| `mobileNoMasked` | `String` |  |
| `verifiedAt` | `OffsetDateTime` |  |
| `externalTxNo` | `String` |  |

##### `GET` `/api/loan-applications/{applId}/identity-verifications/{idvId}`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |
| `idvId` | `Long` |

**응답 data**: `IdentityVerificationResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `idvId` | `Long` |  |
| `applId` | `Long` |  |
| `customerId` | `Long` |  |
| `idvMethodCd` | `String` |  |
| `idvStatusCd` | `String` |  |
| `idvResultCd` | `String` |  |
| `idvTargetCd` | `String` |  |
| `ciHash` | `String` |  |
| `diHash` | `String` |  |
| `mobileNoMasked` | `String` |  |
| `verifiedAt` | `OffsetDateTime` |  |
| `externalTxNo` | `String` |  |

### 심사

#### BiasResultCallbackController

##### `POST` `/api/loans/reviews/{revId}/bias-result`

review-ai-gateway → loan-service 편향 검증 결과 콜백 수신.

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `revId` | `Long` |

**요청 본문**: `BiasResultCallbackRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `status` | `String` |  |
| `analysisType` | `String` |  |
| `findingSummary` | `String` |  |
| `biasDetected` | `boolean` |  |

**응답 data**: `ResponseEntity<Void>`

#### InternalReviewBatchController

##### `POST` `/api/internal/loan-reviews/expire-bias-reviewing`

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `olderThanDays` | `int` | - |

**응답 data**: `ExpireBiasReviewingResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `processed` | `int` |  |
| `expiredRevIds` | `List<Long>` |  |
| `cutoffAt` | `OffsetDateTime` |  |

##### `POST` `/api/internal/loan-reviews/expire-pending`

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `olderThanDays` | `int` | - |

**응답 data**: `ExpirePendingReviewsResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `processed` | `int` |  |
| `expiredRevIds` | `List<Long>` |  |
| `cutoffAt` | `OffsetDateTime` |  |

##### `POST` `/api/internal/loan-reviews/expire-pending-approver`

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `olderThanDays` | `int` | - |

**응답 data**: `ExpirePendingApproverResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `processed` | `int` |  |
| `expiredRevIds` | `List<Long>` |  |
| `cutoffAt` | `OffsetDateTime` |  |

##### `POST` `/api/internal/loan-reviews/{revId}/bias-ops-note`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `revId` | `Long` |

**요청 본문**: `BiasOpsNoteRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `opsStaffId` | `Long` | 필수 |
| `note` | `String` | 필수(공백불가) |

**응답 data**: `AiReviewAdviceResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `adviceId` | `Long` |  |
| `revId` | `Long` |  |
| `adviceTypeCd` | `String` |  |
| `severityCd` | `String` |  |
| `adviceBody` | `String` |  |
| `model` | `String` |  |
| `modelVersion` | `String` |  |
| `latencyMs` | `Integer` |  |
| `createdAt` | `OffsetDateTime` |  |

#### LoanReviewBiasReportController

##### `POST` `/api/internal/loan-reviews/{revId}/bias-report`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `revId` | `Long` |

**요청 본문**: `BiasReportRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `severityCd` | `String` | 필수(공백불가) |
| `summary` | `String` | 필수(공백불가) |
| `findings` | `List<Finding>` |  |
| `model` | `String` |  |
| `modelVersion` | `String` |  |
| `promptHash` | `String` |  |
| `inputToken` | `Integer` |  |
| `outputToken` | `Integer` |  |
| `latencyMs` | `Integer` |  |

**응답 data**: `AiReviewAdviceResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `adviceId` | `Long` |  |
| `revId` | `Long` |  |
| `adviceTypeCd` | `String` |  |
| `severityCd` | `String` |  |
| `adviceBody` | `String` |  |
| `model` | `String` |  |
| `modelVersion` | `String` |  |
| `latencyMs` | `Integer` |  |
| `createdAt` | `OffsetDateTime` |  |

##### `GET` `/api/loan-reviews/{revId}/advices`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `revId` | `Long` |

**응답 data**: `List<AiReviewAdviceResponse>`

| 필드 | 타입 | 제약 |
|---|---|---|
| `adviceId` | `Long` |  |
| `revId` | `Long` |  |
| `adviceTypeCd` | `String` |  |
| `severityCd` | `String` |  |
| `adviceBody` | `String` |  |
| `model` | `String` |  |
| `modelVersion` | `String` |  |
| `latencyMs` | `Integer` |  |
| `createdAt` | `OffsetDateTime` |  |

##### `GET` `/api/loan-reviews/{revId}/advisory-reports`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `revId` | `Long` |

**응답 data**: `List<AdvisoryReportSummary>`

| 필드 | 타입 | 제약 |
|---|---|---|
| `advrId` | `Long` |  |
| `revId` | `Long` |  |
| `advisoryTypeCd` | `String` |  |
| `severityCd` | `String` |  |
| `advrStatusCd` | `String` |  |
| `advrTitle` | `String` |  |
| `advrSummary` | `String` |  |
| `targetReviewerId` | `String` |  |

##### `POST` `/api/loan-reviews/{revId}/bias-override`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `revId` | `Long` |

**요청 본문**: `BiasOverrideRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `overrideReason` | `String` | 필수(공백불가) |

**응답 data**: `LoanReviewResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `revId` | `Long` |  |
| `applId` | `Long` |  |
| `revTypeCd` | `String` |  |
| `revStatusCd` | `String` |  |
| `revDecisionCd` | `String` |  |
| `approvedAmount` | `Long` |  |
| `approvedRateBps` | `Integer` |  |
| `approvedPeriodMo` | `Integer` |  |
| `rejectReasonCd` | `String` |  |
| `revRemark` | `String` |  |
| `reviewerId` | `Long` |  |
| `reviewedAt` | `OffsetDateTime` |  |
| `approvedAt` | `OffsetDateTime` |  |
| `approverId` | `Long` |  |
| `approvedDecisionCd` | `String` |  |
| `overrideReasonCd` | `String` |  |
| `biasSeverityCd` | `String` |  |
| `biasOverrideBy` | `Long` |  |
| `revAiTrackCd` | `String` |  |
| `revAiPd` | `BigDecimal` |  |
| `revAiRationale` | `String` |  |

#### LoanReviewController

##### `GET` `/api/loan-applications/{applId}/review`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**응답 data**: `LoanReviewResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `revId` | `Long` |  |
| `applId` | `Long` |  |
| `revTypeCd` | `String` |  |
| `revStatusCd` | `String` |  |
| `revDecisionCd` | `String` |  |
| `approvedAmount` | `Long` |  |
| `approvedRateBps` | `Integer` |  |
| `approvedPeriodMo` | `Integer` |  |
| `rejectReasonCd` | `String` |  |
| `revRemark` | `String` |  |
| `reviewerId` | `Long` |  |
| `reviewedAt` | `OffsetDateTime` |  |
| `approvedAt` | `OffsetDateTime` |  |
| `approverId` | `Long` |  |
| `approvedDecisionCd` | `String` |  |
| `overrideReasonCd` | `String` |  |
| `biasSeverityCd` | `String` |  |
| `biasOverrideBy` | `Long` |  |
| `revAiTrackCd` | `String` |  |
| `revAiPd` | `BigDecimal` |  |
| `revAiRationale` | `String` |  |

##### `PATCH` `/api/loan-applications/{applId}/review`

- **인가**: 결정 정정 — `ROLE_BRANCH_MANAGER`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**요청 본문**: `ReviseReviewRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `revDecisionCd` | `String` | 필수(공백불가), 형식제약 |
| `approvedAmount` | `Long` | 최소값 |
| `approvedRateBps` | `Integer` | 최소값 |
| `approvedPeriodMo` | `Integer` | 최소값 |
| `rejectReasonCd` | `String` | 길이제한 |
| `revRemark` | `String` | 길이제한 |
| `revisitReasonCd` | `String` | 필수(공백불가), 길이제한 |

**응답 data**: `LoanReviewResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `revId` | `Long` |  |
| `applId` | `Long` |  |
| `revTypeCd` | `String` |  |
| `revStatusCd` | `String` |  |
| `revDecisionCd` | `String` |  |
| `approvedAmount` | `Long` |  |
| `approvedRateBps` | `Integer` |  |
| `approvedPeriodMo` | `Integer` |  |
| `rejectReasonCd` | `String` |  |
| `revRemark` | `String` |  |
| `reviewerId` | `Long` |  |
| `reviewedAt` | `OffsetDateTime` |  |
| `approvedAt` | `OffsetDateTime` |  |
| `approverId` | `Long` |  |
| `approvedDecisionCd` | `String` |  |
| `overrideReasonCd` | `String` |  |
| `biasSeverityCd` | `String` |  |
| `biasOverrideBy` | `Long` |  |
| `revAiTrackCd` | `String` |  |
| `revAiPd` | `BigDecimal` |  |
| `revAiRationale` | `String` |  |

##### `POST` `/api/loan-applications/{applId}/review`

- **인가**: 수동 본심사 실행 — `ROLE_DEPUTY_MANAGER`/`ROLE_OPS`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**요청 본문**: `RunReviewRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `revTypeCd` | `String` | 필수(공백불가), 형식제약 |
| `revDecisionCd` | `String` | 필수(공백불가), 형식제약 |
| `approvedAmount` | `Long` | 최소값 |
| `approvedRateBps` | `Integer` | 최소값 |
| `approvedPeriodMo` | `Integer` | 최소값 |
| `rejectReasonCd` | `String` | 길이제한 |
| `revRemark` | `String` | 길이제한 |

**응답 data**: `LoanReviewResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `revId` | `Long` |  |
| `applId` | `Long` |  |
| `revTypeCd` | `String` |  |
| `revStatusCd` | `String` |  |
| `revDecisionCd` | `String` |  |
| `approvedAmount` | `Long` |  |
| `approvedRateBps` | `Integer` |  |
| `approvedPeriodMo` | `Integer` |  |
| `rejectReasonCd` | `String` |  |
| `revRemark` | `String` |  |
| `reviewerId` | `Long` |  |
| `reviewedAt` | `OffsetDateTime` |  |
| `approvedAt` | `OffsetDateTime` |  |
| `approverId` | `Long` |  |
| `approvedDecisionCd` | `String` |  |
| `overrideReasonCd` | `String` |  |
| `biasSeverityCd` | `String` |  |
| `biasOverrideBy` | `Long` |  |
| `revAiTrackCd` | `String` |  |
| `revAiPd` | `BigDecimal` |  |
| `revAiRationale` | `String` |  |

##### `POST` `/api/loan-applications/{applId}/review/acknowledge-bias`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**요청 본문**: `@RequestBody(required = false) AcknowledgeBiasRequest req`

**응답 data**: `LoanReviewResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `revId` | `Long` |  |
| `applId` | `Long` |  |
| `revTypeCd` | `String` |  |
| `revStatusCd` | `String` |  |
| `revDecisionCd` | `String` |  |
| `approvedAmount` | `Long` |  |
| `approvedRateBps` | `Integer` |  |
| `approvedPeriodMo` | `Integer` |  |
| `rejectReasonCd` | `String` |  |
| `revRemark` | `String` |  |
| `reviewerId` | `Long` |  |
| `reviewedAt` | `OffsetDateTime` |  |
| `approvedAt` | `OffsetDateTime` |  |
| `approverId` | `Long` |  |
| `approvedDecisionCd` | `String` |  |
| `overrideReasonCd` | `String` |  |
| `biasSeverityCd` | `String` |  |
| `biasOverrideBy` | `Long` |  |
| `revAiTrackCd` | `String` |  |
| `revAiPd` | `BigDecimal` |  |
| `revAiRationale` | `String` |  |

##### `POST` `/api/loan-applications/{applId}/review/approver-approve`

- **인가**: 승인자 결재 — `ROLE_BRANCH_MANAGER`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**요청 본문**: `ApproverApproveRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `approverDecisionCd` | `String` | 필수(공백불가) |
| `overrideReasonCd` | `String` |  |
| `overrideRemark` | `String` |  |
| `overrideAmount` | `Long` |  |
| `overrideRateBps` | `Integer` |  |
| `overridePeriodMo` | `Integer` |  |
| `overrideRejectReasonCd` | `String` |  |

**응답 data**: `LoanReviewResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `revId` | `Long` |  |
| `applId` | `Long` |  |
| `revTypeCd` | `String` |  |
| `revStatusCd` | `String` |  |
| `revDecisionCd` | `String` |  |
| `approvedAmount` | `Long` |  |
| `approvedRateBps` | `Integer` |  |
| `approvedPeriodMo` | `Integer` |  |
| `rejectReasonCd` | `String` |  |
| `revRemark` | `String` |  |
| `reviewerId` | `Long` |  |
| `reviewedAt` | `OffsetDateTime` |  |
| `approvedAt` | `OffsetDateTime` |  |
| `approverId` | `Long` |  |
| `approvedDecisionCd` | `String` |  |
| `overrideReasonCd` | `String` |  |
| `biasSeverityCd` | `String` |  |
| `biasOverrideBy` | `Long` |  |
| `revAiTrackCd` | `String` |  |
| `revAiPd` | `BigDecimal` |  |
| `revAiRationale` | `String` |  |

##### `POST` `/api/loan-applications/{applId}/review/auto-decide`

- **인가**: 자동 심사 — `ROLE_OPS`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**응답 data**: `LoanReviewResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `revId` | `Long` |  |
| `applId` | `Long` |  |
| `revTypeCd` | `String` |  |
| `revStatusCd` | `String` |  |
| `revDecisionCd` | `String` |  |
| `approvedAmount` | `Long` |  |
| `approvedRateBps` | `Integer` |  |
| `approvedPeriodMo` | `Integer` |  |
| `rejectReasonCd` | `String` |  |
| `revRemark` | `String` |  |
| `reviewerId` | `Long` |  |
| `reviewedAt` | `OffsetDateTime` |  |
| `approvedAt` | `OffsetDateTime` |  |
| `approverId` | `Long` |  |
| `approvedDecisionCd` | `String` |  |
| `overrideReasonCd` | `String` |  |
| `biasSeverityCd` | `String` |  |
| `biasOverrideBy` | `Long` |  |
| `revAiTrackCd` | `String` |  |
| `revAiPd` | `BigDecimal` |  |
| `revAiRationale` | `String` |  |

##### `POST` `/api/loan-applications/{applId}/review/confirm`

- **인가**: 본심사 확정 — `ROLE_DEPUTY_MANAGER`/`ROLE_OPS`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**요청 본문**: `ConfirmReviewRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `confirmRemark` | `String` | 길이제한 |

**응답 data**: `LoanReviewResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `revId` | `Long` |  |
| `applId` | `Long` |  |
| `revTypeCd` | `String` |  |
| `revStatusCd` | `String` |  |
| `revDecisionCd` | `String` |  |
| `approvedAmount` | `Long` |  |
| `approvedRateBps` | `Integer` |  |
| `approvedPeriodMo` | `Integer` |  |
| `rejectReasonCd` | `String` |  |
| `revRemark` | `String` |  |
| `reviewerId` | `Long` |  |
| `reviewedAt` | `OffsetDateTime` |  |
| `approvedAt` | `OffsetDateTime` |  |
| `approverId` | `Long` |  |
| `approvedDecisionCd` | `String` |  |
| `overrideReasonCd` | `String` |  |
| `biasSeverityCd` | `String` |  |
| `biasOverrideBy` | `Long` |  |
| `revAiTrackCd` | `String` |  |
| `revAiPd` | `BigDecimal` |  |
| `revAiRationale` | `String` |  |

##### `POST` `/api/loan-applications/{applId}/review/escalate-to-hq`

- **인가**: 본사 상신 — `ROLE_BRANCH_MANAGER`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**요청 본문**: `EscalateToHqRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `escalateReason` | `String` | 필수(공백불가), 길이제한 |

**응답 data**: `LoanReviewResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `revId` | `Long` |  |
| `applId` | `Long` |  |
| `revTypeCd` | `String` |  |
| `revStatusCd` | `String` |  |
| `revDecisionCd` | `String` |  |
| `approvedAmount` | `Long` |  |
| `approvedRateBps` | `Integer` |  |
| `approvedPeriodMo` | `Integer` |  |
| `rejectReasonCd` | `String` |  |
| `revRemark` | `String` |  |
| `reviewerId` | `Long` |  |
| `reviewedAt` | `OffsetDateTime` |  |
| `approvedAt` | `OffsetDateTime` |  |
| `approverId` | `Long` |  |
| `approvedDecisionCd` | `String` |  |
| `overrideReasonCd` | `String` |  |
| `biasSeverityCd` | `String` |  |
| `biasOverrideBy` | `Long` |  |
| `revAiTrackCd` | `String` |  |
| `revAiPd` | `BigDecimal` |  |
| `revAiRationale` | `String` |  |

#### PendingReviewController

##### `GET` `/api/loan-reviews/escalated`

**응답 data**: `Page<LoanReviewResponse>`

##### `GET` `/api/loan-reviews/pending`

**응답 data**: `List<LoanReviewResponse>`

| 필드 | 타입 | 제약 |
|---|---|---|
| `revId` | `Long` |  |
| `applId` | `Long` |  |
| `revTypeCd` | `String` |  |
| `revStatusCd` | `String` |  |
| `revDecisionCd` | `String` |  |
| `approvedAmount` | `Long` |  |
| `approvedRateBps` | `Integer` |  |
| `approvedPeriodMo` | `Integer` |  |
| `rejectReasonCd` | `String` |  |
| `revRemark` | `String` |  |
| `reviewerId` | `Long` |  |
| `reviewedAt` | `OffsetDateTime` |  |
| `approvedAt` | `OffsetDateTime` |  |
| `approverId` | `Long` |  |
| `approvedDecisionCd` | `String` |  |
| `overrideReasonCd` | `String` |  |
| `biasSeverityCd` | `String` |  |
| `biasOverrideBy` | `Long` |  |
| `revAiTrackCd` | `String` |  |
| `revAiPd` | `BigDecimal` |  |
| `revAiRationale` | `String` |  |

##### `GET` `/api/loan-reviews/pending-approver`

**응답 data**: `List<LoanReviewResponse>`

| 필드 | 타입 | 제약 |
|---|---|---|
| `revId` | `Long` |  |
| `applId` | `Long` |  |
| `revTypeCd` | `String` |  |
| `revStatusCd` | `String` |  |
| `revDecisionCd` | `String` |  |
| `approvedAmount` | `Long` |  |
| `approvedRateBps` | `Integer` |  |
| `approvedPeriodMo` | `Integer` |  |
| `rejectReasonCd` | `String` |  |
| `revRemark` | `String` |  |
| `reviewerId` | `Long` |  |
| `reviewedAt` | `OffsetDateTime` |  |
| `approvedAt` | `OffsetDateTime` |  |
| `approverId` | `Long` |  |
| `approvedDecisionCd` | `String` |  |
| `overrideReasonCd` | `String` |  |
| `biasSeverityCd` | `String` |  |
| `biasOverrideBy` | `Long` |  |
| `revAiTrackCd` | `String` |  |
| `revAiPd` | `BigDecimal` |  |
| `revAiRationale` | `String` |  |

##### `GET` `/api/loan-reviews/stats`

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `from` | `String` | O |
| `to` | `String` | O |

**응답 data**: `ReviewStatsResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `from` | `String` |  |
| `to` | `String` |  |
| `totalCount` | `long` |  |
| `byTypeDecision` | `Map<String, Long>` |  |
| `byStatus` | `Map<String, Long>` |  |
| `byRejectReason` | `Map<String, Long>` |  |

#### ReviewCheckLogController

##### `GET` `/api/loan-reviews/{revId}/checks`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `revId` | `Long` |

**응답 data**: `List<ReviewCheckLogResponse>`

| 필드 | 타입 | 제약 |
|---|---|---|
| `rchkId` | `Long` |  |
| `revId` | `Long` |  |
| `checkItemCd` | `String` |  |
| `checkResultCd` | `String` |  |
| `checkRemark` | `String` |  |
| `checkerId` | `Long` |  |
| `checkedAt` | `OffsetDateTime` |  |

##### `POST` `/api/loan-reviews/{revId}/checks`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `revId` | `Long` |

**요청 본문**: `@RequestBody @Valid AddReviewCheckLogRequest req`

**응답 data**: `ReviewCheckLogResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `rchkId` | `Long` |  |
| `revId` | `Long` |  |
| `checkItemCd` | `String` |  |
| `checkResultCd` | `String` |  |
| `checkRemark` | `String` |  |
| `checkerId` | `Long` |  |
| `checkedAt` | `OffsetDateTime` |  |

### 담보·LTV·보증

#### CollateralController

##### `GET` `/api/loan-applications/{applId}/collaterals`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**응답 data**: `CollateralListResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `items` | `List<CollateralResponse>` |  |
| `totalCount` | `int` |  |

##### `POST` `/api/loan-applications/{applId}/collaterals`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**요청 본문**: `CreateCollateralRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `colTypeCd` | `String` | 필수(공백불가), 길이제한 |
| `colName` | `String` | 길이제한 |
| `colAddress` | `String` | 길이제한 |
| `colRegistryNo` | `String` | 길이제한 |
| `declaredValue` | `Long` | 최소값 |
| `currencyCd` | `String` | 길이제한 |
| `ownershipTypeCd` | `String` | 길이제한 |
| `seniorLienYn` | `String` | 형식제약 |
| `seniorLienAmount` | `Long` | 최소값 |

**응답 data**: `CollateralResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `colId` | `Long` |  |
| `applId` | `Long` |  |
| `colTypeCd` | `String` |  |
| `colStatusCd` | `String` |  |
| `colNo` | `String` |  |
| `colName` | `String` |  |
| `colAddress` | `String` |  |
| `colRegistryNo` | `String` |  |
| `declaredValue` | `Long` |  |
| `currencyCd` | `String` |  |
| `ownershipTypeCd` | `String` |  |
| `seniorLienYn` | `String` |  |
| `seniorLienAmount` | `Long` |  |

#### CollateralDirectController

##### `PATCH` `/api/collaterals/{colId}`

담보 ID 기반 직접 접근 엔드포인트. 수정·해제 등 신청 경로 없이 colId 로 식별.

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `colId` | `Long` |

**요청 본문**: `UpdateCollateralRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `colTypeCd` | `String` | 길이제한 |
| `colName` | `String` | 길이제한 |
| `colAddress` | `String` | 길이제한 |
| `colRegistryNo` | `String` | 길이제한 |
| `declaredValue` | `Long` | 최소값 |
| `currencyCd` | `String` | 길이제한 |
| `ownershipTypeCd` | `String` | 길이제한 |
| `seniorLienYn` | `String` | 형식제약 |
| `seniorLienAmount` | `Long` | 최소값 |

**응답 data**: `CollateralResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `colId` | `Long` |  |
| `applId` | `Long` |  |
| `colTypeCd` | `String` |  |
| `colStatusCd` | `String` |  |
| `colNo` | `String` |  |
| `colName` | `String` |  |
| `colAddress` | `String` |  |
| `colRegistryNo` | `String` |  |
| `declaredValue` | `Long` |  |
| `currencyCd` | `String` |  |
| `ownershipTypeCd` | `String` |  |
| `seniorLienYn` | `String` |  |
| `seniorLienAmount` | `Long` |  |

##### `POST` `/api/collaterals/{colId}/evaluations`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `colId` | `Long` |

**요청 본문**: `EvaluateCollateralRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `evalMethodCd` | `String` | 필수(공백불가), 길이제한 |
| `evalAgencyCd` | `String` | 길이제한 |
| `appraisedValue` | `Long` | 필수, 최소값 |
| `appliedValue` | `Long` | 최소값 |
| `evalReportUrl` | `String` | 길이제한 |
| `evalReportHash` | `String` | 길이제한 |
| `appliedStartDate` | `String` | 형식제약 |
| `appliedEndDate` | `String` | 형식제약 |

**응답 data**: `CollateralEvaluationResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `cevalColId` | `Long` |  |
| `colId` | `Long` |  |
| `evalMethodCd` | `String` |  |
| `evalAgencyCd` | `String` |  |
| `appraisedValue` | `Long` |  |
| `appliedValue` | `Long` |  |
| `evalStatusCd` | `String` |  |
| `evalReportUrl` | `String` |  |
| `evalReportHash` | `String` |  |
| `evaluatedAt` | `OffsetDateTime` |  |
| `appliedStartDate` | `String` |  |
| `appliedEndDate` | `String` |  |

##### `POST` `/api/collaterals/{colId}/release`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `colId` | `Long` |

**요청 본문**: `ReleaseCollateralRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `releaseReasonCd` | `String` | 필수(공백불가), 길이제한 |
| `releaseDate` | `String` | 형식제약 |
| `releaseRemark` | `String` | 길이제한 |

**응답 data**: `CollateralResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `colId` | `Long` |  |
| `applId` | `Long` |  |
| `colTypeCd` | `String` |  |
| `colStatusCd` | `String` |  |
| `colNo` | `String` |  |
| `colName` | `String` |  |
| `colAddress` | `String` |  |
| `colRegistryNo` | `String` |  |
| `declaredValue` | `Long` |  |
| `currencyCd` | `String` |  |
| `ownershipTypeCd` | `String` |  |
| `seniorLienYn` | `String` |  |
| `seniorLienAmount` | `Long` |  |

#### GuaranteeInsuranceController

##### `POST` `/api/loan-contracts/{cntrId}/guarantee-insurance`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**요청 본문**: `IssueGuaranteeInsuranceRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `ginsAgencyCd` | `String` | 필수(공백불가), 길이제한 |
| `guaranteeAmount` | `Long` | 필수, 최소값 |
| `guaranteeRatioBps` | `Integer` | 필수, 최소값, 최대값 |
| `premiumAmount` | `Long` | 필수, 최소값 |
| `ginsStartDate` | `String` | 형식제약 |
| `ginsEndDate` | `String` | 형식제약 |
| `ginsDocUrl` | `String` | 길이제한 |
| `ginsDocHash` | `String` | 길이제한 |

**응답 data**: `GuaranteeInsuranceResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `ginsId` | `Long` |  |
| `cntrId` | `Long` |  |
| `ginsAgencyCd` | `String` |  |
| `ginsPolicyNo` | `String` |  |
| `guaranteeAmount` | `Long` |  |
| `guaranteeRatioBps` | `Integer` |  |
| `premiumAmount` | `Long` |  |
| `ginsStatusCd` | `String` |  |
| `ginsStartDate` | `String` |  |
| `ginsEndDate` | `String` |  |
| `ginsDocUrl` | `String` |  |
| `ginsDocHash` | `String` |  |
| `issuedAt` | `OffsetDateTime` |  |

##### `GET` `/api/loan-contracts/{cntrId}/guarantee-insurance/{ginsId}`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |
| `ginsId` | `Long` |

**응답 data**: `GuaranteeInsuranceResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `ginsId` | `Long` |  |
| `cntrId` | `Long` |  |
| `ginsAgencyCd` | `String` |  |
| `ginsPolicyNo` | `String` |  |
| `guaranteeAmount` | `Long` |  |
| `guaranteeRatioBps` | `Integer` |  |
| `premiumAmount` | `Long` |  |
| `ginsStatusCd` | `String` |  |
| `ginsStartDate` | `String` |  |
| `ginsEndDate` | `String` |  |
| `ginsDocUrl` | `String` |  |
| `ginsDocHash` | `String` |  |
| `issuedAt` | `OffsetDateTime` |  |

##### `POST` `/api/loan-contracts/{cntrId}/guarantee-insurance/{ginsId}/cancel`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |
| `ginsId` | `Long` |

**요청 본문**: `@RequestBody(required = false) @Valid CancelGuaranteeInsuranceRequest req`

**응답 data**: `GuaranteeInsuranceResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `ginsId` | `Long` |  |
| `cntrId` | `Long` |  |
| `ginsAgencyCd` | `String` |  |
| `ginsPolicyNo` | `String` |  |
| `guaranteeAmount` | `Long` |  |
| `guaranteeRatioBps` | `Integer` |  |
| `premiumAmount` | `Long` |  |
| `ginsStatusCd` | `String` |  |
| `ginsStartDate` | `String` |  |
| `ginsEndDate` | `String` |  |
| `ginsDocUrl` | `String` |  |
| `ginsDocHash` | `String` |  |
| `issuedAt` | `OffsetDateTime` |  |

#### GuaranteeInsuranceExpiryController

##### `POST` `/api/internal/guarantee-insurance-expiry/run`

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `baseDate` | `String` | O |

**응답 data**: `GuaranteeInsuranceExpiryRunResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `baseDate` | `String` |  |
| `totalCandidates` | `int` |  |
| `processed` | `int` |  |

#### GuarantorAgreementController

##### `GET` `/api/loan-applications/{applId}/guarantor-agreements`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**응답 data**: `GuarantorAgreementListResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `applId` | `Long` |  |
| `count` | `int` |  |
| `items` | `List<GuarantorAgreementResponse>` |  |

##### `POST` `/api/loan-applications/{applId}/guarantor-agreements`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**요청 본문**: `RegisterGuarantorAgreementRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `guarantorName` | `String` | 필수(공백불가), 길이제한 |
| `guarantorMobileNo` | `String` | 필수(공백불가), 형식제약 |
| `relationTypeCd` | `String` | 길이제한 |
| `gagrTypeCd` | `String` | 필수(공백불가), 형식제약 |
| `guaranteeAmount` | `Long` | 필수, 최소값 |
| `guaranteeRatioBps` | `Integer` |  |

**응답 data**: `GuarantorAgreementResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `gagrId` | `Long` |  |
| `applId` | `Long` |  |
| `gmstId` | `Long` |  |
| `guarantorNameMasked` | `String` |  |
| `mobileNoMasked` | `String` |  |
| `relationTypeCd` | `String` |  |
| `gagrTypeCd` | `String` |  |
| `guaranteeAmount` | `Long` |  |
| `guaranteeRatioBps` | `Integer` |  |
| `gagrStatusCd` | `String` |  |
| `consentedAt` | `OffsetDateTime` |  |
| `signedDocUrl` | `String` |  |
| `signedDocHash` | `String` |  |

##### `POST` `/api/loan-applications/{applId}/guarantor-agreements/{gagrId}/cancel`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |
| `gagrId` | `Long` |

**요청 본문**: `@RequestBody(required = false) @Valid CancelGuarantorAgreementRequest req`

**응답 data**: `GuarantorAgreementResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `gagrId` | `Long` |  |
| `applId` | `Long` |  |
| `gmstId` | `Long` |  |
| `guarantorNameMasked` | `String` |  |
| `mobileNoMasked` | `String` |  |
| `relationTypeCd` | `String` |  |
| `gagrTypeCd` | `String` |  |
| `guaranteeAmount` | `Long` |  |
| `guaranteeRatioBps` | `Integer` |  |
| `gagrStatusCd` | `String` |  |
| `consentedAt` | `OffsetDateTime` |  |
| `signedDocUrl` | `String` |  |
| `signedDocHash` | `String` |  |

##### `POST` `/api/loan-applications/{applId}/guarantor-agreements/{gagrId}/sign`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |
| `gagrId` | `Long` |

**요청 본문**: `SignGuarantorAgreementRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `signedDocUrl` | `String` | 필수(공백불가), 길이제한 |
| `signedDocHash` | `String` | 필수(공백불가), 길이제한 |

**응답 data**: `GuarantorAgreementResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `gagrId` | `Long` |  |
| `applId` | `Long` |  |
| `gmstId` | `Long` |  |
| `guarantorNameMasked` | `String` |  |
| `mobileNoMasked` | `String` |  |
| `relationTypeCd` | `String` |  |
| `gagrTypeCd` | `String` |  |
| `guaranteeAmount` | `Long` |  |
| `guaranteeRatioBps` | `Integer` |  |
| `gagrStatusCd` | `String` |  |
| `consentedAt` | `OffsetDateTime` |  |
| `signedDocUrl` | `String` |  |
| `signedDocHash` | `String` |  |

#### LtvCalculationController

##### `GET` `/api/collaterals/{colId}/ltv-calculation`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `colId` | `Long` |

**응답 data**: `LtvCalculationResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `ltvId` | `Long` |  |
| `applId` | `Long` |  |
| `colId` | `Long` |  |
| `appliedColValue` | `Long` |  |
| `seniorLienAmount` | `Long` |  |
| `requestedAmount` | `Long` |  |
| `ltvRatioBps` | `Integer` |  |
| `ltvLimitBps` | `Integer` |  |
| `maxLoanAmount` | `Long` |  |
| `ltvStatusCd` | `String` |  |
| `calculatedAt` | `OffsetDateTime` |  |
| `calcEngineVersion` | `String` |  |

##### `POST` `/api/collaterals/{colId}/ltv-calculation`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `colId` | `Long` |

**요청 본문**: `RunLtvCalculationRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `appliedColValue` | `Long` | 최소값 |
| `seniorLienAmount` | `Long` | 최소값 |
| `requestedAmount` | `Long` | 최소값 |
| `ltvLimitBps` | `Integer` | 최소값 |
| `calcEngineVersion` | `String` | 길이제한 |

**응답 data**: `LtvCalculationResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `ltvId` | `Long` |  |
| `applId` | `Long` |  |
| `colId` | `Long` |  |
| `appliedColValue` | `Long` |  |
| `seniorLienAmount` | `Long` |  |
| `requestedAmount` | `Long` |  |
| `ltvRatioBps` | `Integer` |  |
| `ltvLimitBps` | `Integer` |  |
| `maxLoanAmount` | `Long` |  |
| `ltvStatusCd` | `String` |  |
| `calculatedAt` | `OffsetDateTime` |  |
| `calcEngineVersion` | `String` |  |

### 서류

#### LoanDocumentController

##### `GET` `/api/loan-applications/{applId}/documents`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**응답 data**: `LoanDocumentListResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `items` | `List<LoanDocumentResponse>` |  |
| `totalCount` | `int` |  |

##### `POST` `/api/loan-applications/{applId}/documents`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `applId` | `Long` |

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `docTypeCd` | `String` | O |
| `docSourceCd` | `String` | - |

**응답 data**: `LoanDocumentResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `docId` | `Long` |  |
| `applId` | `Long` |  |
| `docTypeCd` | `String` |  |
| `docStatusCd` | `String` |  |
| `docSourceCd` | `String` |  |
| `docName` | `String` |  |
| `docUrl` | `String` |  |
| `docHash` | `String` |  |
| `mimeType` | `String` |  |
| `fileSizeBytes` | `Long` |  |
| `submittedAt` | `OffsetDateTime` |  |
| `verifiedAt` | `OffsetDateTime` |  |
| `verifyResultCd` | `String` |  |
| `retentionUntil` | `String` |  |

#### LoanDocumentDirectController

##### `DELETE` `/api/loan-documents/{docId}`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `docId` | `Long` |

**응답 data**: `LoanDocumentResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `docId` | `Long` |  |
| `applId` | `Long` |  |
| `docTypeCd` | `String` |  |
| `docStatusCd` | `String` |  |
| `docSourceCd` | `String` |  |
| `docName` | `String` |  |
| `docUrl` | `String` |  |
| `docHash` | `String` |  |
| `mimeType` | `String` |  |
| `fileSizeBytes` | `Long` |  |
| `submittedAt` | `OffsetDateTime` |  |
| `verifiedAt` | `OffsetDateTime` |  |
| `verifyResultCd` | `String` |  |
| `retentionUntil` | `String` |  |

### 약정·실행

#### LoanContractAdminController

##### `GET` `/api/admin/loan-contracts`

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `cntrStatusCd` | `String` | - |
| `dateFrom` | `String` | - |
| `dateTo` | `String` | - |
| `page` | `int` | - |
| `size` | `int` | - |

**응답 data**: `Map (동적 필드)`

#### LoanContractController

##### `GET` `/api/loan-contracts`

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `customerId` | `Long` | - |

**응답 data**: `Map (동적 필드)`

##### `POST` `/api/loan-contracts`

ROLE_OPS(관리자): 파라미터로 넘긴 customerId 그대로 사용

**요청 본문**: `CreateContractRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `applId` | `Long` | 필수 |
| `contractedAmount` | `Long` | 필수, 최소값 |
| `contractedPeriodMo` | `Integer` | 필수, 최소값 |
| `baseRateBps` | `Integer` | 필수, 최소값 |
| `spreadBps` | `Integer` | 최소값 |
| `preferentialRateBps` | `Integer` | 최소값 |
| `totalRateBps` | `Integer` | 최소값 |
| `rateTypeCd` | `String` | 필수(공백불가), 길이제한 |
| `repaymentMethodCd` | `String` | 필수(공백불가), 길이제한 |
| `currencyCd` | `String` | 길이제한 |
| `cntrStartDate` | `String` | 형식제약 |
| `cntrEndDate` | `String` | 형식제약 |
| `cntrDocUrl` | `String` | 길이제한 |
| `cntrDocHash` | `String` | 길이제한 |

**응답 data**: `LoanContractResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `cntrId` | `Long` |  |
| `cntrNo` | `String` |  |
| `applId` | `Long` |  |
| `revId` | `Long` |  |
| `customerId` | `Long` |  |
| `prodId` | `Long` |  |
| `contractedAmount` | `Long` |  |
| `currencyCd` | `String` |  |
| `contractedPeriodMo` | `Integer` |  |
| `totalRateBps` | `Integer` |  |
| `baseRateBps` | `Integer` |  |
| `spreadBps` | `Integer` |  |
| `preferentialRateBps` | `Integer` |  |
| `rateTypeCd` | `String` |  |
| `repaymentMethodCd` | `String` |  |
| `cntrStatusCd` | `String` |  |
| `cntrStartDate` | `String` |  |
| `cntrEndDate` | `String` |  |
| `cntrDocUrl` | `String` |  |
| `cntrDocHash` | `String` |  |
| `signedAt` | `OffsetDateTime` |  |

##### `GET` `/api/loan-contracts/{cntrId}`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**응답 data**: `LoanContractResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `cntrId` | `Long` |  |
| `cntrNo` | `String` |  |
| `applId` | `Long` |  |
| `revId` | `Long` |  |
| `customerId` | `Long` |  |
| `prodId` | `Long` |  |
| `contractedAmount` | `Long` |  |
| `currencyCd` | `String` |  |
| `contractedPeriodMo` | `Integer` |  |
| `totalRateBps` | `Integer` |  |
| `baseRateBps` | `Integer` |  |
| `spreadBps` | `Integer` |  |
| `preferentialRateBps` | `Integer` |  |
| `rateTypeCd` | `String` |  |
| `repaymentMethodCd` | `String` |  |
| `cntrStatusCd` | `String` |  |
| `cntrStartDate` | `String` |  |
| `cntrEndDate` | `String` |  |
| `cntrDocUrl` | `String` |  |
| `cntrDocHash` | `String` |  |
| `signedAt` | `OffsetDateTime` |  |

#### LoanExecutionController

##### `POST` `/api/loan-contracts/{cntrId}/executions`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**요청 본문**: `DrawdownRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `executedAmount` | `Long` | 필수, 최소값 |
| `currencyCd` | `String` | 길이제한 |
| `disbursementBankCd` | `String` | 길이제한 |
| `disbursementAccountNo` | `String` | 필수(공백불가) |
| `valueDate` | `String` | 형식제약 |
| `feeAmount` | `Long` | 최소값 |

**응답 data**: `LoanExecutionResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `execId` | `Long` |  |
| `cntrId` | `Long` |  |
| `executedAmount` | `Long` |  |
| `cumulativeExecutedAmount` | `Long` |  |
| `currencyCd` | `String` |  |
| `execStatusCd` | `String` |  |
| `disbursementBankCd` | `String` |  |
| `disbursementAccountMasked` | `String` |  |
| `executedAt` | `OffsetDateTime` |  |
| `valueDate` | `String` |  |
| `feeAmount` | `Long` |  |
| `journalEntryNo` | `String` |  |

#### RepaymentAccountController

##### `GET` `/api/loan-contracts/{cntrId}/repayment-account`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**응답 data**: `RepaymentAccountResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `racctId` | `Long` |  |
| `cntrId` | `Long` |  |
| `accountId` | `Long` |  |
| `bankCd` | `String` |  |
| `accountNoMasked` | `String` |  |
| `holderNameMasked` | `String` |  |
| `racctStatusCd` | `String` |  |
| `autoDebitYn` | `String` |  |
| `debitDay` | `Integer` |  |
| `verifiedAt` | `OffsetDateTime` |  |

##### `POST` `/api/loan-contracts/{cntrId}/repayment-account`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**요청 본문**: `RegisterRepaymentAccountRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `bankCd` | `String` | 필수(공백불가), 길이제한 |
| `accountNo` | `String` | 필수(공백불가), 길이제한 |
| `holderName` | `String` | 길이제한 |
| `accountId` | `Long` |  |
| `autoDebitYn` | `String` | 형식제약 |
| `debitDay` | `Integer` | 최소값, 최대값 |

**응답 data**: `RepaymentAccountResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `racctId` | `Long` |  |
| `cntrId` | `Long` |  |
| `accountId` | `Long` |  |
| `bankCd` | `String` |  |
| `accountNoMasked` | `String` |  |
| `holderNameMasked` | `String` |  |
| `racctStatusCd` | `String` |  |
| `autoDebitYn` | `String` |  |
| `debitDay` | `Integer` |  |
| `verifiedAt` | `OffsetDateTime` |  |

##### `POST` `/api/loan-contracts/{cntrId}/repayment-account/verify`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**요청 본문**: `VerifyRepaymentAccountRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `verifyChannelCd` | `String` | 길이제한 |
| `verifyRemark` | `String` | 길이제한 |

**응답 data**: `RepaymentAccountResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `racctId` | `Long` |  |
| `cntrId` | `Long` |  |
| `accountId` | `Long` |  |
| `bankCd` | `String` |  |
| `accountNoMasked` | `String` |  |
| `holderNameMasked` | `String` |  |
| `racctStatusCd` | `String` |  |
| `autoDebitYn` | `String` |  |
| `debitDay` | `Integer` |  |
| `verifiedAt` | `OffsetDateTime` |  |

#### VirtualAccountController

##### `POST` `/api/loan-contracts/{cntrId}/virtual-account`

대출 상환용 가상계좌.

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**응답 data**: `VirtualAccountResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `accountNo` | `String` |  |
| `bankCode` | `String` |  |
| `accountNickname` | `String` |  |

### 상환·중도상환·부분상환·역분개

#### PartialRepaymentController

##### `POST` `/api/loan-contracts/{cntrId}/repayments/partial`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**요청 본문**: `PartialRepayRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `installmentNo` | `Integer` | 필수 |
| `amount` | `Long` | 필수, 최소값 |
| `channelCd` | `String` | 길이제한 |
| `valueDate` | `String` | 형식제약 |

**응답 data**: `PartialRepaymentResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `rtxId` | `Long` |  |
| `rschId` | `Long` |  |
| `installmentNo` | `Integer` |  |
| `paidAmount` | `Long` |  |
| `principalPortion` | `Long` |  |
| `interestPortion` | `Long` |  |
| `cumulativePaid` | `Long` |  |
| `scheduledTotal` | `Long` |  |
| `scheduleStatusAfter` | `String` |  |
| `paidAt` | `OffsetDateTime` |  |

#### PrepaymentController

##### `POST` `/api/loan-contracts/{cntrId}/prepayments`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**요청 본문**: `PrepayRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `amount` | `Long` | 필수, 최소값 |
| `channelCd` | `String` | 길이제한 |
| `valueDate` | `String` | 형식제약 |

**응답 data**: `PrepaymentResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `rtxId` | `Long` |  |
| `cntrId` | `Long` |  |
| `prepaidPrincipal` | `Long` |  |
| `feeAmount` | `Long` |  |
| `totalAmount` | `Long` |  |
| `outstandingAfter` | `Long` |  |
| `supersededInstallments` | `int` |  |
| `newScheduleVersionCd` | `String` |  |
| `newInstallmentCount` | `int` |  |
| `paidAt` | `OffsetDateTime` |  |

#### RepaymentController

##### `GET` `/api/loan-contracts/{cntrId}/repayments`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**응답 data**: `RepaymentTransactionListResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `cntrId` | `Long` |  |
| `totalCount` | `int` |  |
| `totalPaidAmount` | `Long` |  |
| `items` | `List<RepaymentTransactionResponse>` |  |

##### `POST` `/api/loan-contracts/{cntrId}/repayments`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**요청 본문**: `RepayInstallmentRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `installmentNo` | `Integer` | 필수, 최소값 |
| `channelCd` | `String` | 길이제한 |
| `valueDate` | `String` | 형식제약 |

**응답 data**: `RepaymentTransactionResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `rtxId` | `Long` |  |
| `cntrId` | `Long` |  |
| `rschId` | `Long` |  |
| `rtxTypeCd` | `String` |  |
| `rtxStatusCd` | `String` |  |
| `totalAmount` | `Long` |  |
| `principalAmount` | `Long` |  |
| `interestAmount` | `Long` |  |
| `overdueInterestAmount` | `Long` |  |
| `feeAmount` | `Long` |  |
| `channelCd` | `String` |  |
| `currencyCd` | `String` |  |
| `paidAt` | `OffsetDateTime` |  |
| `valueDate` | `String` |  |
| `balanceAfter` | `Long` |  |
| `idempotencyKey` | `String` |  |

##### `POST` `/api/loan-contracts/{cntrId}/repayments/online`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**요청 본문**: `RepayInstallmentRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `installmentNo` | `Integer` | 필수, 최소값 |
| `channelCd` | `String` | 길이제한 |
| `valueDate` | `String` | 형식제약 |

**응답 data**: `RepaymentTransactionResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `rtxId` | `Long` |  |
| `cntrId` | `Long` |  |
| `rschId` | `Long` |  |
| `rtxTypeCd` | `String` |  |
| `rtxStatusCd` | `String` |  |
| `totalAmount` | `Long` |  |
| `principalAmount` | `Long` |  |
| `interestAmount` | `Long` |  |
| `overdueInterestAmount` | `Long` |  |
| `feeAmount` | `Long` |  |
| `channelCd` | `String` |  |
| `currencyCd` | `String` |  |
| `paidAt` | `OffsetDateTime` |  |
| `valueDate` | `String` |  |
| `balanceAfter` | `Long` |  |
| `idempotencyKey` | `String` |  |

#### RepaymentScheduleController

##### `GET` `/api/loan-contracts/{cntrId}/repayment-schedules`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `version` | `String` | - |

**응답 data**: `RepaymentScheduleListResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `cntrId` | `Long` |  |
| `rschVersionCd` | `String` |  |
| `totalCount` | `int` |  |
| `totalScheduledPrincipal` | `Long` |  |
| `totalScheduledInterest` | `Long` |  |
| `totalScheduledAmount` | `Long` |  |
| `items` | `List<RepaymentScheduleResponse>` |  |

#### ReversalController

##### `POST` `/api/loan-contracts/{cntrId}/repayments/{rtxId}/reversal`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |
| `rtxId` | `Long` |

**요청 본문**: `@Valid @RequestBody(required = false) ReverseRepaymentRequest req`

**응답 data**: `ReversalResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `reversalRtxId` | `Long` |  |
| `targetRtxId` | `Long` |  |
| `cntrId` | `Long` |  |
| `restoredRschId` | `Long` |  |
| `amount` | `Long` |  |
| `reversedAt` | `OffsetDateTime` |  |
| `supersededVersionCd` | `String` |  |
| `supersededCount` | `Integer` |  |
| `restoredVersionCd` | `String` |  |
| `restoredCount` | `Integer` |  |

### 연체

#### DelinquencyController

##### `GET` `/api/loan-contracts/{cntrId}/delinquency`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**응답 data**: `DelinquencyResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `dlqId` | `Long` |  |
| `cntrId` | `Long` |  |
| `dlqStatusCd` | `String` |  |
| `dlqStartDate` | `String` |  |
| `dlqEndDate` | `String` |  |
| `dlqDays` | `Integer` |  |
| `dlqPrincipalAmt` | `Long` |  |
| `dlqInterestAmt` | `Long` |  |
| `dlqTotalAmt` | `Long` |  |
| `overdueRateBps` | `Integer` |  |
| `dlqStageCd` | `String` |  |
| `resolvedAt` | `OffsetDateTime` |  |

##### `GET` `/api/loan-contracts/{cntrId}/delinquency/snapshots`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**응답 data**: `DelinquencySnapshotListResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `cntrId` | `Long` |  |
| `dlqId` | `Long` |  |
| `totalCount` | `int` |  |
| `items` | `List<DelinquencySnapshotResponse>` |  |

#### DelinquencyRolloverController

##### `POST` `/api/internal/delinquency/rollover`

연체 일배치 트리거 (internal). 보통 매일 새벽 자동이체 직후 호출된다.

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `baseDate` | `String` | O |

**응답 data**: `DelinquencyRolloverResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `baseDate` | `String` |  |
| `newlyOverdueInstallments` | `int` |  |
| `activeDelinquencies` | `int` |  |
| `resolvedDelinquencies` | `int` |  |
| `snapshotsCreated` | `int` |  |

### 금리변경·만기·종결

#### AutoDebitCallbackController

##### `POST` `/api/internal/auto-debit/payment-result`

**요청 본문**: `AutoDebitPaymentResultRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `piId` | `String` | 필수(공백불가) |
| `failureCategory` | `String` |  |

**응답 data**: `RepaymentTransactionResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `rtxId` | `Long` |  |
| `cntrId` | `Long` |  |
| `rschId` | `Long` |  |
| `rtxTypeCd` | `String` |  |
| `rtxStatusCd` | `String` |  |
| `totalAmount` | `Long` |  |
| `principalAmount` | `Long` |  |
| `interestAmount` | `Long` |  |
| `overdueInterestAmount` | `Long` |  |
| `feeAmount` | `Long` |  |
| `channelCd` | `String` |  |
| `currencyCd` | `String` |  |
| `paidAt` | `OffsetDateTime` |  |
| `valueDate` | `String` |  |
| `balanceAfter` | `Long` |  |
| `idempotencyKey` | `String` |  |

#### AutoDebitController

##### `POST` `/api/internal/auto-debit/run`

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `baseDate` | `String` | O |

**응답 data**: `AutoDebitRunResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `baseDate` | `String` |  |
| `totalCandidates` | `int` |  |
| `processed` | `int` |  |
| `skipped` | `int` |  |
| `skipReason` | `String` |  |

#### LoanClosureController

##### `GET` `/api/loan-contracts/{cntrId}/closure`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**응답 data**: `LoanClosureResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `closId` | `Long` |  |
| `cntrId` | `Long` |  |
| `closTypeCd` | `String` |  |
| `closReasonCd` | `String` |  |
| `closStatusCd` | `String` |  |
| `finalPrincipalAmt` | `Long` |  |
| `finalInterestAmt` | `Long` |  |
| `finalFeeAmt` | `Long` |  |
| `prepaymentFeeAmt` | `Long` |  |
| `totalSettledAmt` | `Long` |  |
| `closDate` | `String` |  |
| `closedAt` | `OffsetDateTime` |  |
| `closDocUrl` | `String` |  |
| `closDocHash` | `String` |  |
| `writeOffAmount` | `Long` |  |
| `subrogationAmount` | `Long` |  |
| `subrogationPartyRef` | `String` |  |
| `writeOffReasonCd` | `String` |  |

##### `POST` `/api/loan-contracts/{cntrId}/closure`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**요청 본문**: `CloseLoanRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `closureTypeCd` | `String` | 필수(공백불가), 길이제한 |
| `closureReasonCd` | `String` | 길이제한 |
| `closureDate` | `String` | 형식제약 |
| `finalFeeAmt` | `Long` | 최소값 |
| `prepaymentFeeAmt` | `Long` | 최소값 |
| `closureDocUrl` | `String` | 길이제한 |
| `closureDocHash` | `String` | 길이제한 |
| `subrogationPartyRef` | `String` | 길이제한 |
| `writeOffReasonCd` | `String` | 길이제한 |

**응답 data**: `LoanClosureResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `closId` | `Long` |  |
| `cntrId` | `Long` |  |
| `closTypeCd` | `String` |  |
| `closReasonCd` | `String` |  |
| `closStatusCd` | `String` |  |
| `finalPrincipalAmt` | `Long` |  |
| `finalInterestAmt` | `Long` |  |
| `finalFeeAmt` | `Long` |  |
| `prepaymentFeeAmt` | `Long` |  |
| `totalSettledAmt` | `Long` |  |
| `closDate` | `String` |  |
| `closedAt` | `OffsetDateTime` |  |
| `closDocUrl` | `String` |  |
| `closDocHash` | `String` |  |
| `writeOffAmount` | `Long` |  |
| `subrogationAmount` | `Long` |  |
| `subrogationPartyRef` | `String` |  |
| `writeOffReasonCd` | `String` |  |

#### MaturityBatchController

##### `POST` `/api/internal/maturity/run`

만기 도래 일배치 트리거 (internal).

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `baseDate` | `String` | O |

**응답 data**: `MaturityRunResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `baseDate` | `String` |  |
| `totalCandidates` | `int` |  |
| `processed` | `int` |  |

#### MaturityController

##### `GET` `/api/loan-contracts/{cntrId}/maturity`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**응답 data**: `MaturityResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `matId` | `Long` |  |
| `cntrId` | `Long` |  |
| `originalMaturityDate` | `String` |  |
| `currentMaturityDate` | `String` |  |
| `matStatusCd` | `String` |  |
| `extensionTypeCd` | `String` |  |
| `extensionCount` | `Integer` |  |
| `lastExtendedDate` | `String` |  |
| `extendedPeriodMo` | `Integer` |  |
| `noticeStatusCd` | `String` |  |
| `lastNoticeAt` | `OffsetDateTime` |  |

##### `POST` `/api/loan-contracts/{cntrId}/maturity/extend`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**요청 본문**: `ExtendMaturityRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `extendedPeriodMo` | `Integer` | 필수, 최소값, 최대값 |
| `extensionTypeCd` | `String` | 길이제한 |
| `extensionReason` | `String` | 길이제한 |

**응답 data**: `MaturityResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `matId` | `Long` |  |
| `cntrId` | `Long` |  |
| `originalMaturityDate` | `String` |  |
| `currentMaturityDate` | `String` |  |
| `matStatusCd` | `String` |  |
| `extensionTypeCd` | `String` |  |
| `extensionCount` | `Integer` |  |
| `lastExtendedDate` | `String` |  |
| `extendedPeriodMo` | `Integer` |  |
| `noticeStatusCd` | `String` |  |
| `lastNoticeAt` | `OffsetDateTime` |  |

#### RateChangeController

##### `GET` `/api/loan-contracts/{cntrId}/rate-changes`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**응답 data**: `RateChangeHistoryListResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `cntrId` | `Long` |  |
| `totalCount` | `int` |  |
| `items` | `List<RateChangeHistoryResponse>` |  |

##### `POST` `/api/loan-contracts/{cntrId}/rate-changes`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**요청 본문**: `CreateRateChangeRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `newBaseRateBps` | `Integer` | 필수, 최소값 |
| `newSpreadBps` | `Integer` | 최소값 |
| `newPreferentialRateBps` | `Integer` | 최소값 |
| `newTotalRateBps` | `Integer` | 최소값 |
| `appliedStartDate` | `String` | 필수(공백불가), 형식제약 |
| `rateChangeReasonCd` | `String` | 필수(공백불가), 길이제한 |

**응답 data**: `RateChangeApplyResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `rchgId` | `Long` |  |
| `cntrId` | `Long` |  |
| `previousRateBps` | `Integer` |  |
| `newRateBps` | `Integer` |  |
| `appliedStartDate` | `String` |  |
| `rateChangeReasonCd` | `String` |  |
| `newScheduleVersionCd` | `String` |  |
| `supersededInstallments` | `int` |  |
| `newInstallments` | `int` |  |
| `changedAt` | `OffsetDateTime` |  |

### 증명서·상태이력·알림

#### LoanCertificateController

##### `GET` `/api/loan-contracts/{cntrId}/certificates`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**응답 data**: `LoanCertificateListResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `cntrId` | `Long` |  |
| `totalCount` | `int` |  |
| `items` | `List<LoanCertificateResponse>` |  |

##### `POST` `/api/loan-contracts/{cntrId}/certificates`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**요청 본문**: `IssueCertificateRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `certTypeCd` | `String` | 필수(공백불가), 길이제한 |
| `certPurposeCd` | `String` | 길이제한 |
| `issueChannelCd` | `String` | 길이제한 |
| `certDocUrl` | `String` | 길이제한 |
| `certDocHash` | `String` | 길이제한 |
| `retentionUntil` | `String` | 형식제약 |

**응답 data**: `LoanCertificateResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `certId` | `Long` |  |
| `cntrId` | `Long` |  |
| `customerId` | `Long` |  |
| `certTypeCd` | `String` |  |
| `certNo` | `String` |  |
| `certStatusCd` | `String` |  |
| `certPurposeCd` | `String` |  |
| `certDocUrl` | `String` |  |
| `certDocHash` | `String` |  |
| `issueChannelCd` | `String` |  |
| `issuedAt` | `OffsetDateTime` |  |
| `retentionUntil` | `String` |  |

#### LoanCertificateDirectController

##### `GET` `/api/loan-certificates/{certId}`

증명서 ID 기반 직접 접근. 계약 경로 없이 certId 단건 조회.

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `certId` | `Long` |

**응답 data**: `LoanCertificateResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `certId` | `Long` |  |
| `cntrId` | `Long` |  |
| `customerId` | `Long` |  |
| `certTypeCd` | `String` |  |
| `certNo` | `String` |  |
| `certStatusCd` | `String` |  |
| `certPurposeCd` | `String` |  |
| `certDocUrl` | `String` |  |
| `certDocHash` | `String` |  |
| `issueChannelCd` | `String` |  |
| `issuedAt` | `OffsetDateTime` |  |
| `retentionUntil` | `String` |  |

#### LoanStatusHistoryController

##### `GET` `/api/status-history`

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `targetTable` | `String` | O |
| `targetId` | `Long` | O |

**응답 data**: `StatusHistoryListResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `targetDomainCd` | `String` |  |
| `targetTableCd` | `String` |  |
| `targetId` | `Long` |  |
| `count` | `int` |  |
| `items` | `List<StatusHistoryResponse>` |  |

#### NotificationDispatchController

##### `POST` `/api/internal/notifications/dispatch`

알림 outbox 디스패치 트리거 (internal).

**응답 data**: `NotificationDispatchSummary`

| 필드 | 타입 | 제약 |
|---|---|---|
| `processed` | `int` |  |
| `sent` | `int` |  |
| `failed` | `int` |  |
| `dead` | `int` |  |

#### NotificationOutboxController

##### `GET` `/api/notifications`

운영자용 알림 outbox 조회·재전송 엔드포인트.

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `eventType` | `String` | - |
| `status` | `String` | - |

**응답 data**: `NotificationOutboxListResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `items` | `List<NotificationOutboxListItem>` |  |
| `totalCount` | `long` |  |
| `page` | `int` |  |
| `size` | `int` |  |

##### `GET` `/api/notifications/{outboxId}`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `outboxId` | `Long` |

**응답 data**: `NotificationOutboxResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `outboxId` | `Long` |  |
| `eventTypeCd` | `String` |  |
| `referenceId` | `Long` |  |
| `channelCd` | `String` |  |
| `payload` | `String` |  |
| `status` | `String` |  |
| `attemptNo` | `int` |  |
| `maxAttempt` | `int` |  |
| `nextAttemptAt` | `OffsetDateTime` |  |
| `lastError` | `String` |  |
| `sentAt` | `OffsetDateTime` |  |
| `idempotencyKey` | `String` |  |

##### `POST` `/api/notifications/{outboxId}/retry`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `outboxId` | `Long` |

**응답 data**: `NotificationOutboxResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `outboxId` | `Long` |  |
| `eventTypeCd` | `String` |  |
| `referenceId` | `Long` |  |
| `channelCd` | `String` |  |
| `payload` | `String` |  |
| `status` | `String` |  |
| `attemptNo` | `int` |  |
| `maxAttempt` | `int` |  |
| `nextAttemptAt` | `OffsetDateTime` |  |
| `lastError` | `String` |  |
| `sentAt` | `OffsetDateTime` |  |
| `idempotencyKey` | `String` |  |

### 이자·회계·ECL 배치

#### AccountingSummaryBatchController

##### `POST` `/api/internal/accounting-summary/run`

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `baseDate` | `String` | O |

**응답 data**: `AccountingSummaryRunResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `created` | `boolean` |  |
| `summaryDate` | `String` |  |
| `interestRevenue` | `long` |  |
| `overdueInterestRevenue` | `long` |  |
| `autoDebitTotal` | `long` |  |
| `autoDebitCount` | `int` |  |
| `disbursedAmount` | `long` |  |
| `disbursedCount` | `int` |  |
| `activeContractCount` | `int` |  |
| `activeDelinquencyCount` | `int` |  |

#### EclCalculationBatchController

##### `POST` `/api/internal/ecl/run`

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `baseMonth` | `String` | O |

**응답 data**: `EclCalculationRunResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `baseMonth` | `String` |  |
| `totalCandidates` | `int` |  |
| `processed` | `int` |  |
| `skipped` | `int` |  |
| `totalEcl` | `long` |  |

#### InterestAccrualBatchController

##### `POST` `/api/internal/interest-accrual/run`

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `baseDate` | `String` | O |

**응답 data**: `InterestAccrualRunResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `baseDate` | `String` |  |
| `totalCandidates` | `int` |  |
| `processed` | `int` |  |
| `skipped` | `int` |  |

#### InterestAccrualController

##### `GET` `/api/loan-contracts/{cntrId}/interest-accruals`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `cntrId` | `Long` |

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `from` | `String` | - |
| `to` | `String` | - |

**응답 data**: `InterestAccrualListResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `cntrId` | `Long` |  |
| `totalCount` | `int` |  |
| `sumDailyInterest` | `Long` |  |
| `latestCumulativeInterest` | `Long` |  |
| `items` | `List<InterestAccrualResponse>` |  |

### 배치·캘린더·동기화

#### BusinessCalendarController

##### `GET` `/api/business-calendar`

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `from` | `String` | O |
| `to` | `String` | O |

**응답 data**: `BusinessCalendarListResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `fromDate` | `String` |  |
| `toDate` | `String` |  |
| `count` | `int` |  |
| `items` | `List<BusinessCalendarResponse>` |  |

##### `POST` `/api/business-calendar`

**요청 본문**: `RegisterBusinessCalendarRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `calDate` | `String` | 필수(공백불가), 형식제약 |
| `businessDayYn` | `String` | 필수(공백불가), 형식제약 |
| `holidayTypeCd` | `String` | 길이제한 |
| `holidayName` | `String` | 길이제한 |
| `baseCountryCd` | `String` | 길이제한 |

**응답 data**: `BusinessCalendarResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `calId` | `Long` |  |
| `calDate` | `String` |  |
| `businessDayYn` | `String` |  |
| `holidayTypeCd` | `String` |  |
| `holidayName` | `String` |  |
| `baseCountryCd` | `String` |  |

##### `GET` `/api/business-calendar/by-date`

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `calDate` | `String` | O |

**응답 data**: `BusinessCalendarResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `calId` | `Long` |  |
| `calDate` | `String` |  |
| `businessDayYn` | `String` |  |
| `holidayTypeCd` | `String` |  |
| `holidayName` | `String` |  |
| `baseCountryCd` | `String` |  |

##### `GET` `/api/business-calendar/check`

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `calDate` | `String` | O |

**응답 data**: `BusinessDayCheckResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `calDate` | `String` |  |
| `businessDay` | `boolean` |  |
| `source` | `String` |  |
| `holidayTypeCd` | `String` |  |
| `holidayName` | `String` |  |

##### `DELETE` `/api/business-calendar/{calId}`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `calId` | `Long` |

**응답 data**: `ResponseEntity<Void>`

##### `PUT` `/api/business-calendar/{calId}`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `calId` | `Long` |

**요청 본문**: `UpdateBusinessCalendarRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `businessDayYn` | `String` | 필수(공백불가), 형식제약 |
| `holidayTypeCd` | `String` | 길이제한 |
| `holidayName` | `String` | 길이제한 |
| `baseCountryCd` | `String` | 길이제한 |

**응답 data**: `BusinessCalendarResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `calId` | `Long` |  |
| `calDate` | `String` |  |
| `businessDayYn` | `String` |  |
| `holidayTypeCd` | `String` |  |
| `holidayName` | `String` |  |
| `baseCountryCd` | `String` |  |

#### CalendarSeederController

##### `POST` `/api/internal/calendar-seeder/run`

영업일 캘린더 시드 트리거 (internal).

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `year` | `int` | O |

**응답 data**: `CalendarSeederRunResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `year` | `int` |  |
| `totalDays` | `int` |  |
| `inserted` | `int` |  |
| `skipped` | `int` |  |

#### CommonSyncDispatchController

##### `POST` `/api/internal/common-sync/backfill/contracts`

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `pageSize` | `int` | - |

**응답 data**: `Integer`

##### `POST` `/api/internal/common-sync/backfill/products`

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `pageSize` | `int` | - |

**응답 data**: `Integer`

##### `POST` `/api/internal/common-sync/dispatch`

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `pageSize` | `int` | - |

**응답 data**: `CommonSyncDispatchSummary`

| 필드 | 타입 | 제약 |
|---|---|---|
| `total` | `int` |  |
| `done` | `int` |  |
| `failed` | `int` |  |
| `dead` | `int` |  |

#### EodBatchController

##### `GET` `/api/internal/eod/history`

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `from` | `String` | - |
| `to` | `String` | - |

**응답 data**: `List<EodHistoryResponse>`

| 필드 | 타입 | 제약 |
|---|---|---|
| `jobExecutionId` | `Long` |  |
| `baseDate` | `String` |  |
| `status` | `String` |  |
| `exitCode` | `String` |  |
| `startTime` | `LocalDateTime` |  |
| `endTime` | `LocalDateTime` |  |
| `durationMs` | `Long` |  |
| `steps` | `List<StepInfo>` |  |

##### `POST` `/api/internal/eod/restart`

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `baseDate` | `String` | O |

**응답 data**: `EodRunResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `baseDate` | `String` |  |
| `jobStatus` | `String` |  |
| `jobExecutionId` | `Long` |  |
| `message` | `String` |  |

##### `POST` `/api/internal/eod/run`

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `baseDate` | `String` | O |

**응답 data**: `EodRunResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `baseDate` | `String` |  |
| `jobStatus` | `String` |  |
| `jobExecutionId` | `Long` |  |
| `message` | `String` |  |

#### EomBatchController

##### `POST` `/api/internal/eom/run`

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `baseMonth` | `String` | O |

**응답 data**: `EodRunResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `baseDate` | `String` |  |
| `jobStatus` | `String` |  |
| `jobExecutionId` | `Long` |  |
| `message` | `String` |  |

### 감사·긴급접근

#### AuditLogController

##### `GET` `/api/audit/access-logs`

특정 건의 전체 접근 이력 조회 (COMPLIANCE 전용).

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `targetType` | `String` | O |
| `targetId` | `Long` | O |

**응답 data**: `List<AuditLogEntry>`

##### `GET` `/api/audit/break-glass`

break-glass 이벤트 전체 조회 (COMPLIANCE 전용). actorId 로 필터 가능.

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `actorId` | `Long` | - |

**응답 data**: `List<AuditLogEntry>`

#### BreakGlassController

##### `POST` `/api/break-glass`

- **인가**: 긴급 접근 — 직원 역할(CUSTOMER 제외)

**요청 본문**: `BreakGlassRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `applId` | `Long` |  |
| `reason` | `String` |  |

**응답 data**: `ResponseEntity<BreakGlassResponse>`
