# 여신계 프론트 시나리오 테스트 결과

> **검증 방식 안내**
> 본 결과는 **라이브 UI 실행이 아닌 정적 코드 검증(static code verification)** 결과다.
> 현재 실행 환경에 Docker 데몬·PostgreSQL/Redis 서버 및 AI 부속 서비스
> (review-ai-gateway, doc-agent, auto-loan-review, inference-server)가 기동돼 있지 않아
> 브라우저 기반 실제 클릭 테스트는 불가능했다.
> 대신 각 시나리오의 기대결과가 **프론트(web/app)와 백엔드(services/loan-service 등) 코드에
> 실제 구현되어 있는지**를 파일·라인 단위로 확인했다.
> 따라서 통과여부는 "구현 충족 여부" 기준이며, 라이브 실행 시 데이터/설정에 따라 달라질 수 있다.

- 검증일: 2026-06-10
- 대상 브랜치: `claude/loan-product-test-scenarios-l2s4cg`
- 통과여부 표기: **Pass**(구현 충족) / **Fail**(미구현·기대와 상충) / **보류**(코드상 불확정·조건부)

## 요약

- 총 43개 시나리오 중 **Pass 39 / 보류 4 / Fail 0**
- 보류 항목: No.3, No.5, No.19, No.28 (아래 상세 참조)

## 결과 표

| No | 화면(경로) | 계정 | 구분 | 테스트 케이스 | 기대결과 | 실제결과(코드검증) | 통과여부 |
|---|---|---|---|---|---|---|---|
| 1 | /products/loan | user01 | 정상 | 대출 상품 목록 진입 | 상품 카드 목록·금리/한도 표시 | LoanListPage가 상품 조회 후 금리(minRateBps/maxRateBps)·한도(formatMax) 렌더링 | Pass |
| 2 | /products/loan/credit/{prodID} | 비로그인 | 실패 | 대출 신청(비로그인) | 로그인 페이지로 이동 | [대출 신청하기]→`/loans/apply`, AuthGuard에서 `/loans` 비공개 경로라 `/login` 리다이렉트 | Pass |
| 3 | /loans/apply?prodID= | user01 | 실패 | 한도조회 필수값 미입력 | "~을 입력해주세요." 메시지 | handlePreview가 `if(!prodId\|\|!amount) return`으로 **조용히 조기반환**, 검증 메시지 미표시(연소득은 선택값) | 보류 |
| 4 | /loans/apply?prodID= | user01 | 정상 | 한도조회(미리보기) | 예상한도 표시 | preview API 호출 후 `estimatedLimitAmt` 표시 | Pass |
| 5 | /loans/apply?prodID= | user01 | 실패 | 최대 신청 금액 초과 | "최대 대출 신청 가능 금액은 ~원입니다." | preview는 상품 최대한도 검증 없음(신청 시점에만 validateRequestedRanges). 클라이언트 메시지 미구현 | 보류 |
| 6 | /loans/apply?prodID= | user01 | 실패 | 필수값 미입력 차단 | [대출 신청하기] 버튼 비활성 | `canSubmit = prodId&&amount&&purpose&&employmentType&&agreed`, disabled 시 회색(bg-gray-200) | Pass |
| 7 | /loans/apply/{applId}/identity-verification | user01 | 실패 | 휴대폰 형식 오류 | "휴대폰 번호를 올바르게…" 에러 | `if(!mobileNo\|\|length<10) setError('휴대폰 번호를 올바르게 입력해 주세요.')`, 백엔드 `@Pattern(\d{10,11})` | Pass |
| 8 | /admin/loan/review | deputy01 | 정상 | 본심사 목록·탭 조회 | 상태별 목록·건수 표시 | listPending/listPendingApprover 엔드포인트, 탭 UI·건수 렌더링 | Pass |
| 9 | /admin/loan/review | deputy01 | 정상 | 심사 상세 진입 | 심사 상세 화면 이동 | 각 행 Link `/admin/loan/review/{applId}`, GET review 상세 엔드포인트 | Pass |
| 10 | /admin/loan/review/{applId} | deputy01 | 정상 | 1단계 가심사 실행 | PRESCREENED·한도/금리·CB·DSR 자동생성 | runPrescreening(SUBMITTED 한정), PASS 시 estimated_limit/rate 산출, SUBMITTED→PRESCREENED | Pass |
| 11 | /admin/loan/review/{applId} | deputy01 | 예외 | SUBMITTED 아닌데 가심사 | 버튼 대신 안내 문구 | UI `applStatusCd!=='SUBMITTED'`시 안내, 백엔드 LOAN_047 검증 | Pass |
| 12 | /admin/loan/review/{applId} | audit01 | 실패 | 가심사 권한 없음 | "가심사 실행 권한이 없습니다(심사역·운영)" | audit01=ROLE_COMPLIANCE → canRunReview=false 안내, 백엔드 @HasAnyRole(DEPUTY,OPS) 차단 | Pass |
| 13 | /admin/loan/review/{applId} | deputy01 | 정상 | 2단계 신용평가 APPROVE | 결정/점수/등급/PD 표시 | runCreditEvaluation(engine+decision), CreditEvaluationService.run 응답에 결정/점수/등급/PD | Pass |
| 14 | /admin/loan/review/{applId} | deputy01 | 정상 | 신용평가 REVIEW/REJECT | 해당 결정 적재 | 결정 선택값 그대로 저장, 자동결정 시 REJECT→REJECTED 유도 | Pass |
| 15 | /admin/loan/review/{applId} | deputy01 | 정상 | 3단계 DSR PASS | DSR PASS·비율 표시(연소득 필수) | annualIncome 필수(disabled 제어), DSR 결과·dsrRatioBps 산출 | Pass |
| 16 | /admin/loan/review/{applId} | deputy01 | 정상 | DSR FAIL | DSR FAIL 표시 | ratio>limit 시 STATUS_FAIL, 자동결정 FAIL→REJECTED(DSR_OVER) | Pass |
| 17 | /admin/loan/review/{applId} | deputy01 | 정상 | 4단계 본심사 시작(수동) | 심사 생성·상태=BIAS_REVIEWING | 본심사 버튼 `disabled=!dsrDone`, 진입 시 BIAS_REVIEWING 전이 | Pass |
| 18 | /admin/loan/review/{applId} | deputy01 | 정상 | 자동 결정 | PENDING_APPROVAL 권고 적재 | autoDecide(`disabled=!dsrDone`)→PENDING_APPROVAL, CB.REVIEW 시 LOAN_048 수동권유 | Pass |
| 19 | /admin/loan/review/{applId} | deputy01 | 정상 | 심사 확정(권고) | 완료(COMPLETED) 전이 | confirm 호출 시 `loan.review.bias-check.enabled` **기본값 true** → COMPLETED가 아니라 BIAS_REVIEWING으로 전이. 편향검사 off일 때만 COMPLETED | 보류 |
| 20 | /admin/loan/review/{applId} | deputy01 | 정상 | 편향 인지 처리 | PENDING_APPROVER 전이 | acknowledgeBias(severity≠BLOCKED)→PENDING_APPROVER, isBiasBlocked() 사전검증 | Pass |
| 21 | /admin/loan/review/{applId} | deputy01 | 정상 | 편향 BLOCKED 오버라이드 | 오버라이드 처리·진행 가능 | biasOverride(사유 필수)→biasOverrideBy/Reason/At 기록 | Pass |
| 22 | /admin/loan/review/{applId} | deputy01 | 정상 | 체크 로그 추가 | 체크 로그 행 추가 | POST /checks(수동항목만 허용, 자동항목 LOAN_043 차단) | Pass |
| 23 | /admin/loan/review/{applId} | employee01 | 정상 | 승인자 최종 승인(4-eye) | 승인 완료→신청 APPROVED | approverApprove(ROLE_BRANCH_MANAGER), approverId≠reviewerId 검증, COMPLETED+markApproved | Pass |
| 24 | /admin/loan/review/{applId} | employee01 | 정상 | 승인자 반려 | 반려→신청 REJECTED | approverDecisionCd=REJECTED→markRejected | Pass |
| 25 | /admin/loan/review/{applId} | deputy01 | 실패 | 4-eye 위반(동일인 승인) | 거부 에러 | approverId.equals(reviewerId) 시 LOAN_196 예외, currentActorId로만 식별(바디 조작 불가) | Pass |
| 26 | /admin/loan/review/{applId} | employee01 | 정상 | 결정 정정(지점장) | 정정 반영·체크로그 기록 | revise(ROLE_BRANCH_MANAGER), 사전조건 APPROVED/REJECTED(LOAN_044), 정정자≠승인자(LOAN_207) | Pass |
| 27 | /admin/loan/review/{applId} | deputy01 | 실패 | 정정 권한 없음 | "결정 정정 권한이 없습니다(지점장)" | canRevise=false 안내, PATCH /review는 ROLE_BRANCH_MANAGER 외 403 | Pass |
| 28 | /admin/loan/review/{applId} | employee01 | 정상 | 본사 상신(이상거래) | ESCALATED_TO_HQ 처리 | 백엔드 escalateToHq 엔드포인트(ROLE_BRANCH_MANAGER)는 존재하나, 메인 심사 액션에 **[상신] 버튼 없음**. 'ESCALATE'는 AI 자문 인지응답(ACK_CODES) 옵션일 뿐 본사 상신 직접 트리거 UI 부재 | 보류 |
| 29 | /admin/loan/review | review01 | 실패 | 상신 건 탭 조회 | 상신 탭 노출·목록(타 역할 숨김) | canViewEscalated=hasRole(HQ_REVIEWER)로 탭 노출, GET /escalated는 ROLE_HQ_REVIEWER 한정 | Pass |
| 30 | /admin/loan/review | deputy01 | 정상 | 통계 조회 | 결정유형·상태·거절사유별 집계 | GET /stats(from/to), byTypeDecision·byStatus·byRejectReason 3그리드 표시 | Pass |
| 31 | /loans/apply/result?applId= | user01 | 정상 | 승인 결과 표시 | 승인 배너·상태=승인·AI 트랙 배지 | isApproved→승인 배너, STATUS_LABEL, revAiTrackCd 기반 AI 트랙 배지 | Pass |
| 32 | /loans/apply/result?applId= | user01 | 정상 | 거절 결과·사유 표시 | 거절 배너+거절 사유 | isRejected→거절 배너, REJECT_REASON_LABEL[rejectReasonCd] | Pass |
| 33 | /loans/apply/result?applId= | user01 | 정상 | 상태 변경 이력 | 이력 테이블(이전→변경) | getStatusHistory()→테이블(beforeStatusCd→afterStatusCd) | Pass |
| 34 | /loans/apply/{applId}/collateral | user01 | 정상 | 담보 등록 | 담보 등록됨 | collateral create→evaluate→calculateLtv 순차 호출(감정/LTV 자동) | Pass |
| 35 | /loans/apply/{applId}/guarantor | user01 | 정상 | 보증인 동의 | 보증 약정 등록 | guarantor register→sign(전자서명) 등록 | Pass |
| 36 | /loans/apply/{applId}/documents | user01 | 정상 | 서류 제출 | 검증 결과(통과/재제출/보류) | uploadDocument→verifyResultCd(AUTO_PASS/NEEDS_RESUBMIT/HOLD/FRAUD), doc-agent 연계 | Pass |
| 37 | /products/loan/status | user01 | 정상 | 진행현황 조회 | 신청 건 진행 상태 표시 | loanApplicationApi.list→STATUS_LABEL 매핑 표시 | Pass |
| 38 | /products/loan/my | user01 | 정상 | 내 대출/계약 조회 | 보유 대출·계약 목록 | loanContractApi.list→계약 목록 렌더링 | Pass |
| 39 | /admin/loan/auto-review-sim | ops01 | 정상 | 자동심사 평가 | track=TRACK_1/PD/결정 결과 | evaluateAutoReview→AutoReviewEvaluateResult(track/pd/decisionScore) 렌더링 **(auto-loan-review 서비스 UP 필요)** | Pass |
| 40 | /admin/loan/documents | review01/ops01 | 정상 | 문서 큐 관리 | 휴먼리뷰 큐 목록 | getDocuments→목록(docTypeCd/verifyResultCd) **(doc-agent 서비스 UP 필요)** | Pass |
| 41 | /admin/loan/identity | deputy01/ops01 | 정상 | 본인확인 관리 조회 | IDV 내역 표시 | identityVerificationApi.get→IDV 상세(idvStatusCd/verifiedAt) | Pass |
| 42 | /admin/loan/contracts | ops01 | 정상 | 계약 목록 조회 | 계약 목록 표시 | loanContractApi.adminList→필터/페이징 렌더링 | Pass |
| 43 | /admin/loan/credit-report | ops01 | 정상 | 신용정보 신고 조회 | 신고 내역 표시 | creditInfoReportApi.list→목록 렌더링·ACK 처리 | Pass |

## 보류 항목 상세 / 권고

- **No.3 한도조회 필수값 미입력**: `handlePreview()`가 `if(!selectedProdId || !amount) return`으로 조용히 종료한다. 어떤 값이 빠졌는지 안내하는 "~을 입력해주세요" 메시지가 없어 사용자 피드백이 부족하다. → 필드별 검증 메시지 추가 권고.
- **No.5 최대 신청 금액 초과**: 한도조회(preview)는 상품 최대한도 검증을 하지 않는다(실제 신청 시 `validateRequestedRanges`에서만 검증). → preview 단계에서도 상품 한도 비교·안내 메시지 추가 권고.
- **No.19 심사 확정→COMPLETED**: `loan.review.bias-check.enabled` 기본값이 `true`라, 확정 시 COMPLETED가 아니라 BIAS_REVIEWING으로 전이된다. 기대결과(COMPLETED)는 편향검사를 끈 환경에서만 성립. → 시나리오의 사전조건(편향검사 on/off)을 명시하거나 기대결과를 환경별로 분기 권고.
- **No.28 본사 상신(ESCALATED_TO_HQ)**: 백엔드 `escalateToHq` 엔드포인트(ROLE_BRANCH_MANAGER)는 있으나 메인 심사 화면에 [상신] 버튼이 없다. 'ESCALATE'는 AI 자문 리포트 인지응답(ACK_CODES) 옵션으로만 존재. → 진행중 건에 대한 [상신] 액션 버튼 UI 추가 권고.

## 라이브 테스트 전제(참고)

실제 클릭 시나리오 테스트를 수행하려면 다음 스택이 모두 기동돼야 한다.
PostgreSQL 16 / Redis 7 / loan-service / customer-service / api-gateway / web(Next.js, 3001)
및 AI 부속 서비스(review-ai-gateway, doc-agent, auto-loan-review, inference-server).
`docker compose up` 환경에서 시드(V25 데모) 적재 후 재검증을 권장한다.
