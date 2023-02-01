package org.zeveon.component;

import lombok.NonNull;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

/**
 * @author Stanislav Vafin
 */
@Component("dateTimeProvider")
public class CustomDateTimeProvider implements DateTimeProvider {

    @NonNull
    @Override
    public Optional<TemporalAccessor> getNow() {
        return Optional.of(ZonedDateTime.now());
    }
}
