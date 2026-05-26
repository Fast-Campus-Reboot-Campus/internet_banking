package com.bank.loan.batch.config;

import com.bank.loan.accounting.service.MonthlyAccountingSummaryBatchService;
import com.bank.loan.ecl.service.EclCalculationBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * EOM(월마감) 배치 잡 설정 — EOD 와 별도 잡.
 *
 * 스텝:
 *   1. monthlyAccountingSummaryStep — 월별 회계 요약 적재
 *   2. eclCalculationStep            — IFRS9 ECL 월별 산출 (ACTIVE 약정마다)
 *
 * JobParameter: baseMonth (YYYYMM 6자리).
 * 멱등: UNIQUE(summary_month) — 같은 baseMonth 재실행 시 서비스 단에서 skip.
 * 잡 단의 JobInstanceAlreadyCompleteException 도 동일 baseMonth 로 차단됨.
 *
 * 향후 확장:
 *   - 월별 신규/종결 약정 통계 스텝
 *   - 월별 알림 발송 (월말 명세서 등)
 *   - 충당금 산출 (ECL Phase 4)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class EomBatchConfig {

    private final MonthlyAccountingSummaryBatchService monthlyAccountingSummaryBatchService;
    private final EclCalculationBatchService eclCalculationBatchService;

    @Bean
    public Job loanEomJob(JobRepository jobRepository,
                          Step monthlyAccountingSummaryStep,
                          Step eclCalculationStep) {
        return new JobBuilder("loanEomJob", jobRepository)
                .start(monthlyAccountingSummaryStep)
                .next(eclCalculationStep)
                .build();
    }

    @Bean
    public Step eclCalculationStep(JobRepository jobRepository, PlatformTransactionManager txManager) {
        return new StepBuilder("eclCalculationStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String baseMonth = chunkContext.getStepContext().getJobParameters().get("baseMonth").toString();
                    try {
                        var result = eclCalculationBatchService.run(baseMonth);
                        log.info("[EOM][{}] ecl total={} processed={} skipped={} totalEcl={}",
                                baseMonth, result.totalCandidates(), result.processed(),
                                result.skipped(), result.totalEcl());
                    } catch (Exception e) {
                        log.error("[EOM][{}] ecl 실패: {}", baseMonth, e.getMessage(), e);
                    }
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }

    @Bean
    public Step monthlyAccountingSummaryStep(JobRepository jobRepository, PlatformTransactionManager txManager) {
        return new StepBuilder("monthlyAccountingSummaryStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String baseMonth = chunkContext.getStepContext().getJobParameters().get("baseMonth").toString();
                    try {
                        var result = monthlyAccountingSummaryBatchService.run(baseMonth);
                        log.info("[EOM][{}] monthlyAccountingSummary created={} interest={} npl={}",
                                baseMonth, result.created(), result.interestRevenue(), result.monthEndNplCount());
                    } catch (Exception e) {
                        log.error("[EOM][{}] monthlyAccountingSummary 실패: {}", baseMonth, e.getMessage(), e);
                    }
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }
}
