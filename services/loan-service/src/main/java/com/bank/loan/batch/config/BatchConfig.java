package com.bank.loan.batch.config;

import com.bank.loan.accrual.service.InterestAccrualBatchService;
import com.bank.loan.applicationexpiry.service.ApplicationExpiryBatchService;
import com.bank.loan.autodebit.service.AutoDebitBatchService;
import com.bank.loan.delinquency.service.DelinquencyRolloverService;
import com.bank.loan.delinquency.service.OverdueInterestAccrualBatchService;
import com.bank.loan.guaranteeinsuranceexpiry.service.GuaranteeInsuranceExpiryBatchService;
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
 * EOD(일마감) 배치 잡 설정.
 *
 * 스텝 순서:
 *   1. interestAccrualStep         — 이자 발생 (영업일 무관)
 *   2. autoDebitStep               — 자동이체 (영업일만, 비영업일은 서비스 내부에서 skip)
 *   3. delinquencyRolloverStep     — 연체 판정·갱신·스냅샷
 *   4. overdueInterestAccrualStep  — 연체 이자 일별 발생 (rollover 직후 ACTIVE dlq 기준)
 *   5. applicationExpiryStep       — 승인 만료 처리
 *   6. guaranteeInsuranceExpiryStep — 보증보험 만기 처리 (gins_end_date < baseDate)
 *
 * 각 Tasklet 은 서비스 예외를 catch 해 로그만 남기고 다음 스텝을 계속 진행한다.
 * Spring Batch JobRepository 에 스텝별 실행 이력이 기록된다.
 *
 * 멱등성: baseDate 를 JobParameter 로 사용한다.
 *   같은 baseDate 로 재실행 시 이미 완료된 JobExecution 이 존재하면 JobInstanceAlreadyCompleteException 발생.
 *   실패한 잡은 재실행 가능 (Spring Batch 기본 동작).
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private final InterestAccrualBatchService interestAccrualBatchService;
    private final AutoDebitBatchService autoDebitBatchService;
    private final DelinquencyRolloverService delinquencyRolloverService;
    private final OverdueInterestAccrualBatchService overdueInterestAccrualBatchService;
    private final ApplicationExpiryBatchService applicationExpiryBatchService;
    private final GuaranteeInsuranceExpiryBatchService guaranteeInsuranceExpiryBatchService;

    @Bean
    public Job loanEodJob(JobRepository jobRepository,
                          Step interestAccrualStep,
                          Step autoDebitStep,
                          Step delinquencyRolloverStep,
                          Step overdueInterestAccrualStep,
                          Step applicationExpiryStep,
                          Step guaranteeInsuranceExpiryStep) {
        return new JobBuilder("loanEodJob", jobRepository)
                .start(interestAccrualStep)
                .next(autoDebitStep)
                .next(delinquencyRolloverStep)
                .next(overdueInterestAccrualStep)
                .next(applicationExpiryStep)
                .next(guaranteeInsuranceExpiryStep)
                .build();
    }

    @Bean
    public Step interestAccrualStep(JobRepository jobRepository, PlatformTransactionManager txManager) {
        return new StepBuilder("interestAccrualStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String baseDate = baseDate(chunkContext);
                    try {
                        var result = interestAccrualBatchService.run(baseDate);
                        log.info("[EOD][{}] interestAccrual processed={} skipped={}",
                                baseDate, result.processed(), result.skipped());
                    } catch (Exception e) {
                        log.error("[EOD][{}] interestAccrual 실패: {}", baseDate, e.getMessage(), e);
                    }
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }

    @Bean
    public Step autoDebitStep(JobRepository jobRepository, PlatformTransactionManager txManager) {
        return new StepBuilder("autoDebitStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String baseDate = baseDate(chunkContext);
                    try {
                        var result = autoDebitBatchService.run(baseDate);
                        log.info("[EOD][{}] autoDebit processed={} skipped={} skipReason={}",
                                baseDate, result.processed(), result.skipped(), result.skipReason());
                    } catch (Exception e) {
                        log.error("[EOD][{}] autoDebit 실패: {}", baseDate, e.getMessage(), e);
                    }
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }

    @Bean
    public Step delinquencyRolloverStep(JobRepository jobRepository, PlatformTransactionManager txManager) {
        return new StepBuilder("delinquencyRolloverStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String baseDate = baseDate(chunkContext);
                    try {
                        var result = delinquencyRolloverService.rollover(baseDate);
                        log.info("[EOD][{}] delinquencyRollover newlyOverdue={} active={} resolved={} snapshots={}",
                                baseDate, result.newlyOverdueInstallments(),
                                result.activeDelinquencies(), result.resolvedDelinquencies(),
                                result.snapshotsCreated());
                    } catch (Exception e) {
                        log.error("[EOD][{}] delinquencyRollover 실패: {}", baseDate, e.getMessage(), e);
                    }
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }

    @Bean
    public Step overdueInterestAccrualStep(JobRepository jobRepository, PlatformTransactionManager txManager) {
        return new StepBuilder("overdueInterestAccrualStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String baseDate = baseDate(chunkContext);
                    try {
                        var result = overdueInterestAccrualBatchService.run(baseDate);
                        log.info("[EOD][{}] overdueInterestAccrual processed={} skipped={}",
                                baseDate, result.processed(), result.skipped());
                    } catch (Exception e) {
                        log.error("[EOD][{}] overdueInterestAccrual 실패: {}", baseDate, e.getMessage(), e);
                    }
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }

    @Bean
    public Step applicationExpiryStep(JobRepository jobRepository, PlatformTransactionManager txManager) {
        return new StepBuilder("applicationExpiryStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String baseDate = baseDate(chunkContext);
                    try {
                        var result = applicationExpiryBatchService.run(baseDate);
                        log.info("[EOD][{}] applicationExpiry expired={}", baseDate, result.processed());
                    } catch (Exception e) {
                        log.error("[EOD][{}] applicationExpiry 실패: {}", baseDate, e.getMessage(), e);
                    }
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }

    @Bean
    public Step guaranteeInsuranceExpiryStep(JobRepository jobRepository, PlatformTransactionManager txManager) {
        return new StepBuilder("guaranteeInsuranceExpiryStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String baseDate = baseDate(chunkContext);
                    try {
                        var result = guaranteeInsuranceExpiryBatchService.run(baseDate);
                        log.info("[EOD][{}] guaranteeInsuranceExpiry total={} expired={}",
                                baseDate, result.totalCandidates(), result.processed());
                    } catch (Exception e) {
                        log.error("[EOD][{}] guaranteeInsuranceExpiry 실패: {}", baseDate, e.getMessage(), e);
                    }
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }

    private static String baseDate(org.springframework.batch.core.scope.context.ChunkContext ctx) {
        return ctx.getStepContext().getJobParameters().get("baseDate").toString();
    }
}
