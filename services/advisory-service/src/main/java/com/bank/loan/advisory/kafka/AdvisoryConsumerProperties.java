package com.bank.loan.advisory.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * advisory consumer 튜닝 파라미터.
 *
 * application.yml의 advisory.consumer.* 값을 바꾸는 것만으로 재컴파일 없이 실험 가능.
 *
 * L7 실험 가이드:
 *   1. fetch.min-bytes: 1 → 10240 변경 후 처리량/지연 비교
 *   2. max-poll-records: 500 → 5000 변경 후 rebalance 빈도 비교
 *   3. experiment-delay-ms: 0 → 60000 설정 시 max.poll.interval.ms 초과 시뮬레이션
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "advisory.consumer")
public class AdvisoryConsumerProperties {

    // fetch 튜닝 (처리량 vs 지연 trade-off)
    private int fetchMinBytes       = 1;      // 낮을수록 즉시 응답(저지연), 높을수록 모아서 응답(고처리량)
    private int fetchMaxWaitMs      = 500;    // fetchMinBytes 못 채울 때 최대 대기
    private int fetchMaxBytes       = 52_428_800;   // 50MB
    private int maxPartitionFetchBytes = 1_048_576; // 1MB

    // 폴링 (rebalance 안정성)
    private int maxPollRecords      = 500;    // 높이면 처리량↑, max.poll.interval.ms 초과 위험
    private int maxPollIntervalMs   = 300_000;
    private int sessionTimeoutMs    = 45_000;
    private int heartbeatIntervalMs = 15_000;

    // 타임아웃
    private int requestTimeoutMs    = 30_000;
    private int defaultApiTimeoutMs = 60_000;

    // 실험용 처리 지연 (ms). 0=정상. 양수 설정 시 max.poll.interval.ms 초과 시뮬레이션
    private long experimentDelayMs  = 0;

    // DLQ 재시도
    private long dlqBackoffIntervalMs = 1_000;
    private long dlqMaxAttempts       = 3;

    // L8 — Partition Assignment Strategy
    // 실험: Range / RoundRobin / StickyAssignor / CooperativeStickyAssignor 비교
    //   Range                  : 토픽별 파티션을 연속 블록으로 할당. 토픽 수 증가 시 불균형 가능
    //   RoundRobin             : 전체 파티션을 순서대로 하나씩 할당. 균등하지만 eager rebalance
    //   StickyAssignor         : 기존 할당 최대 유지. 단 rebalance 중 전체 정지 (eager)
    //   CooperativeStickyAssignor: 재할당 안 된 파티션은 계속 처리 (incremental cooperative rebalance)
    private String assignmentStrategy =
            "org.apache.kafka.clients.consumer.CooperativeStickyAssignor";

    // L8 — Static Group Membership
    // null(기본): 동적 멤버십 — 재시작마다 새 member-id 발급 → rebalance 발생
    // 값 설정: 정적 멤버십 — 재시작 후 session.timeout.ms 내 복귀 시 rebalance 안 일어남
    // 실험: ADVISORY_CONSUMER_INSTANCE_ID 환경변수로 인스턴스별 다른 값 주입
    private String groupInstanceId = null;

    // L8 — Rack Awareness
    // 같은 rack의 replica에서 읽어 크로스 AZ 대역폭 절감
    // 브로커에 replica.selector.class 설정 필요 (docker-compose.yml 참고)
    private String clientRack = "";
}
