package org.zeveon.job;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.zeveon.component.RabbitSender;
import org.zeveon.data.Data;
import org.zeveon.entity.ChatSettings;
import org.zeveon.entity.Host;
import org.zeveon.entity.Statistic;
import org.zeveon.service.ChatSettingsService;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * @author Stanislav Vafin
 */
@Component
@RequiredArgsConstructor
public class PrepareHostsTask {

    private final RabbitSender rabbitSender;

    private final ChatSettingsService chatSettingsService;

    @Scheduled(fixedRate = 1000)
    @Transactional(readOnly = true)
    public void scheduleFixedRateTask() {
        chatSettingsService.getAllChatSettings()
                .forEach(chatSettings -> chatSettings.getHosts()
                        .forEach(host -> {
                            var modifiedDate = getModifiedDate(chatSettings, host);
                            modifiedDate.ifPresentOrElse(
                                    md -> {
                                        if (checkHostReady(chatSettings, md)) {
                                            lockHostAndSend(host);
                                        }
                                    },
                                    () -> lockHostAndSend(host)
                            );
                        }));
    }

    private void lockHostAndSend(Host host) {
        Data.getHostById(host.getId()).ifPresent(h -> {
            if (h.getLock().tryAcquire()) {
                rabbitSender.send(host.getId());
            }
        });
    }

    private boolean checkHostReady(ChatSettings chatSettings, ZonedDateTime modifiedDate) {
        return chatSettings.getCheckRate()
                .minus(Duration.between(modifiedDate, ZonedDateTime.now())).isNegative();
    }

    private Optional<ZonedDateTime> getModifiedDate(ChatSettings chatSettings, Host host) {
        return host.getStatistic().stream()
                .filter(s -> s.getId().getMethod().equals(chatSettings.getMethod()))
                .findAny().map(Statistic::getModifiedDate);
    }
}
