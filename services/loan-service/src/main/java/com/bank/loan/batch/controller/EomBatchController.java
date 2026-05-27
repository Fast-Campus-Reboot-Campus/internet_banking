package com.bank.loan.batch.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.batch.dto.EodRunResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "EOM 배치", description = "LoanEomJob — 월마감 배치 수동 트리거 (internal)")
@Slf4j
@RestController
@RequestMapping("/api/internal/eom")
@RequiredArgsConstructor
@Validated
public class EomBatchController {

    private final JobLauncher jobLauncher;
    @Qualifier("loanEomJob")
    private final Job loanEomJob;

    @Operation(summary = "EOM 월마감 배치 실행",
            description = "baseMonth (YYYYMM) 기준으로 월별 회계 요약을 적재한다. " +
                          "같은 baseMonth 로 이미 완료된 잡이 있으면 SKIPPED 를 반환한다.")
    @PostMapping("/run")
    public ApiResponse<EodRunResponse> run(
            @RequestParam("baseMonth") @Pattern(regexp = "\\d{6}") String baseMonth) {

        JobParameters params = new JobParametersBuilder()
                .addString("baseMonth", baseMonth)
                .toJobParameters();
        try {
            var execution = jobLauncher.run(loanEomJob, params);
            String status = execution.getStatus().name();
            if ("COMPLETED".equals(status)) {
                return ApiResponse.ok(EodRunResponse.completed(baseMonth, execution.getId()));
            }
            return ApiResponse.ok(EodRunResponse.failed(baseMonth, execution.getId(), status));

        } catch (JobInstanceAlreadyCompleteException e) {
            log.info("[EOM] baseMonth={} 이미 완료된 잡", baseMonth);
            return ApiResponse.ok(EodRunResponse.alreadyRun(baseMonth, null));

        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobParametersInvalidException e) {
            log.warn("[EOM] baseMonth={} 잡 실행 거부: {}", baseMonth, e.getMessage());
            return ApiResponse.ok(EodRunResponse.failed(baseMonth, null, e.getMessage()));
        }
    }
}
