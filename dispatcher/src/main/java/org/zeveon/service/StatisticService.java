package org.zeveon.service;

import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.util.Optional;

/**
 * @author Stanislav Vafin
 */
public interface StatisticService {

    Optional<InputFile> generateStatistic();
}
