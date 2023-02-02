package org.zeveon.service;

import org.zeveon.model.BotInfo;
import org.zeveon.model.HealthInfo;
import org.zeveon.model.Method;

import java.util.function.BiConsumer;

/**
 * @author Stanislav Vafin
 */
public interface HealthCheckService {

    void checkHealth(Long hostId, BotInfo botInfo, BiConsumer<Method, HealthInfo> reportStatusMethod);
}
