package org.zeveon.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Stanislav Vafin
 */
public class Command {

    public static final String HELP = "/help";
    public static final String ADD = "/add";
    public static final String GET_HOSTS = "/get_hosts";
    public static final String REMOVE = "/remove";
    public static final String REMOVE_ALL = "/remove_all";
    public static final String STATISTIC = "/statistic";
    public static final String SETTINGS = "/settings";
    public static final String CHANGE_LANGUAGE = "/change_language";
    public static final String CHANGE_METHOD = "/change_method";
    public static final String CHANGE_RATE = "/change_rate";
    public static final String ADD_ADMIN = "/add_admin";
    public static final String REMOVE_ADMIN = "/remove_admin";

    public static final Map<String, String> LIST = new LinkedHashMap<>() {{
        put(ADD, "command.add");
        put(GET_HOSTS, "command.get");
        put(REMOVE, "command.remove");
        put(REMOVE_ALL, "command.remove_all");
        put(STATISTIC, "command.statistic");
        put(SETTINGS, "command.settings");
        put(CHANGE_LANGUAGE, "command.change_language");
        put(CHANGE_METHOD, "command.change_method");
        put(CHANGE_RATE, "command.change_rate");
        put(ADD_ADMIN, "command.add_admin");
        put(REMOVE_ADMIN, "command.remove_admin");
    }};

    public static final Set<String> SUPER_ADMIN_COMMANDS = Set.of(ADD_ADMIN, REMOVE_ADMIN);
}
