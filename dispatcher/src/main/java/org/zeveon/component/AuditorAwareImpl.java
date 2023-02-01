package org.zeveon.component;

import lombok.NonNull;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;
import org.zeveon.context.UserContext;

import java.util.Optional;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

/**
 * @author Stanislav Vafin
 */
@Component("auditorProvider")
public class AuditorAwareImpl implements AuditorAware<String> {

    public static final String SYSTEM = "SYSTEM";

    @NonNull
    @Override
    public Optional<String> getCurrentAuditor() {
        var instance = UserContext.getInstance();
        return instance != null
                ? ofNullable(instance.getUser().getUserName())
                : of(SYSTEM);
    }
}
