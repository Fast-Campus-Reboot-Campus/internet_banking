package com.bank.loan.batch.listener;

import com.bank.loan.notification.channel.KafkaChannelAdapter;
import com.bank.loan.notification.outbox.NotificationOutboxAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * EOD 잡 종료 알림 listener.
 *
 * 잡 commit 후 결과 요약을 NotificationOutbox 에 적재한다.
 *   eventTypeCd  = LOAN_EOD_COMPLETED (BatchStatus.COMPLETED) 또는 LOAN_EOD_FAILED (그 외)
 *   referenceId  = JobExecution ID
 *   channelCd    = KAFKA_DOMAIN_EVENT
 *
 * outbox 의 dispatch 배치(NotificationDispatchService) 가 KafkaChannelAdapter 로 발행한다.
 * 외부 모니터링/Slack 시스템이 토픽 "loan-domain-events" 구독으로 EOD 결과를 수신한다.
 *
 * 멱등: outbox idempotencyKey = eventType + ":" + executionId + ":KAFKA_DOMAIN_EVENT" UNIQUE.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EodNotificationListener implements JobExecutionListener {

    public static final String EVENT_TYPE_COMPLETED = "LOAN_EOD_COMPLETED";
    public static final String EVENT_TYPE_FAILED    = "LOAN_EOD_FAILED";

    private final NotificationOutboxAppender outboxAppender;
    private final ObjectMapper objectMapper;

    @Override
    public void afterJob(JobExecution execution) {
        String baseDate = execution.getJobParameters().getString("baseDate");
        BatchStatus status = execution.getStatus();
        String eventType = status == BatchStatus.COMPLETED ? EVENT_TYPE_COMPLETED : EVENT_TYPE_FAILED;

        String payload;
        try {
            payload = objectMapper.writeValueAsString(buildPayload(execution, baseDate));
        } catch (JsonProcessingException e) {
            log.error("[EOD-noti] JSON 직렬화 실패 baseDate={} execId={}: {}",
                    baseDate, execution.getId(), e.getMessage());
            return;
        }

        try {
            outboxAppender.enqueue(eventType, execution.getId(),
                    KafkaChannelAdapter.CHANNEL_CD, payload);
            log.info("[EOD-noti] outbox 적재 eventType={} execId={} baseDate={}",
                    eventType, execution.getId(), baseDate);
        } catch (RuntimeException e) {
            // outbox 적재 실패는 잡 자체 결과에 영향 주면 안 됨
            log.error("[EOD-noti] outbox 적재 실패 baseDate={} execId={}: {}",
                    baseDate, execution.getId(), e.toString());
        }
    }

    private Map<String, Object> buildPayload(JobExecution exec, String baseDate) {
        LocalDateTime start = exec.getStartTime();
        LocalDateTime end   = exec.getEndTime();
        Long durationMs = (start != null && end != null)
                ? Duration.between(start, end).toMillis() : null;

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("baseDate", baseDate);
        root.put("jobExecutionId", exec.getId());
        root.put("status", exec.getStatus().name());
        root.put("exitCode", exec.getExitStatus().getExitCode());
        root.put("startTime", start);
        root.put("endTime", end);
        root.put("durationMs", durationMs);

        List<Map<String, Object>> steps = exec.getStepExecutions().stream()
                .sorted(Comparator.comparing(StepExecution::getId))
                .map(this::stepSummary)
                .toList();
        root.put("steps", steps);
        return root;
    }

    private Map<String, Object> stepSummary(StepExecution se) {
        Long durMs = (se.getStartTime() != null && se.getEndTime() != null)
                ? Duration.between(se.getStartTime(), se.getEndTime()).toMillis() : null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stepName", se.getStepName());
        m.put("status", se.getStatus().name());
        m.put("exitCode", se.getExitStatus().getExitCode());
        m.put("durationMs", durMs);
        return m;
    }
}
