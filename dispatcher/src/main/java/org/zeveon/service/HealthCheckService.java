package org.zeveon.service;

import org.zeveon.model.BotInfo;
import org.zeveon.model.HealthInfo;

/**
 * @author Stanislav Vafin
 */
public interface HealthCheckService {

    HealthInfo checkHealth(Long hostId, BotInfo botInfo);
}
