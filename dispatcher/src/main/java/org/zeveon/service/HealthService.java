package org.zeveon.service;

import org.zeveon.entity.Host;

import java.util.List;

/**
 * @author Stanislav Vafin
 */
public interface HealthService {

    void saveHosts(List<String> hosts);

    List<Host> getHosts();

    void removeHosts(List<Long> hosts);
}
