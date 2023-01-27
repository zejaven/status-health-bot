package org.zeveon.model;

import java.util.Map;

/**
 * @author Stanislav Vafin
 */
public class Command {

    public static final String HELP = "/help";
    public static final String ADD = "/add";
    public static final String GET_SITES = "/get_sites";
    public static final String REMOVE = "/remove";
    public static final String STATISTIC = "/statistic";
    public static final String CHANGE_LANGUAGE = "/change_language";

    public static final Map<String, String> LIST = Map.of(
            ADD, "command.add",
            GET_SITES, "command.get",
            REMOVE, "command.remove",
            STATISTIC, "command.statistic",
            CHANGE_LANGUAGE, "command.change_language"
    );
}
