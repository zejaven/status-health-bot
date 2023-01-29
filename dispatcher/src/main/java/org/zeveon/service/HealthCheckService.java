package org.zeveon.service;

import org.zeveon.entity.Host;
import org.zeveon.model.BotInfo;

/**
 * @author Stanislav Vafin
 */
public interface HealthCheckService {

    void checkHealth(Host host, BotInfo botInfo);
}
