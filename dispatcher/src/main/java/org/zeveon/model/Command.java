package org.zeveon.model;

import java.util.Map;

/**
 * @author Stanislav Vafin
 */
public class Command {

    public static final String HELP = "/help";
    public static final String ADD = "/add";
    public static final String GET_HOSTS = "/get_hosts";
    public static final String REMOVE = "/remove";
    public static final String STATISTIC = "/statistic";
    public static final String CHANGE_LANGUAGE = "/change_language";
    public static final String REMOVE_ALL = "/remove_all";
    public static final String CHANGE_METHOD = "/change_method";

    public static final Map<String, String> LIST = Map.of(
            ADD, "command.add",
            GET_HOSTS, "command.get",
            REMOVE, "command.remove",
            STATISTIC, "command.statistic",
            CHANGE_LANGUAGE, "command.change_language",
            REMOVE_ALL, "command.remove_all",
            CHANGE_METHOD, "command.change_method"
    );
}
