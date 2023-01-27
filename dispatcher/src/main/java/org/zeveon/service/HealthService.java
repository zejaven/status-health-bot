package org.zeveon.service;

import org.zeveon.entity.Site;

import java.util.List;
import java.util.Locale;

/**
 * @author Stanislav Vafin
 */
public interface HealthService {

    void saveSites(List<String> sites);

    List<Site> getSites();

    void removeSites(List<Long> sites);

    Locale getLocale(Long chatId);
}
