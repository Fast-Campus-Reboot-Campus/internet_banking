package com.bank.loan.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 알림 발송용 비동기 실행기.
 *
 * 약정/안내서 PDF 생성 + SMS/알림톡/이메일은 무거운 외부 IO 라서 도메인 트랜잭션을 막지 않도록
 * 별도 스레드 풀에서 fire-and-forget 으로 처리한다.
 *
 * 풀 구성:
 *   core         4     상시 유지 스레드
 *   max          8     피크 시 최대 스레드
 *   queue        100   대기 큐 (메모리 무한 증가 방지)
 *   rejection    CallerRunsPolicy   큐 가득 차면 호출 스레드가 직접 실행 (back-pressure)
 *
 * @TransactionalEventListener(AFTER_COMMIT) + @Async("notificationExecutor") 조합으로
 * 도메인 트랜잭션이 commit 된 후에만 발송이 트리거된다.
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    public static final String NOTIFICATION_EXECUTOR = "notificationExecutor";
    public static final String ADVISORY_EXECUTOR     = "advisoryExecutor";

    @Bean(name = NOTIFICATION_EXECUTOR)
    public ThreadPoolTaskExecutor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("loan-noti-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        log.info("notificationExecutor initialized: core=4, max=8, queue=100, policy=CallerRunsPolicy");
        return executor;
    }

    @Bean(name = ADVISORY_EXECUTOR)
    public ThreadPoolTaskExecutor advisoryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("loan-advisory-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("advisoryExecutor initialized: core=2, max=4, queue=50, policy=DiscardPolicy");
        return executor;
    }
}
