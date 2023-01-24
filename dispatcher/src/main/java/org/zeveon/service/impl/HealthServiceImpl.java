package org.zeveon.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zeveon.cache.Cache;
import org.zeveon.data.Data;
import org.zeveon.entity.Site;
import org.zeveon.repository.HealthRepository;
import org.zeveon.service.HealthService;

import java.util.List;

/**
 * @author Stanislav Vafin
 */
@Service
@AllArgsConstructor
public class HealthServiceImpl implements HealthService {

    private final HealthRepository healthRepository;

    @Override
    @Transactional
    @CacheEvict(value = Cache.SITES, allEntries = true)
    public void saveSites(List<String> siteUrls) {
        var currentSiteUrls = getSites().stream().map(Site::getUrl).toList();
        var sites = siteUrls.stream()
                .filter(s -> !currentSiteUrls.contains(s))
                .map(s -> Site.builder().url(s).build())
                .toList();
        healthRepository.saveAll(sites);
        Data.addAll(sites);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(Cache.SITES)
    public List<Site> getSites() {
        return healthRepository.findAll();
    }

    @Override
    @Transactional
    @CacheEvict(value = Cache.SITES, allEntries = true)
    public void removeSites(List<Long> sites) {
        healthRepository.deleteAllById(sites);
        Data.removeAllById(sites);
    }
}
