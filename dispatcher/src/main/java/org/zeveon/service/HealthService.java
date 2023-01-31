package org.zeveon.service;

import org.zeveon.entity.Host;

import java.util.List;
import java.util.Set;

/**
 * @author Stanislav Vafin
 */
public interface HealthService {

    void saveHosts(Set<String> hosts, Long chatId);

    List<Host> getAllHosts();

    Set<Host> getHosts(Long chatId);

    void removeHosts(Set<Long> hostIds, Long chatId);
}
