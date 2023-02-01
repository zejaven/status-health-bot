package org.zeveon.model;

import lombok.Builder;
import lombok.Data;

/**
 * @author Stanislav Vafin
 */
@Data
@Builder
public class HealthInfo {

    private boolean statisticExists;
    private boolean responseCodeChanged;
    private int responseCode;
}
