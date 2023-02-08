package org.zeveon.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import static java.util.Arrays.stream;

/**
 * @author Stanislav Vafin
 */
@Getter
@AllArgsConstructor
public enum ConnectionType {
    APPENDER_ST_PROTO(true, false),
    APPENDER_CH_PROTO(true, true),
    NO_APPENDER_ST_PROTO(false, false),
    NO_APPENDER_CH_PROTO(false, true);

    private final boolean needAppender;

    private final boolean protocolChanged;

    public static ConnectionType getByFields(boolean modified, boolean protocolChanged) {
        return stream(ConnectionType.values())
                .filter(c -> c.needAppender == modified)
                .filter(c -> c.protocolChanged == protocolChanged)
                .findAny().orElse(APPENDER_ST_PROTO);
    }
}
