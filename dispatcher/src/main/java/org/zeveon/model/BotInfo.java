package org.zeveon.model;

import lombok.Builder;
import lombok.Data;

/**
 * @author Stanislav Vafin
 */
@Data
@Builder
public class BotInfo {

    private Method healthCheckMethod;
    private String botUsername;
    private Integer healthCheckConnectionTimeout;
}
