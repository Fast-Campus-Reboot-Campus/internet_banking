# 대출 심사(Loan Review) 플로우

loan-service의 대출 심사 도메인 전체 흐름입니다. 신청 → 사전심사 → 심사(자동/수동) → 편향성 검증 → 승인자 결재 → 계약/실행으로 이어지는 라이프사이클과, 4-eye 원칙·HQ 에스컬레이션·타임아웃 만료 등 운영 안전장치를 포함합니다.

---

## 1. 전체 라이프사이클 (신청 → 실행)

```mermaid
flowchart TD
    SUB["신청 접수<br/>(SUBMITTED)"] --> PRE["사전심사 통과<br/>(PRESCREENED)"]

    PRE --> GATE{"심사 진입 게이트<br/>CB · DSR · IDV · LTV · 보증인 · 서류"}
    GATE -->|"미충족"| HOLD["진입 차단<br/>(PRESCREENED 유지)"]
    GATE -->|"충족"| PATH{"심사 방식"}

    PATH -->|"수동 (REVIEWER)"| RUN["POST /review<br/>run()"]
    PATH -->|"자동 (배치)"| AUTO["POST /auto-decide<br/>autoDecide()"]

    RUN --> RD
    AUTO --> PA["PENDING_APPROVAL<br/>(자동 추천, 리뷰어 확인 대기)"]
    PA -->|"POST /confirm"| RD["REVIEWER_DECIDED"]

    RD --> BIASQ{"편향성 검증<br/>활성화?"}
    BIASQ -->|"No"| DONE
    BIASQ -->|"Yes"| BR["BIAS_REVIEWING<br/>(편향성 에이전트 분석)"]

    BR --> SEV{"bias 심각도"}
    SEV -->|"BLOCKED"| OVR["시니어 승인자 override<br/>POST /bias-override (4-eye)<br/>또는 Ops note"]
    OVR --> ACK
    SEV -->|"NONE~HIGH"| ACK["POST /acknowledge-bias<br/>acknowledgeBias()"]

    ACK --> PAP["PENDING_APPROVER<br/>(승인자 결재 대기)"]
    PAP -->|"POST /approver-approve<br/>(approverId ≠ reviewerId)"| DONE["COMPLETED"]

    DONE --> DEC{"최종 결정"}
    DEC -->|"APPROVED"| APP["신청 APPROVED<br/>LoanApprovedEvent 발행"]
    DEC -->|"REJECTED"| REJ["신청 REJECTED"]

    APP --> CNT["계약 체결<br/>(CONTRACTED)"]
    CNT --> EXE["대출 실행/입금<br/>(LoanExecution)"]

    classDef terminal fill:#d4edda,stroke:#28a745,color:#155724
    classDef reject fill:#f8d7da,stroke:#dc3545,color:#721c24
    class APP,CNT,EXE terminal
    class REJ,HOLD reject
```

---

## 2. 심사 상태 머신 (LoanReview.revStatusCd)

```mermaid
stateDiagram-v2
    [*] --> PENDING_APPROVAL: autoDecide() (AUTO)
    [*] --> REVIEWER_DECIDED: run() (MANUAL)

    PENDING_APPROVAL --> REVIEWER_DECIDED: confirm()
    PENDING_APPROVAL --> EXPIRED: expirePending (>7d)

    REVIEWER_DECIDED --> BIAS_REVIEWING: 편향성 검증 ON
    REVIEWER_DECIDED --> COMPLETED: 편향성 검증 OFF

    BIAS_REVIEWING --> BIAS_REVIEWING: biasOverride() / bias-ops-note
    BIAS_REVIEWING --> PENDING_APPROVER: acknowledgeBias()
    BIAS_REVIEWING --> EXPIRED: expireBiasReviewing (>14d)

    PENDING_APPROVER --> COMPLETED: approverApprove() (4-eye)
    PENDING_APPROVER --> EXPIRED: expirePendingApprover (>7d)

    REVIEWER_DECIDED --> ESCALATED_TO_HQ: escalateToHq()
    BIAS_REVIEWING --> ESCALATED_TO_HQ: escalateToHq()
    PENDING_APPROVER --> ESCALATED_TO_HQ: escalateToHq()
    PENDING_APPROVAL --> ESCALATED_TO_HQ: escalateToHq()

    COMPLETED --> COMPLETED: revise() (APPROVED↔REJECTED, 계약 전까지)

    COMPLETED --> [*]
    EXPIRED --> [*]
    ESCALATED_TO_HQ --> HQ: ROLE_HQ_REVIEWER 검토
```

---

## 3. 역할(Actor)과 권한

```mermaid
flowchart LR
    REVIEWER["REVIEWER<br/>여신 심사역"] -->|"run / confirm<br/>acknowledge-bias / revise"| R1[" "]
    APPROVER["APPROVER<br/>승인 책임자"] -->|"approver-approve<br/>bias-override"| R2[" "]
    BM["BRANCH_MANAGER<br/>지점장"] -->|"escalate-to-hq"| R3[" "]
    HQ["HQ_REVIEWER<br/>본부 심사"] -->|"escalated 조회"| R4[" "]
    OPS["OPS<br/>운영"] -->|"bias-ops-note<br/>배치 만료"| R5[" "]

    style R1 fill:none,stroke:none
    style R2 fill:none,stroke:none
    style R3 fill:none,stroke:none
    style R4 fill:none,stroke:none
    style R5 fill:none,stroke:none
```

**핵심 통제 — 4-eye 원칙 (이중 확인)**
- `approver-approve`: 결재자(approverId) ≠ 심사역(reviewerId) — `LOAN_196`
- `bias-override`: override 주체 ≠ 심사역(reviewerId) — `LOAN_200`

---

## 4. 상태 코드 레퍼런스

| 상태 (`revStatusCd`) | 의미 |
|---|---|
| `PENDING_APPROVAL` | 자동 추천됨, 리뷰어 확인 대기 |
| `REVIEWER_DECIDED` | 리뷰어 결정 완료, 편향성 검증 전 |
| `BIAS_REVIEWING` | 공정성/규제 편향성 검증 중 |
| `PENDING_APPROVER` | 편향성 통과, 승인자 결재 대기 |
| `COMPLETED` | 최종 확정 (승인 또는 거절) |
| `EXPIRED` | 단계별 타임아웃 만료 |
| `ESCALATED_TO_HQ` | 사기/AML 의심 → 본부 에스컬레이션 |

| 결정 (`revDecisionCd`) | 승인자 결정 (`approvedDecisionCd`) | 편향성 (`biasSeverityCd`) |
|---|---|---|
| `APPROVED` / `REJECTED` | `APPROVE_AS_IS` / `OVERRIDE_APPROVED` / `OVERRIDE_REJECTED` | `BLOCKED` / `HIGH` / `MEDIUM` / `LOW` / `NONE` |

---

## 5. 심사 진입 게이트 (run / autoDecide 전제조건)

심사를 시작하려면 신청이 `PRESCREENED` 상태이면서 아래를 모두 충족해야 합니다.

1. CB(신용평가) 결정 ≠ `REJECT`
2. DSR 상태 = `PASS`
3. IDV(본인확인) = `PASS`
4. 담보 상품이면 모든 담보 LTV = `PASS`
5. 보증 상품이면 서명 보증인 수 ≥ 최소 기준
6. 제출 서류 클리어 (자동/리뷰어 통과)
