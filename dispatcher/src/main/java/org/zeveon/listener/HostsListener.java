package org.zeveon.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.zeveon.component.HealthBot;
import org.zeveon.controller.UpdateController;
import org.zeveon.data.Data;
import org.zeveon.service.HealthCheckService;

/**
 * @author Stanislav Vafin
 */
@Component
@RequiredArgsConstructor
public class HostsListener {
    private final HealthCheckService healthCheckService;

    private final HealthBot healthBot;

    private final UpdateController updateController;

    @RabbitListener(queues = "${rabbitmq.queue}")
    public void listen(Long hostId) {
        Data.getHostById(hostId).ifPresent(host -> {
            try {
                healthCheckService.checkHealth(
                        host.getId(),
                        healthBot.getBotInfo(),
                        (m, h) -> updateController.reportStatusCodeChanged(host, m, h)
                );
            } finally {
                host.getLock().release();
            }
        });
    }
}
