package org.zeveon.service;

import org.zeveon.entity.Site;
import org.zeveon.model.BotInfo;

/**
 * @author Stanislav Vafin
 */
public interface HealthCheckService {

    void checkHealth(Site site, BotInfo botInfo);
}
