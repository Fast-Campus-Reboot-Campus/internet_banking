package com.bank.loan.batch.controller;

import com.bank.common.web.ApiResponse;
import com.bank.loan.batch.dto.EodHistoryResponse;
import com.bank.loan.batch.dto.EodRunResponse;
import com.bank.loan.batch.service.EodHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    private final EodHistoryService historyService;

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

    @Operation(summary = "EOD 실패 잡 재처리",
            description = "지정 baseDate 의 마지막 JobExecution 이 FAILED/STOPPED 인 경우 동일 파라미터로 " +
                          "재실행한다 (Spring Batch 자동 restart — 실패 스텝부터). " +
                          "COMPLETED 잡은 거부, JobExecution 자체가 없으면 NOT_FOUND.")
    @PostMapping("/restart")
    public ApiResponse<EodRunResponse> restart(
            @RequestParam("baseDate") @Pattern(regexp = "\\d{8}") String baseDate) {

        var lastOpt = historyService.findLastExecution(baseDate);
        if (lastOpt.isEmpty()) {
            log.info("[EOD-restart] baseDate={} JobExecution 없음", baseDate);
            return ApiResponse.ok(EodRunResponse.restartNotFound(baseDate));
        }
        var last = lastOpt.get();
        BatchStatus status = last.getStatus();
        if (status == BatchStatus.COMPLETED) {
            log.info("[EOD-restart] baseDate={} 이미 COMPLETED — 거부", baseDate);
            return ApiResponse.ok(EodRunResponse.restartRejected(
                    baseDate, last.getId(), "COMPLETED 잡은 재처리 불가"));
        }
        if (status == BatchStatus.STARTED || status == BatchStatus.STARTING) {
            log.warn("[EOD-restart] baseDate={} 실행 중 — 거부", baseDate);
            return ApiResponse.ok(EodRunResponse.restartRejected(
                    baseDate, last.getId(), "현재 실행 중 — restart 불가"));
        }

        JobParameters params = new JobParametersBuilder()
                .addString("baseDate", baseDate)
                .toJobParameters();
        try {
            var newExec = jobLauncher.run(loanEodJob, params);
            String newStatus = newExec.getStatus().name();
            if ("COMPLETED".equals(newStatus)) {
                return ApiResponse.ok(EodRunResponse.completed(baseDate, newExec.getId()));
            }
            return ApiResponse.ok(EodRunResponse.failed(baseDate, newExec.getId(), newStatus));
        } catch (JobInstanceAlreadyCompleteException e) {
            return ApiResponse.ok(EodRunResponse.restartRejected(baseDate, last.getId(),
                    "이미 COMPLETED 된 잡"));
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobParametersInvalidException e) {
            log.warn("[EOD-restart] baseDate={} 거부: {}", baseDate, e.getMessage());
            return ApiResponse.ok(EodRunResponse.restartRejected(baseDate, last.getId(), e.getMessage()));
        }
    }

    @Operation(summary = "EOD 실행 이력 조회",
            description = "loanEodJob 의 JobExecution 이력을 최신순으로 반환한다. " +
                          "from/to (YYYYMMDD) 가 주어지면 baseDate 가 그 범위에 있는 실행만 반환한다.")
    @GetMapping("/history")
    public ApiResponse<List<EodHistoryResponse>> history(
            @RequestParam(value = "from", required = false) @Pattern(regexp = "\\d{8}") String from,
            @RequestParam(value = "to",   required = false) @Pattern(regexp = "\\d{8}") String to) {
        return ApiResponse.ok(historyService.list(from, to));
    }
}
