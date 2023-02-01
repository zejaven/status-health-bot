package org.zeveon.context;

import org.telegram.telegrambots.meta.api.objects.User;

/**
 * @author Stanislav Vafin
 */
public class UserContext {

    private static final ThreadLocal<UserContext> INSTANCE = new ThreadLocal<>();

    private final User user;

    private UserContext(User user) {
        this.user = user;
    }

    public static UserContext getInstance() {
        return INSTANCE.get();
    }

    public static void setInstance(User user) {
        INSTANCE.set(new UserContext(user));
    }

    public User getUser() {
        return user;
    }
}
