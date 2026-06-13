# customer-service API 명세서

고객·인증·인증서 서비스의 전체 REST 엔드포인트 상세 명세. 컨트롤러·DTO·에러코드 소스에서 추출해 정리한다.

> 형식 참고: [deposit-payment-api-spec.md](deposit-payment-api-spec.md) · [loan-service-api-spec.md](loan-service-api-spec.md)
> 엔드포인트 전체 목록은 [api-spec.md](api-spec.md) 참조.

---

## 공통 사항

### 인증·인가

JWT 검증은 **api-gateway** 가 수행하고, 검증된 사용자 정보를 `X-User-*` 헤더로 전파한다. customer-service 는 게이트웨이 헤더를 SecurityContext 에 올린다(로컬은 `jwt.secret` 설정 시 폴백 파싱).

| 헤더 | 설명 |
|---|---|
| `X-User-Id` | 사용자(customerId) 숫자 |
| `X-User-Role` | 역할(콤마구분 멀티롤) |
| `X-User-Branch` | 직원 소속 지점코드 |
| `X-User-Grade` | 직원 직급코드 |

#### 인가 정책

- **고객 엔드포인트**: 게이트웨이 1차 검증 + 내부망 신뢰로 `permitAll` (로그인·인증·마이페이지 등)
- **직원 전용 `/api/v1/internal/**`**: 직무 그룹(BankRole)으로 게이팅

| 경로 | 허용 직무 그룹 |
|---|---|
| `GET /api/v1/internal/customers/access-logs` | AUDIT_VIEW (COMPLIANCE·HQ_REVIEWER·BRANCH_MANAGER·TELLER·ADMIN) |
| `GET /api/v1/internal/customers/join-stats` | JOIN_STATS (COMPLIANCE·HQ_RISK·ADMIN) |
| `/api/v1/internal/customers/**` | CUSTOMER_VIEW (COMPLIANCE·HQ_REVIEWER·HQ_RISK·BRANCH_MANAGER·DEPUTY_MANAGER·TELLER·ADMIN) |
| `/api/v1/internal/compliance/**`, `/api/v1/internal/party/**` | COMPLIANCE_DESK (COMPLIANCE·HQ_REVIEWER·HQ_RISK·ADMIN) |
| `/api/v1/internal/fds/**` | FDS (COMPLIANCE·HQ_RISK·OPS·ADMIN) |
| `/api/v1/internal/**` (그 외) | 직원 역할(EMPLOYEE_ROLES) |

### 응답 envelope

```json
{ "code": "OK", "message": "OK", "data": { } }
```

- 성공: `code = "OK"` / 실패: `code` 에 오류코드, `data` 는 `null`(검증 오류 시 `{ "errors": [{ "field", "message" }] }`)

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

### 도메인 에러코드 (CustomerErrorCode)

`CUST_<NNN>` 규칙. 회원(001–009)/인증(010–019)/설정(020)/인증서(030–039)/QR(040–049)/출금계좌(050–059)/FDS(060–069)/등록기기(070–079)/PIN(080–089)/휴대폰인증(090–099)/본인확인(094–097)/세션(100)/관계자(110–119)/개인정보(120–129)/인증수단(130–139)/보안카드(140–149).

| 코드 | HTTP | 설명 |
|---|---|---|
| `CUST_001` | 409 | 이미 사용 중인 로그인 ID입니다. |
| `CUST_002` | 404 | 고객 정보를 찾을 수 없습니다. |
| `CUST_003` | 409 | 이미 가입된 고객입니다. |
| `CUST_010` | 401 | 아이디 또는 비밀번호가 올바르지 않습니다. |
| `CUST_011` | 403 | 계정이 잠겨 있습니다. 비밀번호 5회 오류. |
| `CUST_012` | 403 | 탈퇴하거나 비활성화된 계정입니다. |
| `CUST_013` | 401 | 비밀번호가 만료되었습니다. |
| `CUST_020` | 400 | 현재 비밀번호가 올바르지 않습니다. |
| `CUST_030` | 404 | 인증서를 찾을 수 없습니다. |
| `CUST_031` | 401 | 인증서가 만료되었습니다. |
| `CUST_032` | 403 | 폐기된 인증서입니다. |
| `CUST_033` | 401 | 인증서 PIN이 올바르지 않습니다. |
| `CUST_034` | 403 | 인증서가 잠겨 있습니다. |
| `CUST_040` | 404 | QR 토큰을 찾을 수 없습니다. |
| `CUST_041` | 410 | QR 코드가 만료되었습니다. |
| `CUST_042` | 409 | 이미 처리된 QR 코드입니다. |
| `CUST_050` | 404 | 출금계좌를 찾을 수 없습니다. |
| `CUST_051` | 409 | 이미 등록된 출금계좌입니다. |
| `CUST_052` | 400 | 본행 보유계좌는 출금계좌 등록 대상이 아닙니다. 타행 계좌만 등록할 수 있습니다. |
| `CUST_060` | 403 | 비정상 접근이 감지되어 요청이 차단되었습니다. |
| `CUST_061` | 409 | 이미 존재하는 FDS 룰 코드입니다. |
| `CUST_062` | 404 | FDS 탐지 결과를 찾을 수 없습니다. |
| `CUST_063` | 409 | 이미 사고 처리가 등록된 탐지 결과입니다. |
| `CUST_064` | 404 | FDS 룰을 찾을 수 없습니다. |
| `CUST_070` | 404 | 등록된 기기를 찾을 수 없습니다. |
| `CUST_071` | 400 | 지정 PC는 PC 타입 기기만 설정할 수 있습니다. |
| `CUST_080` | 404 | 등록된 PIN을 찾을 수 없습니다. |
| `CUST_081` | 409 | 이미 PIN이 등록된 기기입니다. |
| `CUST_082` | 401 | PIN이 올바르지 않습니다. |
| `CUST_083` | 403 | PIN이 잠겨 있습니다. |
| `CUST_090` | 404 | 인증 요청을 찾을 수 없습니다. |
| `CUST_091` | 410 | 인증 코드가 만료되었습니다. |
| `CUST_092` | 400 | 인증 코드가 올바르지 않습니다. |
| `CUST_093` | 409 | 이미 검증된 인증 요청입니다. |
| `CUST_094` | 404 | 본인확인 정보를 찾을 수 없습니다. |
| `CUST_095` | 410 | 본인확인이 만료되었습니다. 다시 인증해주세요. |
| `CUST_096` | 409 | 이미 사용된 본인확인입니다. |
| `CUST_097` | 400 | 주민등록번호 형식이 올바르지 않습니다. |
| `CUST_100` | 404 | 세션을 찾을 수 없습니다. |
| `CUST_110` | 409 | 이미 활성 상태인 역할입니다. |
| `CUST_111` | 404 | 역할을 찾을 수 없습니다. |
| `CUST_112` | 400 | 자기 자신과의 관계는 등록할 수 없습니다. |
| `CUST_113` | 409 | 이미 존재하는 관계입니다. |
| `CUST_114` | 404 | 관계를 찾을 수 없습니다. |
| `CUST_115` | 404 | 컴플라이언스 정보를 찾을 수 없습니다. |
| `CUST_120` | 404 | 외국인 정보를 찾을 수 없습니다. |
| `CUST_121` | 404 | 납세거주 정보를 찾을 수 없습니다. |
| `CUST_130` | 404 | 인증수단을 찾을 수 없습니다. |
| `CUST_131` | 400 | 주 인증수단은 비활성화할 수 없습니다. |
| `CUST_140` | 404 | 활성 보안카드를 찾을 수 없습니다. |
| `CUST_141` | 409 | 이미 활성 보안카드가 존재합니다. |
| `CUST_142` | 401 | 보안카드 코드가 올바르지 않습니다. |
| `CUST_143` | 410 | 보안카드 챌린지가 만료되었거나 존재하지 않습니다. |
| `CUST_144` | 400 | 챌린지에 없는 위치 코드가 포함되어 있습니다. |

---

## 엔드포인트

### 회원가입·등록

#### RegisterController

##### `POST` `/api/v1/auth/register`

**요청 본문**: `RegisterRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `password` | `String` | 필수(공백불가), 길이제한 |
| `phone` | `String` | 형식제약 |
| `email` | `String` | 길이제한, 이메일 |

**응답 data**: `RegisterResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `customerId` | `Long` |  |
| `loginId` | `String` |  |

##### `POST` `/api/v1/auth/register/corporate`

**요청 본문**: `CorporateRegisterRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `corpEnglishName` | `String` | 필수(공백불가) |
| `tradeName` | `String` | 필수(공백불가) |
| `openingDate` | `String` | 필수(공백불가) |
| `ksicCode` | `String` | 필수(공백불가) |
| `bizItemCode` | `String` | 필수(공백불가) |
| `taxTypeCode` | `String` | 필수(공백불가) |
| `password` | `String` | 필수(공백불가) |
| `email` | `String` | 필수(공백불가) |
| `phone` | `String` | 필수(공백불가) |

**응답 data**: `RegisterResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `customerId` | `Long` |  |
| `loginId` | `String` |  |

#### RegisteredDeviceController

##### `GET` `/api/v1/customers/me/devices`

**응답 data**: `List<RegisteredDeviceResponse>`

| 필드 | 타입 | 제약 |
|---|---|---|
| `deviceId` | `Long` |  |
| `deviceName` | `String` |  |
| `deviceTypeCode` | `String` |  |
| `deviceOsName` | `String` |  |
| `deviceOsVersion` | `String` |  |
| `trusted` | `boolean` |  |
| `designatedPc` | `boolean` |  |
| `deviceStatusCode` | `String` |  |
| `deviceLastUsedAt` | `OffsetDateTime` |  |
| `registeredAt` | `OffsetDateTime` |  |

##### `POST` `/api/v1/customers/me/devices`

**요청 본문**: `RegisterDeviceRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `deviceTypeCode` | `String` | 필수(공백불가) |
| `deviceName` | `String` |  |
| `deviceOsName` | `String` |  |
| `deviceOsVersion` | `String` |  |

**응답 data**: `RegisteredDeviceResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `deviceId` | `Long` |  |
| `deviceName` | `String` |  |
| `deviceTypeCode` | `String` |  |
| `deviceOsName` | `String` |  |
| `deviceOsVersion` | `String` |  |
| `trusted` | `boolean` |  |
| `designatedPc` | `boolean` |  |
| `deviceStatusCode` | `String` |  |
| `deviceLastUsedAt` | `OffsetDateTime` |  |
| `registeredAt` | `OffsetDateTime` |  |

##### `DELETE` `/api/v1/customers/me/devices/{deviceId}`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `deviceId` | `Long` |

**응답 data**: `Void`

##### `PATCH` `/api/v1/customers/me/devices/{deviceId}/designate`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `deviceId` | `Long` |

**응답 data**: `RegisteredDeviceResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `deviceId` | `Long` |  |
| `deviceName` | `String` |  |
| `deviceTypeCode` | `String` |  |
| `deviceOsName` | `String` |  |
| `deviceOsVersion` | `String` |  |
| `trusted` | `boolean` |  |
| `designatedPc` | `boolean` |  |
| `deviceStatusCode` | `String` |  |
| `deviceLastUsedAt` | `OffsetDateTime` |  |
| `registeredAt` | `OffsetDateTime` |  |

##### `PATCH` `/api/v1/customers/me/devices/{deviceId}/trust`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `deviceId` | `Long` |

**응답 data**: `RegisteredDeviceResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `deviceId` | `Long` |  |
| `deviceName` | `String` |  |
| `deviceTypeCode` | `String` |  |
| `deviceOsName` | `String` |  |
| `deviceOsVersion` | `String` |  |
| `trusted` | `boolean` |  |
| `designatedPc` | `boolean` |  |
| `deviceStatusCode` | `String` |  |
| `deviceLastUsedAt` | `OffsetDateTime` |  |
| `registeredAt` | `OffsetDateTime` |  |

##### `PATCH` `/api/v1/customers/me/devices/{deviceId}/untrust`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `deviceId` | `Long` |

**응답 data**: `RegisteredDeviceResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `deviceId` | `Long` |  |
| `deviceName` | `String` |  |
| `deviceTypeCode` | `String` |  |
| `deviceOsName` | `String` |  |
| `deviceOsVersion` | `String` |  |
| `trusted` | `boolean` |  |
| `designatedPc` | `boolean` |  |
| `deviceStatusCode` | `String` |  |
| `deviceLastUsedAt` | `OffsetDateTime` |  |
| `registeredAt` | `OffsetDateTime` |  |

### 로그인·세션

#### LoginController

##### `POST` `/api/v1/auth/login`

**요청 본문**: `LoginRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `loginId` | `String` | 필수(공백불가) |
| `password` | `String` | 필수(공백불가) |

**응답 data**: `LoginResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `customerId` | `Long` |  |
| `accessToken` | `String` |  |
| `refreshToken` | `String` |  |

##### `POST` `/api/v1/auth/refresh`

**요청 본문**: `RefreshRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `refreshToken` | `String` | 필수(공백불가) |

**응답 data**: `LoginResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `customerId` | `Long` |  |
| `accessToken` | `String` |  |
| `refreshToken` | `String` |  |

### 인증서(공동인증서)

#### CertIssueController

##### `POST` `/api/v1/auth/cert/issue`

**요청 본문**: `CertIssueRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `loginId` | `String` | 필수(공백불가) |
| `password` | `String` | 필수(공백불가) |
| `certType` | `String` | 필수(공백불가), 형식제약 |
| `certPin` | `String` | 필수(공백불가), 길이제한 |

**응답 data**: `CertIssueResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `serialNumber` | `String` |  |
| `certType` | `String` |  |
| `issuerName` | `String` |  |
| `subjectDn` | `String` |  |
| `issuedDate` | `String` |  |
| `expiryDate` | `String` |  |

##### `PUT` `/api/v1/auth/cert/pin`

**요청 본문**: `CertPinChangeRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `certSerialNumber` | `String` | 필수(공백불가) |
| `currentPin` | `String` | 필수(공백불가) |
| `newPin` | `String` | 필수(공백불가), 길이제한 |

**응답 data**: `Void`

#### CertLoginController

##### `POST` `/api/v1/auth/cert-login`

**요청 본문**: `CertLoginRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `certSerialNumber` | `String` | 필수(공백불가) |
| `pin` | `String` | 필수(공백불가) |

**응답 data**: `LoginResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `customerId` | `Long` |  |
| `accessToken` | `String` |  |
| `refreshToken` | `String` |  |

#### CertManageController

##### `GET` `/api/v1/cert/manage`

내 인증서 목록

**응답 data**: `List<CertSummaryResponse>`

| 필드 | 타입 | 제약 |
|---|---|---|
| `serialNumber` | `String` |  |
| `certType` | `String` |  |
| `certTypeName` | `String` |  |
| `issuerName` | `String` |  |
| `issuedDate` | `String` |  |
| `expiryDate` | `String` |  |
| `status` | `String` |  |
| `statusName` | `String` |  |

##### `PUT` `/api/v1/cert/manage/pin`

인증서 암호 변경

**요청 본문**: `CertPinChangeRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `certSerialNumber` | `String` | 필수(공백불가) |
| `currentPin` | `String` | 필수(공백불가) |
| `newPin` | `String` | 필수(공백불가), 길이제한 |

**응답 data**: `Void`

##### `DELETE` `/api/v1/cert/manage/{serialNumber}`

인증서 폐기

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `serialNumber` | `String` |

**응답 data**: `Void`

##### `GET` `/api/v1/cert/manage/{serialNumber}`

인증서 상세 조회

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `serialNumber` | `String` |

**응답 data**: `CertDetailResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `serialNumber` | `String` |  |
| `certType` | `String` |  |
| `certTypeName` | `String` |  |
| `issuerName` | `String` |  |
| `subjectDn` | `String` |  |
| `issuerDn` | `String` |  |
| `issuedDate` | `String` |  |
| `expiryDate` | `String` |  |
| `status` | `String` |  |
| `statusName` | `String` |  |
| `purposeCode` | `String` |  |
| `hasPinSet` | `boolean` |  |

#### QrCertController

##### `POST` `/api/v1/auth/qr-cert/approve`

모바일: QR 승인 (loginId + password 인증 후 인증서 발급)

**요청 본문**: `QrCertApproveRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `tokenHash` | `String` | 필수(공백불가) |
| `loginId` | `String` | 필수(공백불가) |
| `password` | `String` | 필수(공백불가) |

**응답 data**: `Void`

##### `POST` `/api/v1/auth/qr-cert/generate`

PC: QR 토큰 생성

**응답 data**: `QrGenerateResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `tokenHash` | `String` |  |
| `confirmCode` | `String` |  |
| `expiryAt` | `OffsetDateTime` |  |

##### `GET` `/api/v1/auth/qr-cert/status`

PC: 상태 폴링. APPROVED면 serialNumber·issuedDate·expiryDate 포함

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `token` | `String` | O |

**응답 data**: `QrCertStatusResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `status` | `String` |  |
| `serialNumber` | `String` |  |
| `issuedDate` | `String` |  |
| `expiryDate` | `String` |  |

#### QrLoginController

##### `POST` `/api/v1/auth/qr/approve`

모바일: QR 승인 (loginId + password로 인증 후 QR 토큰 APPROVED 전환)

**요청 본문**: `QrApproveRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `tokenHash` | `String` | 필수(공백불가) |
| `loginId` | `String` | 필수(공백불가) |
| `password` | `String` | 필수(공백불가) |

**응답 data**: `Void`

##### `POST` `/api/v1/auth/qr/generate`

PC: QR 토큰 생성

**응답 data**: `QrGenerateResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `tokenHash` | `String` |  |
| `confirmCode` | `String` |  |
| `expiryAt` | `OffsetDateTime` |  |

##### `GET` `/api/v1/auth/qr/status`

PC: 상태 폴링. APPROVED면 accessToken·refreshToken 포함

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `token` | `String` | O |

**응답 data**: `QrStatusResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `status` | `String` |  |
| `customerId` | `Long` |  |
| `accessToken` | `String` |  |
| `refreshToken` | `String` |  |

### 인증수단·휴대폰·본인확인

#### AuthMethodController

##### `GET` `/api/v1/customers/me/auth-methods`

**응답 data**: `List<AuthMethodResponse>`

| 필드 | 타입 | 제약 |
|---|---|---|
| `authMethodId` | `Long` |  |
| `authMethodTypeCode` | `String` |  |
| `authMethodAliasName` | `String` |  |
| `authMethodStatusCode` | `String` |  |
| `primary` | `boolean` |  |
| `authMethodRegisteredDate` | `String` |  |
| `authMethodExpiryDate` | `String` |  |
| `lastUsedAt` | `OffsetDateTime` |  |

##### `DELETE` `/api/v1/customers/me/auth-methods/{authMethodId}`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `authMethodId` | `Long` |

**응답 data**: `Void`

##### `PATCH` `/api/v1/customers/me/auth-methods/{authMethodId}/alias`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `authMethodId` | `Long` |

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `alias` | `String` | O |

**응답 data**: `AuthMethodResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `authMethodId` | `Long` |  |
| `authMethodTypeCode` | `String` |  |
| `authMethodAliasName` | `String` |  |
| `authMethodStatusCode` | `String` |  |
| `primary` | `boolean` |  |
| `authMethodRegisteredDate` | `String` |  |
| `authMethodExpiryDate` | `String` |  |
| `lastUsedAt` | `OffsetDateTime` |  |

##### `PATCH` `/api/v1/customers/me/auth-methods/{authMethodId}/primary`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `authMethodId` | `Long` |

**응답 data**: `AuthMethodResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `authMethodId` | `Long` |  |
| `authMethodTypeCode` | `String` |  |
| `authMethodAliasName` | `String` |  |
| `authMethodStatusCode` | `String` |  |
| `primary` | `boolean` |  |
| `authMethodRegisteredDate` | `String` |  |
| `authMethodExpiryDate` | `String` |  |
| `lastUsedAt` | `OffsetDateTime` |  |

#### MobileAuthController

##### `POST` `/api/v1/mobile-auth/send`

인증 코드 발송

**요청 본문**: `SendMobileAuthRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `phoneNumber` | `String` | 필수(공백불가), 형식제약 |
| `telecomCarrierCode` | `String` | 필수(공백불가) |
| `purposeCode` | `String` | 필수(공백불가) |

**응답 data**: `Long`

##### `POST` `/api/v1/mobile-auth/verify`

인증 코드 검증 — 신원확인 목적이면 응답에 verificationId 포함

**요청 본문**: `VerifyMobileAuthRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `phoneNumber` | `String` | 필수(공백불가), 형식제약 |
| `purposeCode` | `String` | 필수(공백불가) |
| `code` | `String` | 필수(공백불가), 형식제약 |

**응답 data**: `VerifyMobileAuthResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `verificationId` | `Long` |  |

### PIN·보안

#### PinController

##### `POST` `/api/v1/auth/pin-login`

PIN 로그인

**요청 본문**: `PinLoginRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `loginId` | `String` | 필수(공백불가) |
| `deviceId` | `Long` | 필수 |
| `pin` | `String` | 필수(공백불가), 형식제약 |

**응답 data**: `LoginResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `customerId` | `Long` |  |
| `accessToken` | `String` |  |
| `refreshToken` | `String` |  |

##### `DELETE` `/api/v1/customers/me/pin`

PIN 해제

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `deviceId` | `Long` | O |

**응답 data**: `Void`

##### `POST` `/api/v1/customers/me/pin`

PIN 등록

**요청 본문**: `RegisterPinRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `deviceId` | `Long` | 필수 |
| `pin` | `String` | 필수(공백불가), 형식제약 |

**응답 data**: `Void`

### 출금계좌(타행)

#### WithdrawalAccountController

##### `GET` `/api/v1/banking/withdrawal-accounts`

출금계좌 목록

**응답 data**: `List<WithdrawalAccountResponse>`

| 필드 | 타입 | 제약 |
|---|---|---|
| `withdrawalAccountId` | `Long` |  |
| `accountNumber` | `String` |  |
| `bankCode` | `String` |  |
| `bankName` | `String` |  |
| `accountHolderName` | `String` |  |
| `accountAlias` | `String` |  |
| `registrationType` | `String` |  |
| `priorityOrder` | `int` |  |
| `registeredAt` | `String` |  |

##### `POST` `/api/v1/banking/withdrawal-accounts`

출금계좌 등록

**요청 본문**: `RegisterWithdrawalAccountRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `accountNumber` | `String` | 필수(공백불가), 형식제약 |
| `bankCode` | `String` | 필수(공백불가), 길이제한 |
| `bankName` | `String` | 필수(공백불가), 길이제한 |
| `accountHolderName` | `String` | 길이제한 |
| `accountAlias` | `String` | 길이제한 |

**응답 data**: `WithdrawalAccountResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `withdrawalAccountId` | `Long` |  |
| `accountNumber` | `String` |  |
| `bankCode` | `String` |  |
| `bankName` | `String` |  |
| `accountHolderName` | `String` |  |
| `accountAlias` | `String` |  |
| `registrationType` | `String` |  |
| `priorityOrder` | `int` |  |
| `registeredAt` | `String` |  |

##### `PUT` `/api/v1/banking/withdrawal-accounts/order`

순위 변경

**요청 본문**: `UpdateOrderRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `orderedIds` | `List<Long>` | 필수 |

**응답 data**: `Void`

##### `DELETE` `/api/v1/banking/withdrawal-accounts/{id}`

출금계좌 삭제

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `id` | `Long` |

**응답 data**: `Void`

### 마이페이지·설정

#### MyPageController

##### `GET` `/api/v1/customers/me`

**응답 data**: `MyPageResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `customerId` | `Long` |  |
| `loginId` | `String` |  |
| `name` | `String` |  |
| `email` | `String` |  |
| `phone` | `String` |  |
| `zipCode` | `String` |  |
| `address` | `String` |  |
| `addressDetail` | `String` |  |
| `birthDate` | `String` |  |
| `genderCode` | `String` |  |
| `customerGradeCode` | `String` |  |
| `customerStatusCode` | `String` |  |
| `creditRatingCode` | `String` |  |
| `joinedAt` | `OffsetDateTime` |  |
| `lastLoginAt` | `OffsetDateTime` |  |

#### SettingsController

##### `PUT` `/api/v1/customers/me/notification`

**요청 본문**: `UpdateNotificationRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `smsReceiveYn` | `boolean` |  |
| `emailReceiveYn` | `boolean` |  |
| `postalReceiveYn` | `boolean` |  |

**응답 data**: `Void`

##### `PUT` `/api/v1/customers/me/password`

**요청 본문**: `ChangePasswordRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `currentPassword` | `String` |  |
| `newPassword` | `String` |  |

**응답 data**: `Void`

##### `PUT` `/api/v1/customers/me/profile`

**요청 본문**: `UpdateProfileRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `email` | `String` |  |
| `phone` | `String` |  |
| `zipCode` | `String` |  |
| `address` | `String` |  |
| `addressDetail` | `String` |  |

**응답 data**: `Void`

##### `GET` `/api/v1/customers/me/settings`

**응답 data**: `SettingsResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `name` | `String` |  |
| `email` | `String` |  |
| `phone` | `String` |  |
| `zipCode` | `String` |  |
| `address` | `String` |  |
| `addressDetail` | `String` |  |
| `smsReceiveYn` | `boolean` |  |
| `emailReceiveYn` | `boolean` |  |
| `postalReceiveYn` | `boolean` |  |

##### `POST` `/api/v1/customers/me/withdraw`

**요청 본문**: `WithdrawRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `currentPassword` | `String` |  |

**응답 data**: `Void`

### 공통코드

#### CodeController

##### `GET` `/api/v1/codes/{groupId}`

그룹 내 유효 코드 조회 (기본: 오늘 기준 활성 코드)

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `groupId` | `String` |

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `date` | `String` | - |

**응답 data**: `List<CodeResponse>`

| 필드 | 타입 | 제약 |
|---|---|---|
| `codeGroupId` | `String` |  |
| `codeValue` | `String` |  |
| `codeName` | `String` |  |
| `description` | `String` |  |
| `sortOrder` | `Integer` |  |

##### `GET` `/api/v1/codes/{groupId}/all`

그룹 내 전체 코드 조회 (만료 포함)

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `groupId` | `String` |

**응답 data**: `List<CodeResponse>`

| 필드 | 타입 | 제약 |
|---|---|---|
| `codeGroupId` | `String` |  |
| `codeValue` | `String` |  |
| `codeName` | `String` |  |
| `description` | `String` |  |
| `sortOrder` | `Integer` |  |

### FDS(이상거래)

#### FdsController

##### `GET` `/api/v1/internal/fds/detections/pending`

-------------------------------------------------------------------------

**응답 data**: `Page<FdsDetectionResponse>`

##### `PATCH` `/api/v1/internal/fds/detections/{detectionId}/confirm`

-------------------------------------------------------------------------

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `detectionId` | `Long` |

**응답 data**: `FdsDetectionResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `fdsDetectionId` | `Long` |  |
| `customerId` | `Long` |  |
| `fdsRuleId` | `Long` |  |
| `fdsDetectionEventTypeCode` | `String` |  |
| `fdsDetectionEventReferenceId` | `Long` |  |
| `fdsDetectedAt` | `OffsetDateTime` |  |
| `fdsDetectionStatusCode` | `String` |  |

##### `PATCH` `/api/v1/internal/fds/detections/{detectionId}/false-positive`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `detectionId` | `Long` |

**응답 data**: `FdsDetectionResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `fdsDetectionId` | `Long` |  |
| `customerId` | `Long` |  |
| `fdsRuleId` | `Long` |  |
| `fdsDetectionEventTypeCode` | `String` |  |
| `fdsDetectionEventReferenceId` | `Long` |  |
| `fdsDetectedAt` | `OffsetDateTime` |  |
| `fdsDetectionStatusCode` | `String` |  |

##### `POST` `/api/v1/internal/fds/incidents`

-------------------------------------------------------------------------

**요청 본문**: `FdsIncidentRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `fdsDetectionId` | `Long` | 필수 |
| `fdsIncidentTypeCode` | `String` | 필수(공백불가) |
| `handlerEmployeeId` | `Long` |  |

**응답 data**: `FdsIncidentResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `fdsIncidentId` | `Long` |  |
| `fdsDetectionId` | `Long` |  |
| `handlerEmployeeId` | `Long` |  |
| `fdsIncidentTypeCode` | `String` |  |
| `fdsIncidentProcessStatusCode` | `String` |  |
| `fssReported` | `boolean` |  |
| `fdsIncidentReportedAt` | `OffsetDateTime` |  |
| `fdsIncidentClosedAt` | `OffsetDateTime` |  |

##### `GET` `/api/v1/internal/fds/incidents/open`

-------------------------------------------------------------------------

**응답 data**: `Page<FdsIncidentResponse>`

##### `PATCH` `/api/v1/internal/fds/incidents/{incidentId}/close`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `incidentId` | `Long` |

**응답 data**: `FdsIncidentResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `fdsIncidentId` | `Long` |  |
| `fdsDetectionId` | `Long` |  |
| `handlerEmployeeId` | `Long` |  |
| `fdsIncidentTypeCode` | `String` |  |
| `fdsIncidentProcessStatusCode` | `String` |  |
| `fssReported` | `boolean` |  |
| `fdsIncidentReportedAt` | `OffsetDateTime` |  |
| `fdsIncidentClosedAt` | `OffsetDateTime` |  |

##### `PATCH` `/api/v1/internal/fds/incidents/{incidentId}/report-fss`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `incidentId` | `Long` |

**응답 data**: `FdsIncidentResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `fdsIncidentId` | `Long` |  |
| `fdsDetectionId` | `Long` |  |
| `handlerEmployeeId` | `Long` |  |
| `fdsIncidentTypeCode` | `String` |  |
| `fdsIncidentProcessStatusCode` | `String` |  |
| `fssReported` | `boolean` |  |
| `fdsIncidentReportedAt` | `OffsetDateTime` |  |
| `fdsIncidentClosedAt` | `OffsetDateTime` |  |

##### `GET` `/api/v1/internal/fds/rules`

-------------------------------------------------------------------------

**응답 data**: `List<FdsRuleResponse>`

| 필드 | 타입 | 제약 |
|---|---|---|
| `fdsRuleId` | `Long` |  |
| `fdsRuleCode` | `String` |  |
| `fdsRuleName` | `String` |  |
| `fdsRuleCategoryCode` | `String` |  |
| `fdsRuleTargetEventCode` | `String` |  |
| `fdsRuleConditionJson` | `String` |  |
| `fdsRuleRiskWeight` | `int` |  |
| `fdsRuleActionTypeCode` | `String` |  |
| `active` | `boolean` |  |
| `fdsRuleEffectiveDate` | `String` |  |
| `fdsRuleExpiryDate` | `String` |  |

##### `POST` `/api/v1/internal/fds/rules`

FDS 관리 API — 직원 전용.

**요청 본문**: `FdsRuleRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `fdsRuleCode` | `String` | 필수(공백불가) |
| `fdsRuleName` | `String` | 필수(공백불가) |
| `fdsRuleCategoryCode` | `String` | 필수(공백불가) |
| `fdsRuleTargetEventCode` | `String` | 필수(공백불가) |
| `fdsRuleConditionJson` | `String` | 필수(공백불가) |
| `fdsRuleRiskWeight` | `Integer` | 필수 |
| `fdsRuleActionTypeCode` | `String` | 필수(공백불가) |
| `fdsRuleEffectiveDate` | `String` | 필수(공백불가) |
| `fdsRuleExpiryDate` | `String` |  |

**응답 data**: `FdsRuleResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `fdsRuleId` | `Long` |  |
| `fdsRuleCode` | `String` |  |
| `fdsRuleName` | `String` |  |
| `fdsRuleCategoryCode` | `String` |  |
| `fdsRuleTargetEventCode` | `String` |  |
| `fdsRuleConditionJson` | `String` |  |
| `fdsRuleRiskWeight` | `int` |  |
| `fdsRuleActionTypeCode` | `String` |  |
| `active` | `boolean` |  |
| `fdsRuleEffectiveDate` | `String` |  |
| `fdsRuleExpiryDate` | `String` |  |

##### `PATCH` `/api/v1/internal/fds/rules/{ruleId}/activate`

-------------------------------------------------------------------------

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `ruleId` | `Long` |

**응답 data**: `FdsRuleResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `fdsRuleId` | `Long` |  |
| `fdsRuleCode` | `String` |  |
| `fdsRuleName` | `String` |  |
| `fdsRuleCategoryCode` | `String` |  |
| `fdsRuleTargetEventCode` | `String` |  |
| `fdsRuleConditionJson` | `String` |  |
| `fdsRuleRiskWeight` | `int` |  |
| `fdsRuleActionTypeCode` | `String` |  |
| `active` | `boolean` |  |
| `fdsRuleEffectiveDate` | `String` |  |
| `fdsRuleExpiryDate` | `String` |  |

##### `PATCH` `/api/v1/internal/fds/rules/{ruleId}/deactivate`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `ruleId` | `Long` |

**응답 data**: `FdsRuleResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `fdsRuleId` | `Long` |  |
| `fdsRuleCode` | `String` |  |
| `fdsRuleName` | `String` |  |
| `fdsRuleCategoryCode` | `String` |  |
| `fdsRuleTargetEventCode` | `String` |  |
| `fdsRuleConditionJson` | `String` |  |
| `fdsRuleRiskWeight` | `int` |  |
| `fdsRuleActionTypeCode` | `String` |  |
| `active` | `boolean` |  |
| `fdsRuleEffectiveDate` | `String` |  |
| `fdsRuleExpiryDate` | `String` |  |

### 관계자·개인정보·컴플라이언스

#### PartyController

##### `GET` `/api/v1/customers/me/roles`

내 역할 목록

**응답 data**: `List<PartyRoleResponse>`

| 필드 | 타입 | 제약 |
|---|---|---|
| `roleId` | `Long` |  |
| `partyId` | `Long` |  |
| `roleTypeCode` | `String` |  |
| `roleStatusCode` | `String` |  |
| `roleStartDate` | `String` |  |
| `roleEndDate` | `String` |  |
| `roleEndReasonCode` | `String` |  |

##### `GET` `/api/v1/internal/compliance/edd-pending`

EDD 심사 대기 목록 (직원용) — EDD 심사·승인 화면의 진입점

**응답 data**: `Page<EddPendingResponse>`

##### `GET` `/api/v1/internal/compliance/fatca-crs`

FATCA/CRS 보고대상 목록 (직원용) — FATCA/CRS 화면의 진입점

**응답 data**: `Page<FatcaReportableResponse>`

##### `GET` `/api/v1/internal/compliance/kyc-expiring`

KYC 만료 예정 목록 (직원용) — targetDate(YYYYMMDD) 이하 만료분

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `targetDate` | `String` | O |

**응답 data**: `Page<KycExpiringResponse>`

##### `GET` `/api/v1/internal/compliance/sanctioned`

제재대상 스크리닝 목록 (직원용) — 제재대상 Hit 검토 화면의 진입점

**응답 data**: `Page<SanctionedPartyResponse>`

##### `GET` `/api/v1/internal/compliance/screening-hits/pending`

제재 스크리닝 검토 대기 큐 — 제재대상 Hit 검토 화면의 진입점

**응답 data**: `Page<SanctionHitResponse>`

##### `PATCH` `/api/v1/internal/compliance/screening-hits/{hitId}/clear`

Hit 무혐의 처리 (동명이인 등)

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `hitId` | `Long` |

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `comment` | `String` | - |

**응답 data**: `Void`

##### `PATCH` `/api/v1/internal/compliance/screening-hits/{hitId}/confirm`

Hit 제재 확정 처리

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `hitId` | `Long` |

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `comment` | `String` | - |

**응답 data**: `Void`

##### `GET` `/api/v1/internal/party/duplicates/pending`

중복고객 검토 대기 큐 — 중복고객 검토 화면의 진입점

**응답 data**: `Page<DuplicateReviewResponse>`

##### `PATCH` `/api/v1/internal/party/duplicates/{caseId}/distinct`

별개(동명이인 등) 확정

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `caseId` | `Long` |

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `comment` | `String` | - |

**응답 data**: `Void`

##### `PATCH` `/api/v1/internal/party/duplicates/{caseId}/duplicate`

복본 확정

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `caseId` | `Long` |

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `comment` | `String` | - |

**응답 data**: `Void`

##### `GET` `/api/v1/internal/party/minors`

미성년(만 19세 미만) 목록 (직원용) — 미성년 검토 화면의 진입점

**응답 data**: `Page<MinorResponse>`

##### `GET` `/api/v1/internal/party/relations/review-pending`

대리인 위임장 검토 대기 목록 — 대리인 검토 화면의 진입점

**응답 data**: `Page<AgentReviewResponse>`

##### `PATCH` `/api/v1/internal/party/relations/{relationId}/approve`

위임장 검토 승인 (권한 액팅)

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `relationId` | `Long` |

**응답 data**: `PartyRelationResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `relationId` | `Long` |  |
| `fromPartyId` | `Long` |  |
| `toPartyId` | `Long` |  |
| `relationTypeCode` | `String` |  |
| `relationDetailCode` | `String` |  |
| `equityRatioBps` | `Integer` |  |
| `representationScope` | `String` |  |
| `relationStartDate` | `String` |  |
| `relationEndDate` | `String` |  |

##### `PATCH` `/api/v1/internal/party/relations/{relationId}/end`

관계 종료 (직원용)

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `relationId` | `Long` |

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `endDate` | `String` | - |
| `reasonCode` | `String` | - |

**응답 data**: `PartyRelationResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `relationId` | `Long` |  |
| `fromPartyId` | `Long` |  |
| `toPartyId` | `Long` |  |
| `relationTypeCode` | `String` |  |
| `relationDetailCode` | `String` |  |
| `equityRatioBps` | `Integer` |  |
| `representationScope` | `String` |  |
| `relationStartDate` | `String` |  |
| `relationEndDate` | `String` |  |

##### `PATCH` `/api/v1/internal/party/relations/{relationId}/reject`

위임장 검토 거절 (위조 의심 등)

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `relationId` | `Long` |

**응답 data**: `PartyRelationResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `relationId` | `Long` |  |
| `fromPartyId` | `Long` |  |
| `toPartyId` | `Long` |  |
| `relationTypeCode` | `String` |  |
| `relationDetailCode` | `String` |  |
| `equityRatioBps` | `Integer` |  |
| `representationScope` | `String` |  |
| `relationStartDate` | `String` |  |
| `relationEndDate` | `String` |  |

##### `PATCH` `/api/v1/internal/party/roles/{roleId}/close`

역할 종료 (직원용)

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `roleId` | `Long` |

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `endDate` | `String` | - |
| `reasonCode` | `String` | O |

**응답 data**: `PartyRoleResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `roleId` | `Long` |  |
| `partyId` | `Long` |  |
| `roleTypeCode` | `String` |  |
| `roleStatusCode` | `String` |  |
| `roleStartDate` | `String` |  |
| `roleEndDate` | `String` |  |
| `roleEndReasonCode` | `String` |  |

##### `POST` `/api/v1/internal/party/{fromPartyId}/relations`

관계 등록 (직원용)

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `fromPartyId` | `Long` |

**요청 본문**: `AddPartyRelationRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `toPartyId` | `Long` | 필수 |
| `relationTypeCode` | `String` | 필수(공백불가) |
| `relationDetailCode` | `String` |  |
| `equityRatioBps` | `Integer` |  |
| `representationScope` | `String` |  |
| `proofUrl` | `String` |  |

**응답 data**: `PartyRelationResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `relationId` | `Long` |  |
| `fromPartyId` | `Long` |  |
| `toPartyId` | `Long` |  |
| `relationTypeCode` | `String` |  |
| `relationDetailCode` | `String` |  |
| `equityRatioBps` | `Integer` |  |
| `representationScope` | `String` |  |
| `relationStartDate` | `String` |  |
| `relationEndDate` | `String` |  |

##### `GET` `/api/v1/internal/party/{partyId}/compliance`

컴플라이언스 정보 조회 (직원용)

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `partyId` | `Long` |

**응답 data**: `ComplianceInfo`

##### `PATCH` `/api/v1/internal/party/{partyId}/compliance/aml-risk`

AML 위험도 변경 (직원용)

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `partyId` | `Long` |

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `riskLevel` | `String` | O |

**응답 data**: `Void`

##### `PATCH` `/api/v1/internal/party/{partyId}/compliance/kyc-complete`

KYC 완료 처리 (직원용)

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `partyId` | `Long` |

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `expiryDate` | `String` | O |
| `methodCode` | `String` | O |

**응답 data**: `Void`

##### `GET` `/api/v1/internal/party/{partyId}/relations`

관계자 관계 목록 (직원용)

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `partyId` | `Long` |

**응답 data**: `List<PartyRelationResponse>`

| 필드 | 타입 | 제약 |
|---|---|---|
| `relationId` | `Long` |  |
| `fromPartyId` | `Long` |  |
| `toPartyId` | `Long` |  |
| `relationTypeCode` | `String` |  |
| `relationDetailCode` | `String` |  |
| `equityRatioBps` | `Integer` |  |
| `representationScope` | `String` |  |
| `relationStartDate` | `String` |  |
| `relationEndDate` | `String` |  |

#### PersonInfoController

##### `GET` `/api/v1/customers/me/foreigner-info`

── 외국인정보 (foreigner_info) ───────────────────────────────────────────

**응답 data**: `ForeignerInfoResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `passportNo` | `String` |  |
| `passportCountryCode` | `String` |  |
| `passportExpiryDate` | `String` |  |
| `stayQualificationCode` | `String` |  |
| `stayExpiryDate` | `String` |  |
| `recentEntryDate` | `String` |  |
| `stayAddress` | `String` |  |

##### `PUT` `/api/v1/customers/me/foreigner-info/passport`

── 외국인정보 (foreigner_info) ───────────────────────────────────────────

**요청 본문**: `UpdatePassportRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `passportNo` | `String` | 필수(공백불가) |
| `countryCode` | `String` | 필수(공백불가) |
| `expiryDate` | `String` | 필수(공백불가) |

**응답 data**: `ForeignerInfoResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `passportNo` | `String` |  |
| `passportCountryCode` | `String` |  |
| `passportExpiryDate` | `String` |  |
| `stayQualificationCode` | `String` |  |
| `stayExpiryDate` | `String` |  |
| `recentEntryDate` | `String` |  |
| `stayAddress` | `String` |  |

##### `PUT` `/api/v1/customers/me/foreigner-info/stay`

**요청 본문**: `UpdateStayRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `stayQualificationCode` | `String` | 필수(공백불가) |
| `stayExpiryDate` | `String` | 필수(공백불가) |

**응답 data**: `ForeignerInfoResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `passportNo` | `String` |  |
| `passportCountryCode` | `String` |  |
| `passportExpiryDate` | `String` |  |
| `stayQualificationCode` | `String` |  |
| `stayExpiryDate` | `String` |  |
| `recentEntryDate` | `String` |  |
| `stayAddress` | `String` |  |

##### `GET` `/api/v1/customers/me/person-info`

── 개인정보 (party_person) ───────────────────────────────────────────────

**응답 data**: `PersonInfoResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `birthDate` | `String` |  |
| `genderCode` | `String` |  |
| `nationalityTypeCode` | `String` |  |
| `nationalityCode` | `String` |  |
| `maritalStatusCode` | `String` |  |
| `dependentCount` | `Integer` |  |
| `occupationCode` | `String` |  |
| `occupationName` | `String` |  |
| `workplaceName` | `String` |  |
| `annualIncomeAmount` | `Long` |  |
| `incomeProofCode` | `String` |  |
| `isPep` | `boolean` |  |
| `pepTypeCode` | `String` |  |

##### `PUT` `/api/v1/customers/me/person-info`

── 개인정보 (party_person) ───────────────────────────────────────────────

**요청 본문**: `UpdatePersonInfoRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `occupationCode` | `String` |  |
| `occupationName` | `String` |  |
| `workplaceName` | `String` |  |
| `annualIncomeAmount` | `Long` |  |
| `incomeProofCode` | `String` |  |
| `maritalStatusCode` | `String` |  |

**응답 data**: `PersonInfoResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `birthDate` | `String` |  |
| `genderCode` | `String` |  |
| `nationalityTypeCode` | `String` |  |
| `nationalityCode` | `String` |  |
| `maritalStatusCode` | `String` |  |
| `dependentCount` | `Integer` |  |
| `occupationCode` | `String` |  |
| `occupationName` | `String` |  |
| `workplaceName` | `String` |  |
| `annualIncomeAmount` | `Long` |  |
| `incomeProofCode` | `String` |  |
| `isPep` | `boolean` |  |
| `pepTypeCode` | `String` |  |

##### `GET` `/api/v1/customers/me/tax-residencies`

── 납세거주정보 (tax_residency_info) ─────────────────────────────────────

**응답 data**: `List<TaxResidencyResponse>`

| 필드 | 타입 | 제약 |
|---|---|---|
| `taxResidencyId` | `Long` |  |
| `residentTypeCode` | `String` |  |
| `taxCountryCode` | `String` |  |
| `foreignTin` | `String` |  |
| `withholdingRateBps` | `Integer` |  |
| `taxResidencyConfirmDate` | `String` |  |

##### `POST` `/api/v1/customers/me/tax-residencies`

── 납세거주정보 (tax_residency_info) ─────────────────────────────────────

**요청 본문**: `AddTaxResidencyRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `residentTypeCode` | `String` | 필수(공백불가) |
| `taxCountryCode` | `String` |  |
| `foreignTin` | `String` |  |
| `withholdingRateBps` | `Integer` |  |
| `taxResidencyConfirmDate` | `String` | 필수(공백불가) |

**응답 data**: `TaxResidencyResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `taxResidencyId` | `Long` |  |
| `residentTypeCode` | `String` |  |
| `taxCountryCode` | `String` |  |
| `foreignTin` | `String` |  |
| `withholdingRateBps` | `Integer` |  |
| `taxResidencyConfirmDate` | `String` |  |

##### `DELETE` `/api/v1/customers/me/tax-residencies/{taxResidencyId}`

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `taxResidencyId` | `Long` |

**응답 data**: `Void`

### 회원 라이프사이클(직원)

#### CustomerAccessLogController

##### `GET` `/api/v1/internal/customers/access-logs`

감사로그 조회 — 감사 화면 데이터원. keyword(직원명·고객명·행위), branch(지점 한정) 선택 필터.

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `keyword` | `String` | - |
| `branch` | `String` | - |

**응답 data**: `Page<AccessLogResponse>`

##### `POST` `/api/v1/internal/customers/{customerId}/access-log`

명시적 접근 기록(연락처 등 민감정보 열람). 행위 직원은 X-Employee-Id 헤더로 식별.

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `customerId` | `Long` |

**요청 본문**: `RecordAccessRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `actionCode` | `String` | 필수(공백불가) |
| `reason` | `String` |  |

**응답 data**: `Void`

#### CustomerLifecycleController

##### `GET` `/api/v1/customers/me/grade-history`

── 고객용 이력 조회 ──────────────────────────────────────────────────────

**응답 data**: `List<CustomerGradeHistory>`

##### `GET` `/api/v1/customers/me/status-history`

── 고객용 이력 조회 ──────────────────────────────────────────────────────

**응답 data**: `List<CustomerStatusHistory>`

##### `GET` `/api/v1/internal/customers`

고객 목록·검색 — 모든 list 화면의 진입점.

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `keyword` | `String` | - |
| `status` | `String` | - |
| `grade` | `String` | - |

**응답 data**: `Page<CustomerSummaryResponse>`

##### `GET` `/api/v1/internal/customers/join-stats`

가입 현황 통계 — 가입 대시보드의 데이터원(customer 집계)

**응답 data**: `JoinStatsResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `total` | `long` |  |
| `joinedToday` | `long` |  |
| `joinedThisMonth` | `long` |  |
| `byStatus` | `List<CodeCount>` |  |
| `byGrade` | `List<CodeCount>` |  |
| `byChannel` | `List<CodeCount>` |  |

##### `GET` `/api/v1/internal/customers/{customerId}`

고객(회원) 상세 — 회원 상세 화면의 데이터원.

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `customerId` | `Long` |

**응답 data**: `CustomerDetailResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `partyId` | `Long` |  |
| `customerGradeCode` | `String` |  |
| `creditRatingCode` | `String` |  |
| `creditEvaluationDate` | `String` |  |
| `customerStatusCode` | `String` |  |
| `email` | `String` |  |
| `phone` | `String` |  |
| `zipCode` | `String` |  |
| `address` | `String` |  |
| `addressDetail` | `String` |  |
| `joinChannelCode` | `String` |  |
| `firstJoinDate` | `String` |  |
| `joinedAt` | `OffsetDateTime` |  |
| `lastTransactionAt` | `OffsetDateTime` |  |
| `dormantAt` | `OffsetDateTime` |  |
| `closedAt` | `OffsetDateTime` |  |
| `partyStatusCode` | `String` |  |
| `genderCode` | `String` |  |
| `nationalityCode` | `String` |  |
| `pep` | `Boolean` |  |

##### `PATCH` `/api/v1/internal/customers/{customerId}/close`

해지(탈퇴) — 다중 필드 → RequestBody

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `customerId` | `Long` |

**요청 본문**: `CloseCustomerRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `closeReasonCode` | `String` | 필수(공백불가) |
| `reasonDetail` | `String` |  |

**응답 data**: `Void`

##### `PATCH` `/api/v1/internal/customers/{customerId}/credit-rating`

신용등급 업데이트 — 다중 필드 → RequestBody

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `customerId` | `Long` |

**요청 본문**: `UpdateCreditRatingRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `ratingCode` | `String` | 필수(공백불가) |
| `evaluationDate` | `String` | 필수(공백불가) |
| `agencyCode` | `String` | 필수(공백불가) |

**응답 data**: `Void`

##### `PATCH` `/api/v1/internal/customers/{customerId}/dormant`

휴면 전환 — 단일 선택적 파람 → RequestParam 유지

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `customerId` | `Long` |

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `reasonDetail` | `String` | - |
| `systemTriggered` | `boolean` | - |

**응답 data**: `Void`

##### `PATCH` `/api/v1/internal/customers/{customerId}/grade`

고객 등급 변경 — 다중 필드 → RequestBody

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `customerId` | `Long` |

**요청 본문**: `ChangeGradeRequest`

| 필드 | 타입 | 제약 |
|---|---|---|
| `newGradeCode` | `String` | 필수(공백불가) |
| `reasonCode` | `String` | 필수(공백불가) |
| `reasonDetail` | `String` |  |
| `systemTriggered` | `boolean` |  |

**응답 data**: `Void`

##### `PATCH` `/api/v1/internal/customers/{customerId}/reactivate`

재활성화(휴면·정지 해제) — 단일 선택적 파람 → RequestParam 유지

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `customerId` | `Long` |

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `reasonDetail` | `String` | - |

**응답 data**: `Void`

##### `PATCH` `/api/v1/internal/customers/{customerId}/suspend`

정지 — 단일 선택적 파람 → RequestParam 유지

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `customerId` | `Long` |

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `reasonDetail` | `String` | - |

**응답 data**: `Void`

### 내부 인증 이벤트

#### InternalAuthEventsController

##### `GET` `/api/v1/internal/auth/{customerId}/events`

최근 windowHours 시간 내 인증서 실패 횟수 등 인증 이벤트 요약(읽기 전용).

**Path 파라미터**

| 이름 | 타입 |
|---|---|
| `customerId` | `Long` |

**Query 파라미터**

| 이름 | 타입 | 필수 |
|---|---|---|
| `windowHours` | `int` | - |

**응답 data**: `AuthEventsResponse`

| 필드 | 타입 | 제약 |
|---|---|---|
| `customerId` | `Long` |  |
| `windowHours` | `int` |  |
| `recentCertFail` | `long` |  |
| `passwordChangedRecently` | `boolean` |  |
