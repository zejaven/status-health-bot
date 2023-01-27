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

    public static final Map<String, String> LIST = Map.of(
            GET_SITES, "Получить список всех сохраненных сайтов",
            REMOVE, "Удалить сайты по их идентификаторам. Пример: /remove 1,4,5",
            STATISTIC, "Показать статистику запросов"
    );
}
