# 회차 상환 동시성 방지 — 설계 결정 기록

## 문제 정의

`RepaymentService.repayInstallment()`는 대출 상환 회차를 DUE/OVERDUE → PAID로 전이한다.
기존 구조는 **조회 → 메모리 확인 → 갱신**의 3단계로 동작했으며,
동일한 결제 요청이 동시에 들어올 경우 **조회와 갱신 사이의 시간 차로 인해 Race Condition이 발생**할 수 있었다.

```
T1: SELECT rsch_status_cd = 'DUE' → isPaid() = false → 검증 통과
T2: SELECT rsch_status_cd = 'DUE' → isPaid() = false → 검증 통과   ← 같은 상태를 동시에 읽음

T1: INSERT repayment_transaction(1,000,000) → schedule PAID 전이
T2: INSERT repayment_transaction(1,000,000) → schedule PAID 전이   ← 중복 결제 발생
```

트랜잭션(READ_COMMITTED 기본값)만으로는 조회 시점의 경쟁 상태를 완전히 막기 어렵다.
두 요청이 동시에 DUE 상태를 읽으면 둘 다 `isPaid() = false` 검증을 통과해 버린다.

---

## 해결 방법

조회와 갱신을 분리하지 않고 **DB 단일 UPDATE 쿼리로 원자적으로 처리**한다.
UPDATE의 WHERE 절에 상태 조건을 포함시켜, 조건을 만족하는 경우에만 실제 변경이 일어나도록 한다.
이후 **rows affected 값을 검증**해 실제 변경된 행이 있을 때만 결제를 계속 진행한다.

```sql
UPDATE repayment_schedule
   SET rsch_status_cd = 'PAID'
 WHERE rsch_id        = ?
   AND rsch_status_cd IN ('DUE', 'OVERDUE')   -- 조건부: 이 시점에 상태가 맞을 때만 변경
   AND deleted_at     IS NULL
```

```
T1: UPDATE ... WHERE status IN ('DUE','OVERDUE') → affected = 1 → 선점 성공 → 결제 진행
T2: UPDATE ... WHERE status IN ('DUE','OVERDUE') → affected = 0 → 이미 PAID  → 에러 반환
```

---

## 해결 과정

### 1. 대안 검토

조건부 UPDATE를 선택하기 전에 아래 4가지 방법을 검토했다.

#### Optimistic Locking — `@Version`

JPA `@Version` 필드를 추가해 UPDATE 시 `WHERE version = ?` 조건을 자동 삽입한다.

```java
// RepaymentSchedule.java
@Version
private Long version;
```

```
T1: SELECT version=0 → UPDATE ... WHERE version=0 → OK (version=1)
T2: SELECT version=0 → UPDATE ... WHERE version=0 → 0 rows → ObjectOptimisticLockingFailureException
```

- JPA 기본 지원, 코드 변경 최소
- 단, 충돌 시 `ObjectOptimisticLockingFailureException` 발생 → 호출자가 catch 후 재시도 루프 필요
- "이미 결제된 회차"와 "동시 경쟁 패배"를 같은 예외로 처리해 에러 분기가 복잡해짐

#### Pessimistic Locking — `SELECT FOR UPDATE`

SELECT 시점에 DB 행 잠금을 획득해 다른 트랜잭션의 접근을 차단한다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<RepaymentSchedule> findByCntrIdAndInstallmentNo...(...);
```

```
T1: SELECT ... FOR UPDATE → 락 획득
T2: SELECT ... FOR UPDATE → T1 커밋까지 대기
T1: UPDATE → PAID → 커밋 → 락 해제
T2: 재진입 → schedule.isPaid() = true → 에러 반환
```

- 강한 보장, 재시도 불필요
- 단, 락을 트랜잭션 전체 기간 동안 유지 → 이자 계산·INSERT 등 비즈니스 로직 실행 내내 다른 요청 차단
- 처리량(Throughput) 저하, 다중 자원 잠금 시 데드락 위험

#### Redis 분산 락

Redis `SETNX`로 논리적 뮤텍스를 구현해 동일 회차에 대한 동시 진입을 차단한다.

```
T1: SETNX repayment:{cntrId}:{installmentNo} → 1 (획득)
T2: SETNX repayment:{cntrId}:{installmentNo} → 0 (실패 → 에러)
T1: 결제 처리 → 락 해제
```

- 다중 앱 인스턴스 환경에서도 동작
- 단, Redis가 **정합성의 의존 대상**이 됨 — Redis 장애 시 결제 불가
- 락 TTL 설정 어려움(너무 짧으면 처리 중 만료, 너무 길면 장애 복구 지연)
- 앱 크래시 시 락 잔류로 회차 영구 잠금 위험

#### DB UNIQUE 제약 + 예외 처리

`repayment_transaction`에 복합 UNIQUE 인덱스를 추가해 중복 INSERT를 DB 레벨에서 거부한다.

```java
catch (DataIntegrityViolationException e) {
    throw new BusinessException(LoanErrorCode.LOAN_091, ...);
}
```

- DB가 최종 방어선 역할, 추가 인프라 의존 없음
- 단, 예외를 정상 흐름 제어에 사용 — 코드 의도가 불분명
- 예외 원인이 중복 결제인지 다른 제약 위반인지 파싱 필요
- 중복 INSERT가 시도된 후에야 차단 → 불필요한 쓰기 후 rollback

### 2. 조건부 UPDATE 선택 근거

| 기준 | 선택 이유 |
|------|-----------|
| **원자성** | 조건 확인과 상태 변경이 DB 단일 쿼리 — 조회/갱신 사이 시간 차 없음 |
| **락 최소화** | 행 잠금을 트랜잭션 전체가 아닌 UPDATE 실행 순간에만 보유 |
| **명확한 의미** | `affected=1` = 선점 성공, `affected=0` = 이미 처리됨 — 분기가 단순 |
| **외부 의존 없음** | Redis·별도 인프라 추가 없이 DB 기능만으로 해결 |
| **재시도 불필요** | 패배한 요청은 즉시 에러 반환, 루프 없음 |

### 3. 구현 및 주의 사항

```java
// RepaymentScheduleRepository — 원자적 상태 전이
@Modifying(clearAutomatically = true)
@Query("""
    UPDATE RepaymentSchedule s
       SET s.rschStatusCd = :newStatus
     WHERE s.rschId        = :rschId
       AND s.rschStatusCd IN :allowedStatuses
       AND s.deletedAt    IS NULL
    """)
int claimStatusChange(@Param("rschId") Long rschId,
                      @Param("newStatus") String newStatus,
                      @Param("allowedStatuses") List<String> allowedStatuses);
```

```java
// RepaymentService — 기존 isPaid() 체크 제거, rows affected 검증으로 교체
String before = schedule.currentStatus();                       // UPDATE 이전에 캡처
int affected = scheduleRepository.claimStatusChange(
        schedule.getRschId(),
        RepaymentSchedule.STATUS_PAID,
        List.of(RepaymentSchedule.STATUS_DUE, RepaymentSchedule.STATUS_OVERDUE));
if (affected == 0) {
    throw new BusinessException(LoanErrorCode.LOAN_091, ...);  // 선점 실패 → 중복 차단
}
```

구현 과정에서 추가로 확인한 사항:

- **`@Modifying(clearAutomatically = true)` 필수**: `@Modifying` 쿼리는 JPA 1차 캐시를 우회한다.
  UPDATE 후 캐시에 남은 엔티티가 stale 상태가 되므로, `clearAutomatically`로 캐시를 비워
  이후 조회가 DB에서 최신 값을 읽도록 보장해야 한다.
- **`before` 상태 캡처 순서**: `claimStatusChange()` 호출 **이전**에 `schedule.currentStatus()`를
  저장해야 한다. UPDATE 후 캐시가 지워지면 상태 이력 기록에 필요한 이전 값을 잃게 된다.

---

## 결과

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| 동시 요청 처리 | SELECT → 메모리 확인 → UPDATE (Race Condition) | 조건부 UPDATE 단일 쿼리 (원자적) |
| 중복 결제 차단 | 미보장 | `affected=0` 즉시 에러 반환으로 차단 |
| 락 유지 범위 | — | UPDATE 실행 순간만 (행 잠금 최소화) |
| 외부 의존 추가 | — | 없음 (DB 기능만 사용) |
| 재시도 로직 | — | 불필요 |

동시 N개 요청이 동일 회차에 들어와도 DB 레벨에서 `affected=1`을 받는 요청 1개만 결제를 완료하고,
나머지는 즉시 에러로 차단된다. 별도 락 없이 단일 쿼리 기반으로 정합성을 보장한다.

---

## 방법 비교 요약

| 방법 | 원자성 | 락 점유 범위 | 외부 의존 | 재시도 필요 | 코드 복잡도 |
|------|--------|-------------|-----------|------------|-------------|
| Optimistic Locking | △ 충돌 후 감지 | 없음 | 없음 | 필요 | 중 |
| Pessimistic Locking | ◎ | 트랜잭션 전체 | 없음 | 불필요 | 낮음 |
| Redis 분산 락 | ◎ | TTL 동안 | Redis | 불필요 | 높음 |
| UNIQUE 제약 + 예외 | ◎ 사후 차단 | 없음 | 없음 | 불필요 | 중 |
| **조건부 UPDATE** | **◎** | **UPDATE 순간만** | **없음** | **불필요** | **낮음** |

---

## 관련 커밋

- `f0c7dba` refactor(repayment): 회차 PAID 전이를 조건부 UPDATE rows affected 검증으로 교체
