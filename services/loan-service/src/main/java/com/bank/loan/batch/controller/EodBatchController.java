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

@Tag(name = "EOD 배치", description = "LoanEodJob — 일마감 배치 수동 트리거 (internal)")
@Slf4j
@RestController
@RequestMapping("/api/internal/eod")
@RequiredArgsConstructor
@Validated
public class EodBatchController {

    private final JobLauncher jobLauncher;
    @Qualifier("loanEodJob")
    private final Job loanEodJob;

    @Operation(summary = "EOD 일마감 배치 실행",
            description = "baseDate 기준으로 이자발생 → 자동이체 → 연체롤오버 → 승인만료 순서로 실행한다. " +
                          "같은 baseDate 로 이미 완료된 잡이 있으면 SKIPPED 를 반환한다.")
    @PostMapping("/run")
    public ApiResponse<EodRunResponse> run(
            @RequestParam("baseDate") @Pattern(regexp = "\\d{8}") String baseDate) {

        JobParameters params = new JobParametersBuilder()
                .addString("baseDate", baseDate)
                .toJobParameters();
        try {
            var execution = jobLauncher.run(loanEodJob, params);
            String status = execution.getStatus().name();
            if ("COMPLETED".equals(status)) {
                return ApiResponse.ok(EodRunResponse.completed(baseDate, execution.getId()));
            }
            return ApiResponse.ok(EodRunResponse.failed(baseDate, execution.getId(), status));

        } catch (JobInstanceAlreadyCompleteException e) {
            log.info("[EOD] baseDate={} 이미 완료된 잡", baseDate);
            return ApiResponse.ok(EodRunResponse.alreadyRun(baseDate, null));

        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobParametersInvalidException e) {
            log.warn("[EOD] baseDate={} 잡 실행 거부: {}", baseDate, e.getMessage());
            return ApiResponse.ok(EodRunResponse.failed(baseDate, null, e.getMessage()));
        }
    }
}
