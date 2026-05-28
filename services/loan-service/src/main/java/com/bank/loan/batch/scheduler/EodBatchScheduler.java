package com.bank.loan.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * EOD 배치 일별 자동 실행 스케줄러.
 *
 * loan.batch.eod-cron (application.yml) 으로 실행 시각 제어.
 * 기본값: "0 0 1 * * *" — 매일 새벽 1시 KST.
 *
 * 동일 baseDate 를 JobParameter 로 사용하므로 같은 날 이미 COMPLETED 된 잡은
 * Spring Batch 가 JobInstanceAlreadyCompleteException 으로 중복 실행을 막는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EodBatchScheduler {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final JobLauncher jobLauncher;
    @Qualifier("loanEodJob")
    private final Job loanEodJob;

    @Scheduled(cron = "${loan.batch.eod-cron}")
    public void runEod() {
        String baseDate = LocalDate.now(KST).format(DATE_FMT);
        log.info("[EOD-Scheduler] baseDate={} 배치 시작", baseDate);
        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("baseDate", baseDate)
                    .toJobParameters();
            var execution = jobLauncher.run(loanEodJob, params);
            log.info("[EOD-Scheduler] baseDate={} 완료: status={} id={}",
                    baseDate, execution.getStatus(), execution.getId());
        } catch (Exception e) {
            log.error("[EOD-Scheduler] baseDate={} 실패: {}", baseDate, e.getMessage(), e);
        }
    }
}
