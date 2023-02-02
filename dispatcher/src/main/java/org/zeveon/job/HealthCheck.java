package org.zeveon.job;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zeveon.component.HealthBot;
import org.zeveon.controller.UpdateController;
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

    private final UpdateController updateController;

    @Async
    @Scheduled(fixedRate = 1000)
    public void scheduleFixedRateTaskAsync() {
        Data.getCurrentHost().ifPresent(host -> {
            if (host.getLock().tryLock()) {
                try {
                    host.getLock().lock();
                    healthCheckService.checkHealth(
                            host.getId(),
                            healthBot.getBotInfo(),
                            (m, h) -> updateController.reportStatusCodeChanged(host, m, h)
                    );
                } finally {
                    host.getLock().unlock();
                    host.getLock().unlock();
                }
            }
        });
    }
}
