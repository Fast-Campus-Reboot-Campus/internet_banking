# 관리자 콘솔 역할 모델 재설계 (AdminRole → BankRole 단일화)

## 배경 / 문제

관리자 콘솔 프론트에 **두 개의 역할 어휘**가 공존했다.

- `BankRole` (`common.BankRole`, JWT `roles`) — 백엔드·게이트웨이의 실제 권한 어휘
- `AdminRole` (`web/lib/admin-mock-data.ts`) — 콘솔 표시용 레거시 7값
  (`ROLE_HQ_AUDIT`/`HQ_REVIEW`/`HQ_RISK`/`HQ_MARKETING`/`PRIMARY_OWNER`/`BRANCH_STAFF`/`OTHER_BRANCH`)

`AdminRole` 은 BankRole 과 1:1 이 아니라 다음 문제를 낳았다.

1. **지점장(`BRANCH_MANAGER`) 전용 표시값이 없음** → 지점장이 화면상 "담당 직원(PRIMARY_OWNER)"으로 표시.
2. `PRIMARY_OWNER`(담당)·`OTHER_BRANCH`(타지점)는 **정적 직급이 아니라 동적 관계**(담당 고객 보유 / 지점 일치)인데 역할로 박혀 있음.
3. `HQ_AUDIT`↔`COMPLIANCE`, `HQ_REVIEW`↔`HQ_REVIEWER` 등 어휘 불일치로 매핑 함수(`GRADE_TO_ADMIN_ROLE`)가 근사치를 생성.

## 결정

**역할 어휘를 `BankRole` 단일로 통일한다.** 대출 어드민 화면이 이미 쓰는 패턴
(`getAdminRoles()` + `hasAnyRole(roles, BankRole.X)`)을 고객측 화면에도 적용한다.

- **동적 관계는 역할이 아니라 계산으로 처리** (결정: 지점 비교 방식)
  - `타지점(OTHER_BRANCH)` = 본사 직군이 아니고 `직원 지점 ≠ 대상 고객 지점` → `isOtherBranch()`
  - `담당(PRIMARY_OWNER)` = `party_relation`(담당 고객 보유) 연동 전까지 별도 판정 없이 **지점 스코프로 근사**. 추후 백엔드 연동 시 계산값으로 복원.
- 표시 라벨은 `BANK_ROLE_LABEL`(BankRole→한글), 배지는 `primaryRoleLabel()`.

## 단계

- **Phase 1 (완료)**: `admin-auth.ts` 에 라벨맵·지점맵·접근정책 헬퍼
  (`isHeadOffice`·`isMaskingRole`·`canViewAuditLog`·`requiresReason`·`isOtherBranch`·`primaryRoleLabel`·`branchLabel`) 추가. 순수 추가라 기존 코드 불변.
- **Phase 2**: `customers`·`dashboard`·`audit-log`·`AdminSidebar`·`login`·`AdminGuard` 을 헬퍼 기반으로 전환. `admin_user` 에서 `role: AdminRole` 제거, `admin_roles`(BankRole[]) + branch 로 게이팅.
- **Phase 3**: `AdminRole` 타입·`ROLE_LABELS`·레거시 정책함수(`applyMasking`/`canViewAuditLog`/`requiresReason` in `admin-mock-data`) 제거.

## 영향

- 지점장이 "지점장"으로 정확히 표시됨. 어휘가 백엔드와 일치해 매핑 근사치 제거.
- `admin_user` 형태 변경 → 콘솔 화면 일괄 수정 필요(Phase 2). `admin_roles` localStorage 계약은 유지.
- `담당` 정밀 판정은 `party_relation` 백엔드 연동 과제로 남김.
