# 회차 상환 동시성 방지 — 설계 결정 기록

## 배경

`RepaymentService.repayInstallment()`는 상환 회차를 DUE/OVERDUE → PAID로 전이한다.
기존 구조는 **조회 → 메모리 확인 → 갱신** 순서로 동작했다.

```
T1: SELECT rsch_status_cd = 'DUE'   → isPaid() = false → 통과
T2: SELECT rsch_status_cd = 'DUE'   → isPaid() = false → 통과
T1: INSERT repayment_transaction(1,000,000) → PAID 전이
T2: INSERT repayment_transaction(1,000,000) → PAID 전이  ← 중복 결제
```

트랜잭션(READ_COMMITTED 기본값) 만으로는 **조회 시점 경쟁 상태**를 완전히 막기 어렵다.
동일 회차에 대해 두 요청이 동시에 DUE 상태를 읽으면 둘 다 검증을 통과한다.

---

## 고려한 방법들

### 1. Optimistic Locking — `@Version`

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

**장점**
- JPA 기본 지원, 코드 변경 최소
- 엔티티 상태와 DB 상태가 항상 일치

**단점**
- 충돌 시 `ObjectOptimisticLockingFailureException` 발생 → 호출자가 catch + 재시도 루프 필요
- "이미 결제된 회차"와 "동시 경쟁 패배"를 같은 예외로 처리 → 에러 분기가 복잡해짐
- 재시도 중 또 충돌하면 반복 실패 가능

### 2. Pessimistic Locking — `SELECT FOR UPDATE`

SELECT 시점에 DB 행 잠금을 획득해 다른 트랜잭션의 접근을 차단한다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<RepaymentSchedule> findByCntrIdAndInstallmentNo...(...);
```

```
T1: SELECT ... FOR UPDATE → 락 획득
T2: SELECT ... FOR UPDATE → T1이 커밋할 때까지 대기
T1: UPDATE → PAID → 커밋 → 락 해제
T2: 재진입 → schedule.isPaid() = true → 에러 반환
```

**장점**
- 강한 보장, 재시도 불필요
- 직관적인 흐름

**단점**
- 락을 트랜잭션 전체 기간 동안 유지 → 이자 계산, INSERT 등 비즈니스 로직 실행 내내 다른 요청을 차단
- 처리량(Throughput) 저하, 특히 이자 계산이 느릴 경우 병목
- 다중 자원 잠금 시 데드락 위험

### 3. Redis 분산 락

Redis `SETNX`로 논리적 뮤텍스를 구현해 동일 회차에 대한 동시 진입을 차단한다.

```
T1: SETNX repayment:{cntrId}:{installmentNo} → 1 (획득)
T2: SETNX repayment:{cntrId}:{installmentNo} → 0 (실패 → 대기 또는 에러)
T1: 결제 처리 → 락 해제
```

**장점**
- 다중 앱 인스턴스 환경에서도 동작
- DB 락보다 가볍고 빠름

**단점**
- Redis가 **정합성의 의존 대상**이 됨 (캐시 장애 시 결제 불가)
- 락 TTL 설정 어려움: 너무 짧으면 처리 중 만료, 너무 길면 장애 복구 지연
- 앱 크래시 시 락 잔류 가능 → 회차 영구 잠금 위험
- 코드베이스에 Redis 의존이 없는 결제 경로에 인프라 의존성 추가

### 4. DB UNIQUE 제약 + 예외 처리

`repayment_transaction` 에 `(rsch_id, rtx_type_cd)` 복합 UNIQUE 인덱스를 추가해
중복 INSERT를 DB 레벨에서 거부하고 예외를 포착한다.

```java
catch (DataIntegrityViolationException e) {
    throw new BusinessException(LoanErrorCode.LOAN_091, ...);
}
```

**장점**
- DB가 최종 방어선 역할
- 추가 런타임 의존성 없음

**단점**
- 예외를 정상 흐름 제어에 사용 — 코드 의도가 불분명
- `DataIntegrityViolationException` 원인이 중복 결제인지 다른 제약 위반인지 파싱 필요
- 중복 INSERT가 발생한 후에야 차단 → repayment_transaction 행이 이미 써진 뒤 rollback

---

## 선택한 방법 — 조건부 UPDATE + rows affected 검증

```java
// RepaymentScheduleRepository
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
// RepaymentService
int affected = scheduleRepository.claimStatusChange(
        schedule.getRschId(),
        RepaymentSchedule.STATUS_PAID,
        List.of(RepaymentSchedule.STATUS_DUE, RepaymentSchedule.STATUS_OVERDUE));
if (affected == 0) {
    throw new BusinessException(LoanErrorCode.LOAN_091, ...);
}
```

```
T1: UPDATE rsch SET status='PAID' WHERE rsch_id=1 AND status IN ('DUE','OVERDUE')
    → affected=1 → 결제 계속 진행
T2: UPDATE rsch SET status='PAID' WHERE rsch_id=1 AND status IN ('DUE','OVERDUE')
    → affected=0 (이미 PAID) → LOAN_091 예외
```

**선택 이유**

| 기준 | 선택 이유 |
|------|-----------|
| **원자성** | 조건 확인과 상태 변경이 DB 단일 쿼리 — 조회/갱신 사이 시간 차 없음 |
| **락 최소화** | 행 잠금을 트랜잭션 전체가 아닌 UPDATE 실행 순간에만 보유 |
| **명확한 의미** | `affected=0` = "이미 처리됨", `affected=1` = "선점 성공" — 분기가 단순 |
| **외부 의존 없음** | Redis·별도 인프라 추가 없이 DB 기능만으로 해결 |
| **재시도 불필요** | 패배한 요청은 즉시 에러 반환, 루프 없음 |

**주의한 점**

- `@Modifying(clearAutomatically = true)` 필수: `@Modifying` 쿼리는 JPA 1차 캐시를 우회하므로
  UPDATE 후 캐시에 남아있는 엔티티가 stale 상태가 됨. `clearAutomatically`로 캐시를 비워
  이후 조회가 DB에서 최신 값을 읽도록 보장함.
- `before` 상태 캡처 순서: `claimStatusChange()` 호출 **이전**에 `schedule.currentStatus()`를
  변수에 저장해야 함. UPDATE 후 엔티티 상태가 캐시에서 제거되어 재조회가 필요해지기 때문.

---

## 방법 비교 요약

| 방법 | 원자성 | 락 점유 | 외부 의존 | 재시도 | 코드 복잡도 |
|------|--------|---------|-----------|--------|-------------|
| Optimistic Locking | △ (충돌 후 감지) | 없음 | 없음 | 필요 | 중 (예외 처리) |
| Pessimistic Locking | ◎ | 트랜잭션 전체 | 없음 | 불필요 | 낮음 |
| Redis 분산 락 | ◎ | TTL 동안 | Redis | 불필요 | 높음 |
| UNIQUE 제약 + 예외 | ◎ (사후) | 없음 | 없음 | 불필요 | 중 (예외 파싱) |
| **조건부 UPDATE** | **◎** | **UPDATE 순간만** | **없음** | **불필요** | **낮음** |

---

## 관련 커밋

- `f0c7dba` refactor(repayment): 회차 PAID 전이를 조건부 UPDATE rows affected 검증으로 교체
