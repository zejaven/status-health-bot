package org.zeveon.job;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zeveon.component.HealthBot;
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
        Data.getCurrentHost().ifPresent(h -> {
            if (h.getLock().tryLock()) {
                try {
                    h.getLock().lock();
                    healthCheckService.checkHealth(h, healthBot.getBotInfo());
                } finally {
                    h.getLock().unlock();
                    h.getLock().unlock();
                }
            }
        });
    }
}
