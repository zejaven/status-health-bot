package org.zeveon.context;

import org.telegram.telegrambots.meta.api.objects.User;

/**
 * @author Stanislav Vafin
 */
public class UserContext {

    private static final ThreadLocal<UserContext> INSTANCE = new ThreadLocal<>();

    private final User user;
    private final boolean admin;
    private final boolean superAdmin;

    private UserContext(User user, boolean admin, boolean superAdmin) {
        this.user = user;
        this.admin = admin;
        this.superAdmin = superAdmin;
    }

    public static UserContext getInstance() {
        return INSTANCE.get();
    }

    public static void setInstance(User user, boolean isAdmin, boolean isSuperAdmin) {
        INSTANCE.set(new UserContext(user, isAdmin, isSuperAdmin));
    }

    public User getUser() {
        return user;
    }

    public boolean isAdmin() {
        return admin;
    }

    public boolean isSuperAdmin() {
        return superAdmin;
    }
}
