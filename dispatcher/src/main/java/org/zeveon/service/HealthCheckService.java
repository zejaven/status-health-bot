package org.zeveon.service;

import org.zeveon.entity.Site;

/**
 * @author Stanislav Vafin
 */
public interface HealthCheckService {

    void checkHealth(Site site);
}
