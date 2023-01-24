package org.zeveon.job;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zeveon.controller.HealthBot;
import org.zeveon.data.Data;
import org.zeveon.service.HealthCheckService;

/**
 * @author Stanislav Vafin
 */
@Slf4j
@Component
@EnableAsync
@AllArgsConstructor
public class HealthCheck {
    private final HealthCheckService healthCheckService;

    private final HealthBot healthBot;

    @Async
    @Scheduled(fixedRate = 1000)
    public void scheduleFixedRateTaskAsync() {
        Data.getCurrentSite().ifPresent(s -> {
            if (s.getLock().tryLock()) {
                try {
                    s.getLock().lock();
                    healthCheckService.checkHealth(s, healthBot.getBotInfo());
                } finally {
                    s.getLock().unlock();
                    s.getLock().unlock();
                }
            }
        });
    }
}
