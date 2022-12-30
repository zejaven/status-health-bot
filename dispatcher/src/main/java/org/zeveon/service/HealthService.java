package org.zeveon.service;

import org.zeveon.entity.Site;

import java.util.List;

/**
 * @author Stanislav Vafin
 */
public interface HealthService {

    void saveSites(List<String> sites);

    List<Site> getSites();

    void removeSites(List<Long> sites);
}
