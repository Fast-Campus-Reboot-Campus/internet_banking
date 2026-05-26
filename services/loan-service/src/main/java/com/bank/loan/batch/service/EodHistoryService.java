package com.bank.loan.batch.service;

import com.bank.loan.batch.dto.EodHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * EOD 잡 실행 이력 조회 (Spring Batch JobExplorer 기반).
 *
 * 최근 N개의 JobInstance 를 조회하고 각각의 JobExecution 을 펼쳐서 반환한다.
 * from/to 가 주어지면 JobParameters.baseDate 가 그 범위에 있는 실행만 필터링한다.
 *
 * baseDate 사전식 비교: YYYYMMDD 문자열은 사전식 비교가 곧 날짜 비교.
 */
@Service
@RequiredArgsConstructor
public class EodHistoryService {

    public static final String JOB_NAME = "loanEodJob";
    private static final int MAX_INSTANCES = 200;

    private final JobExplorer jobExplorer;

    /**
     * 특정 baseDate 의 가장 최근 JobExecution 을 반환.
     * 재처리 (restart) 가능 여부 판단용.
     */
    public Optional<JobExecution> findLastExecution(String baseDate) {
        List<JobInstance> instances = jobExplorer.getJobInstances(JOB_NAME, 0, MAX_INSTANCES);
        for (JobInstance inst : instances) {
            List<JobExecution> execs = jobExplorer.getJobExecutions(inst);
            if (execs.isEmpty()) continue;
            // JobExplorer.getJobExecutions 는 최신순 (생성 역순)
            JobExecution last = execs.get(0);
            if (baseDate.equals(last.getJobParameters().getString("baseDate"))) {
                return Optional.of(last);
            }
        }
        return Optional.empty();
    }

    public List<EodHistoryResponse> list(String from, String to) {
        List<JobInstance> instances = jobExplorer.getJobInstances(JOB_NAME, 0, MAX_INSTANCES);

        List<EodHistoryResponse> result = new ArrayList<>();
        for (JobInstance instance : instances) {
            for (JobExecution exec : jobExplorer.getJobExecutions(instance)) {
                String baseDate = baseDateOf(exec);
                if (baseDate == null) continue;
                if (from != null && baseDate.compareTo(from) < 0) continue;
                if (to   != null && baseDate.compareTo(to)   > 0) continue;
                result.add(toResponse(exec, baseDate));
            }
        }

        // 최신 실행 우선
        result.sort(Comparator.comparing(EodHistoryResponse::jobExecutionId).reversed());
        return result;
    }

    private static String baseDateOf(JobExecution exec) {
        Object v = exec.getJobParameters().getParameters().get("baseDate") == null
                ? null
                : exec.getJobParameters().getString("baseDate");
        return v == null ? null : v.toString();
    }

    private static EodHistoryResponse toResponse(JobExecution exec, String baseDate) {
        LocalDateTime start = exec.getStartTime();
        LocalDateTime end   = exec.getEndTime();
        Long durationMs = (start != null && end != null)
                ? Duration.between(start, end).toMillis()
                : null;

        List<EodHistoryResponse.StepInfo> steps = exec.getStepExecutions().stream()
                .sorted(Comparator.comparing(StepExecution::getId))
                .map(EodHistoryService::toStepInfo)
                .toList();

        return new EodHistoryResponse(
                exec.getId(),
                baseDate,
                exec.getStatus().name(),
                exec.getExitStatus().getExitCode(),
                start,
                end,
                durationMs,
                steps
        );
    }

    private static EodHistoryResponse.StepInfo toStepInfo(StepExecution se) {
        LocalDateTime sStart = se.getStartTime();
        LocalDateTime sEnd   = se.getEndTime();
        Long durMs = (sStart != null && sEnd != null)
                ? Duration.between(sStart, sEnd).toMillis()
                : null;
        return new EodHistoryResponse.StepInfo(
                se.getId(),
                se.getStepName(),
                se.getStatus().name(),
                se.getExitStatus().getExitCode(),
                sStart,
                sEnd,
                durMs,
                se.getExitStatus().getExitDescription()
        );
    }
}
