# 요구사항 명세서 (Requirements Specification)

> **프로젝트**: Internet Banking MVP
> **버전**: 0.0.1-SNAPSHOT
> **최종 수정일**: 2026-06-11
> **패키지 루트**: `com.bank`

본 문서는 Internet Banking MVP 플랫폼의 기능·비기능 요구사항을 정리한 명세서다.
공통 AI 가이드는 [`AI_GUIDELINES.md`](AI_GUIDELINES.md)를, 상세 설계는 각 도메인 문서를 따른다.

---

## 1. 개요

### 1.1 목적
MSA(마이크로서비스 아키텍처) 기반 인터넷뱅킹 플랫폼으로, **수신(예금)·여신(대출)·결제(이체)·고객·상담** 도메인을 독립 서비스로 분리하고, AI/RAG 기반 자동심사·유사사례 검색·편향 검증·이상거래 조사를 통합한다.

### 1.2 범위
- 고객 채널: 계좌 조회/이체, 예금 상품 가입·해지, 대출 신청, 챗봇 상담
- 직원 채널(어드민): 고객 관리, 대출 본심사, AI 감사, 상담사 채팅, 감사 로그
- AI 에이전트: 대출 자동심사, 서류 심사, 자문 리포트, 이상거래 조사, 챗봇

### 1.3 용어
| 용어 | 설명 |
|---|---|
| 당행 이체 (INTERNAL) | 자행 계좌 간 이체, 동기 처리 |
| 타행 이체 (EXTERNAL) | 외부 은행 이체, KFTC/BOK 망 비동기 청산 |
| DSR | Debt Service Ratio, 총부채원리금상환비율 |
| LTV | Loan-to-Value, 담보인정비율 |
| PD | Probability of Default, 부도확률 |
| 4-Eye 원칙 | 검토자와 승인자를 분리하는 이중 확인 통제 |
| RAG | Retrieval-Augmented Generation, 검색 증강 생성 |
| HITL | Human-in-the-Loop, 사람 개입 승인 |

---

## 2. 시스템 구성

### 2.1 마이크로서비스 목록
| 서비스 | 포트 | 언어/스택 | 역할 |
|---|---|---|---|
| gateway-service | 8080 | Java / Spring Cloud Gateway | API 게이트웨이, 라우팅, 인증 검증 |
| customer-service | 8081 | Java / Spring Boot | 고객 정보, 인증, 권한, FDS |
| deposit-service | 8082 | Java / Spring Boot | 예금 상품·계약·계좌·거래, 상품 추천 |
| deposit-api | 8082(proxy) | Python / FastAPI | 이체 실행·소유주 검증 경량 레이어 |
| loan-service | 8083 | Java / Spring Boot + pgvector | 대출 생애주기, RAG 검색 |
| payment-service | 8084 | Java / Spring Boot | 타행이체 청산 (KFTC/BOK), Outbox/Saga |
| master-service | 8085 | Java / Spring Boot | 공통 코드·마스터 데이터 |
| advisory-service | 8085 | Java / Spring Boot | 심사역 자문 리포트 (CRITICAL 게이트) |
| auto-loan-review | 8089 | Java / Spring Boot + pgvector | ML 자동심사 + LLM 리포트 + 편향 검증 |
| doc-agent | 8087 | Java / Spring Boot | 대출 서류 심사 (OCR·위변조·라우팅) |
| consultation-service | 8087 | Python / FastAPI | 챗봇·상담사 채팅, 챗봇 이체 |
| fraud-investigation-agent | 8090 | Python / LangGraph | 이상거래 조사, HITL 승인 |
| inference-server | — | Python | OCR·위변조·필드 추출 모델 서빙 |
| web | 3001 | Next.js 15 / TypeScript | 고객·어드민 통합 프런트엔드 |

### 2.2 기술 스택
- **백엔드**: Java 17, Spring Boot 3.3.x, Gradle 멀티모듈 / Python 3.11, FastAPI, SQLAlchemy, LangGraph
- **데이터**: PostgreSQL 16 (서비스별 독립 DB), pgvector(RAG), Redis 7, Apache Kafka 3.8
- **AI/LLM**: Spring AI, OpenAI GPT-4o-mini, Ollama Qwen2.5:3b
- **프런트엔드**: Next.js 15, React 18, TypeScript, TanStack Query, Tailwind CSS, Shadcn
- **모니터링**: Prometheus, Grafana, Loki/Promtail
- **인프라**: Docker, Docker Compose, Confluent Schema Registry

---

## 3. 인증 및 권한 요구사항

### 3.1 로그인 (FR-AUTH)
| ID | 요구사항 |
|---|---|
| FR-AUTH-01 | 고객·직원은 ID/비밀번호로 로그인할 수 있다 (`POST /api/v1/auth/login`). |
| FR-AUTH-02 | 고객은 금융인증서(PIN 기반)로 로그인할 수 있다 (`POST /api/v1/auth/cert-login`, SSR 프록시). |
| FR-AUTH-03 | 고객/직원 구분은 employee 테이블의 `grade_code` 존재 여부로 판정한다. |
| FR-AUTH-04 | 인증 성공 시 JWT를 발급하며 `roles` 클레임에 BankRole 권한 배열을 포함한다. |
| FR-AUTH-05 | 로그인 시도(LoginAttempt)를 기록하고, 이상 패턴을 FDS로 탐지한다. |
| FR-AUTH-06 | 등록 디바이스(RegisteredDevice)·모바일 인증(MobileAuth)을 관리한다. |

### 3.2 권한 모델 (BankRole)
| 역할 | 구분 | 설명 |
|---|---|---|
| `CUSTOMER` | 고객 | 일반 고객 |
| `TELLER` | 지점 | 창구원 |
| `DEPUTY_MANAGER` | 지점 | 부지점장 |
| `BRANCH_MANAGER` | 지점 | 지점장 |
| `HQ_REVIEWER` | 본사 | 여신 심사역 |
| `HQ_RISK` | 본사 | 리스크 담당 |
| `HQ_MARKETING` | 본사 | 마케팅 |
| `COMPLIANCE` | 본사 | 컴플라이언스 |
| `OPS` | 본사 | 운영 |
| `INTERNAL` | 본사 | 내부 |
| `ADMIN` | 시스템 | 전 권한 |

### 3.3 통제 원칙
| ID | 요구사항 |
|---|---|
| FR-AUTH-07 | 대출 본심사는 4-Eye 원칙을 적용한다 (검토자 ≠ 승인자). |
| FR-AUTH-08 | 편향 override 주체는 검토자와 달라야 한다. |
| FR-AUTH-09 | 화면별 노출은 역할(`bankRoles`) 기반으로 제어한다. |

---

## 4. 고객 도메인 요구사항 (FR-CUST)

| ID | 요구사항 |
|---|---|
| FR-CUST-01 | 고객은 본인 정보(`GET /customers/me`)를 조회할 수 있다. |
| FR-CUST-02 | 직원은 고객 상세(`GET /customers/{customerId}`)를 조회할 수 있다. |
| FR-CUST-03 | 시스템은 본인확인(IdentityVerification) 결과를 기록한다. |
| FR-CUST-04 | 시스템은 이상거래 탐지(FdsDetection/FdsIncident)를 수행한다. |
| FR-CUST-05 | 인증 실패 이벤트를 내부 API로 제공한다 (fraud-agent 연동). |

---

## 5. 예금 도메인 요구사항 (FR-DEP)

### 5.1 상품 조회·가입
| ID | 요구사항 |
|---|---|
| FR-DEP-01 | 예금/적금/청약 상품을 타입·상태·저축유형으로 필터링 조회한다. |
| FR-DEP-02 | 상품별 가입 조건(가입금액·기간·금리·대상 그룹)을 제공한다. |
| FR-DEP-03 | 가입 대상 그룹을 연령(min_age/max_age)·직군 기준으로 제한한다 (예: 청년 19~34, 국군 18~27). |
| FR-DEP-04 | 고객은 상품 계약을 생성·조회할 수 있다. |
| FR-DEP-05 | 계약 상태(ACTIVE/MATURED/TERMINATED)를 변경(만기·해지)할 수 있다. |
| FR-DEP-06 | 계약 적용 금리는 기본금리(BASE)와 우대금리(PREFERENTIAL)를 합산한다. |

### 5.2 상품 추천 (현금흐름 기반, 100점 채점)
| 항목 | 배점 | 기준 |
|---|---|---|
| 재정 적합도 | 40점 | 예금: 잔액/최소가입금액, 적금: 월저축액/(최소가입금액×2) |
| 예상 수익(ROI) | 30점 | 금리×기간 세전 이자, 후보 풀 내 정규화 |
| 유동성 매칭 | 20점 | 거래 빈도 vs 만기 적합도, 조기해지 허용 시 +2점 |
| 부가 혜택 | 10점 | 비과세 +6점, 중도해지 가능 +4점 |

### 5.3 계좌·거래
| ID | 요구사항 |
|---|---|
| FR-DEP-07 | 고객 계좌를 타입별(예금/적금/청약/입출금)로 조회한다. |
| FR-DEP-08 | 계좌 상태(ACTIVE/DORMANT/CLOSED)를 관리한다. |
| FR-DEP-09 | 매월 이자 지급 내역(InterestHistory)을 기록한다. |
| FR-DEP-10 | 계좌별 거래 내역을 조회한다. |

### 5.4 이체
| ID | 요구사항 |
|---|---|
| FR-DEP-11 | 당행 이체(INTERNAL)는 동기 처리한다 (deposit-api). |
| FR-DEP-12 | 이체는 멱등성 키(idempotency_key, UUID v4)로 중복 제출을 방지한다. |
| FR-DEP-13 | 이체 시 소유주 검증(X-Customer-Id)을 수행한다. |
| FR-DEP-14 | 이체는 KST 기준 일일 누적 금액·횟수 한도를 검증한다. 초과 시 `DAILY_TRANSFER_AMOUNT_EXCEEDED`/`DAILY_TRANSFER_COUNT_EXCEEDED` 반환. |

---

## 6. 대출 도메인 요구사항 (FR-LOAN)

### 6.1 신청·가심사
| ID | 요구사항 |
|---|---|
| FR-LOAN-01 | 고객은 금액·기간·용도·상환방식으로 대출을 신청한다. |
| FR-LOAN-02 | 신청 시 가심사(CreditScoreEngine)가 자동 실행된다. |
| FR-LOAN-03 | 가심사 통과 시 CB 신용평가가 자동 연쇄 실행된다. |
| FR-LOAN-04 | 신용평가 완료 시 DSR 산출이 자동 연쇄 실행된다. |

### 6.2 검증 게이트 (본심사 진입 전 필수)
| ID | 조건 |
|---|---|
| FR-LOAN-05 | CB(신용평가) 결정 ≠ REJECT |
| FR-LOAN-06 | DSR 상태 = PASS |
| FR-LOAN-07 | IDV(본인확인) = PASS |
| FR-LOAN-08 | 담보 상품인 경우 LTV = PASS |
| FR-LOAN-09 | 보증 상품인 경우 보증인 서명 수 ≥ 기준 |
| FR-LOAN-10 | 서류 클리어 (자동 또는 리뷰어 통과) |

### 6.3 본심사 (상태 머신)
```
SUBMITTED → PRESCREENED → 심사(수동/자동) → REVIEWER_DECIDED → BIAS_REVIEWING → PENDING_APPROVER → COMPLETED
```
| ID | 요구사항 |
|---|---|
| FR-LOAN-11 | 수동/자동 심사를 수행할 수 있다 (auto-loan-review 연동). |
| FR-LOAN-12 | 리뷰어 확정 후 편향 검토(BIAS_REVIEWING)를 거친다. |
| FR-LOAN-13 | 승인자 결재는 검토자와 달라야 한다 (4-Eye). |
| FR-LOAN-14 | 자문 리포트(advisory)의 CRITICAL 신호가 미확인이면 승인을 차단한다. |

### 6.4 계약·실행·상환
| ID | 요구사항 |
|---|---|
| FR-LOAN-15 | 심사 통과 시 대출 계약(약정)을 생성한다. |
| FR-LOAN-16 | 대출 실행(입금)을 처리한다. |
| FR-LOAN-17 | 상환 스케줄·상환 거래(원리금·연체이자)를 관리한다. |
| FR-LOAN-18 | 금리 변경·만기·해지·연체를 추적한다. |

---

## 7. 결제·타행이체 요구사항 (FR-PAY)

| ID | 요구사항 |
|---|---|
| FR-PAY-01 | 타행 이체(EXTERNAL)는 비동기로 처리한다 (202 Accepted). |
| FR-PAY-02 | 금액 < 10억은 KFTC(금융결제원), ≥ 10억은 BOK(한국은행) 망으로 라우팅한다. |
| FR-PAY-03 | 송신 계좌 검증·출금 분개(Ledger)를 수행한다. |
| FR-PAY-04 | Outbox 패턴으로 이벤트 발행을 보장한다. |
| FR-PAY-05 | 실패 시 Saga 보상으로 롤백(REVERSING)한다. |
| FR-PAY-06 | 멱등성 키(X-Idempotency-Key)로 중복을 방지한다. |
| FR-PAY-07 | 상태 전이: CLEARING → COMPLETED / REVERSING |

---

## 8. 상담·챗봇 요구사항 (FR-CHAT)

| ID | 요구사항 |
|---|---|
| FR-CHAT-01 | 시나리오형 챗봇이 의도를 분류하고 기능 코드로 라우팅한다. |
| FR-CHAT-02 | 상품 안내·금리·가입조건·약관 검색(TERMS_RAG)·FAQ를 제공한다. |
| FR-CHAT-03 | 인증 고객에게 내 계좌·가입 상품·계약·만기·이자·현금흐름을 제공한다. |
| FR-CHAT-04 | 현금흐름 기반 상품 추천(CASH_FLOW_RECOMMEND, 100점 채점)을 제공한다. |
| FR-CHAT-05 | 저축 목표(SAVINGS_GOAL) 멀티턴 에이전트를 제공한다. |
| FR-CHAT-06 | 챗봇에서 이체(`POST /chatbot/transfer`)를 실행할 수 있다 (deposit-api 호출). |
| FR-CHAT-07 | 챗봇 → 상담사 핸드오프 및 실시간 상담사 채팅을 지원한다. |
| FR-CHAT-08 | 직원용 기능(고객/계약/계좌/이체흐름/상담이력/현금흐름)을 제공한다. |
| FR-CHAT-09 | 미등록 의도는 LLM 폴백으로 응답한다. |

---

## 9. AI 에이전트 요구사항 (FR-AI)

### 9.1 자동심사 (auto-loan-review)
| ID | 요구사항 |
|---|---|
| FR-AI-01 | Rule Engine이 하드제약·정책 매트릭스로 Track(자동승인/자동거절/심사역판단)을 결정한다. |
| FR-AI-02 | ML 모델이 decision + PD(부도확률)를 예측한다 (inference-server). |
| FR-AI-03 | LLM 파이프라인(@Async)이 목적분석 → 심사리포트(RAG) → 선심사를 수행한다. |
| FR-AI-04 | 편향 검증으로 성별·나이·지역 편향을 검사한다. BLOCKED 시 4-Eye override 필요. |
| FR-AI-05 | PSI 드리프트(주간, 경고 0.10/심각 0.20)·공정성(월간, 승인률 편차 > 0.05)을 모니터링한다. |
| FR-AI-06 | 자동심사 의견은 감사 기록(AgentAuditRecord, append-only)으로 추적한다. |

### 9.2 서류 심사 (doc-agent)
| ID | 요구사항 |
|---|---|
| FR-AI-07 | 5단계 파이프라인(Ingest → Classify → OCR → Extract → Forgery → Verify)을 수행한다. |
| FR-AI-08 | OCR 입력 전 PII(주민번호·전화·계좌)를 마스킹한다. |
| FR-AI-09 | 위변조 점수 기반 자동 라우팅: < 0.3 AUTO_PASS, 0.3~0.7 NEEDS_RESUBMIT, ≥ 0.7 HOLD. |

### 9.3 자문 리포트 (advisory-service)
| ID | 요구사항 |
|---|---|
| FR-AI-10 | RAG 기반 자문 신호를 생성하고 심각도(INFO/WARN/CRITICAL)를 부여한다. |
| FR-AI-11 | CRITICAL 미확인 시 승인을 차단한다 (LOAN_201). |

### 9.4 유사사례 검색 (RAG)
| ID | 요구사항 |
|---|---|
| FR-AI-12 | 심사 완료 건을 청크·임베딩하여 저장한다. |
| FR-AI-13 | 검색 백엔드를 inline(pgvector) 또는 es(Elasticsearch)로 구성한다. |

### 9.5 이상거래 조사 (fraud-investigation-agent)
| ID | 요구사항 |
|---|---|
| FR-AI-14 | 5개 시나리오(보이스피싱·계정탈취·자금세탁·내부자부정·정상)를 동시 분석한다. |
| FR-AI-15 | 에이전트는 권고까지만 생성하고, 분석가 승인(HITL) 후 지급정지·STR 등 실제 동작을 실행한다. |
| FR-AI-16 | 인증 실패 이벤트는 customer-service 실데이터를 조회한다. |

---

## 10. 비기능 요구사항 (NFR)

| 구분 | ID | 요구사항 |
|---|---|---|
| 신뢰성 | NFR-01 | 이체·결제는 멱등성 키로 중복 처리를 방지한다. |
| 신뢰성 | NFR-02 | 타행이체는 Outbox/Saga로 이벤트 발행과 보상을 보장한다. |
| 보안 | NFR-03 | 인증은 JWT 기반이며 권한은 BankRole로 제어한다. |
| 보안 | NFR-04 | 중요 판단(대출 승인·편향 override)은 4-Eye로 통제한다. |
| 보안 | NFR-05 | LLM 입력 전 PII를 마스킹한다. |
| 감사 | NFR-06 | AI 심사 의견은 append-only 감사 기록으로 보존한다. |
| 확장성 | NFR-07 | 서비스는 도메인별 독립 DB를 사용하는 MSA로 구성한다. |
| 관측성 | NFR-08 | Prometheus·Grafana·Loki로 메트릭·로그를 수집한다. |
| 성능 | NFR-09 | LLM 심사 파이프라인은 비동기(@Async)로 처리해 응답 지연을 분리한다. |
| 데이터 | NFR-10 | 스키마 변경은 Flyway 마이그레이션으로 관리한다. |

---

## 11. 외부 연동

| 연동 대상 | 목적 | 방식 |
|---|---|---|
| KFTC (금융결제원) | 타행이체 청산 (< 10억) | Kafka 클러스터 |
| BOK (한국은행) | 거액 결제 (≥ 10억) | Kafka 클러스터 |
| OpenAI / Ollama | LLM 추론 (심사·챗봇·서류) | REST |
| inference-server | OCR·위변조·필드 추출·PD 모델 | REST |
| MinIO | 서류 객체 저장 | S3 호환 |

---

## 12. 관련 문서

| 문서 | 경로 |
|---|---|
| 공통 AI 가이드 | [`AI_GUIDELINES.md`](AI_GUIDELINES.md) |
| 대출 AI 에이전트 흐름 | [`loan-ai-agents-flow.md`](loan-ai-agents-flow.md) |
| 대출 심사 플로우 | [`loan-review-flow.md`](loan-review-flow.md) |
| 대출 ERD | [`loan_erd.md`](loan_erd.md) |
| 데이터 사전 | [`data_dictionary.md`](data_dictionary.md) |
| 예금·결제 API 명세 | [`deposit-payment-api-spec.md`](deposit-payment-api-spec.md) |
| 인증·보안 DDL 설계 | [`auth_security_ddl_design.md`](auth_security_ddl_design.md) |
| 자문 연동 가이드 | [`advisory-integration-guide.md`](advisory-integration-guide.md) |
| 배포 가이드 | [`DEPLOY.md`](DEPLOY.md) |
| 모니터링 가이드 | [`monitoring/`](monitoring/) |
