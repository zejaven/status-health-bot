package org.zeveon.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zeveon.cache.Cache;
import org.zeveon.data.Data;
import org.zeveon.entity.Host;
import org.zeveon.repository.HostRepository;
import org.zeveon.service.HealthService;

import java.util.List;

/**
 * @author Stanislav Vafin
 */
@Service
@RequiredArgsConstructor
public class HealthServiceImpl implements HealthService {

    private final HostRepository hostRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = Cache.HOSTS, allEntries = true)
    public void saveHosts(List<String> hostUrls) {
        var currentHostUrls = getHosts().stream().map(Host::getUrl).toList();
        var hosts = hostUrls.stream()
                .filter(s -> !currentHostUrls.contains(s))
                .map(s -> Host.builder().url(s).build())
                .toList();
        hostRepository.saveAll(hosts);
        Data.addAll(hosts);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(Cache.HOSTS)
    public List<Host> getHosts() {
        return hostRepository.findAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = Cache.HOSTS, allEntries = true)
    public void removeHosts(List<Long> hosts) {
        hostRepository.deleteAllById(hosts);
        Data.removeAllById(hosts);
    }
}
