package org.zeveon.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zeveon.cache.Cache;
import org.zeveon.data.Data;
import org.zeveon.entity.Site;
import org.zeveon.repository.SiteRepository;
import org.zeveon.service.HealthService;

import java.util.List;
import java.util.Locale;

/**
 * @author Stanislav Vafin
 */
@Service
@RequiredArgsConstructor
public class HealthServiceImpl implements HealthService {

    private final SiteRepository siteRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = Cache.SITES, allEntries = true)
    public void saveSites(List<String> siteUrls) {
        var currentSiteUrls = getSites().stream().map(Site::getUrl).toList();
        var sites = siteUrls.stream()
                .filter(s -> !currentSiteUrls.contains(s))
                .map(s -> Site.builder().url(s).build())
                .toList();
        siteRepository.saveAll(sites);
        Data.addAll(sites);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(Cache.SITES)
    public List<Site> getSites() {
        return siteRepository.findAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = Cache.SITES, allEntries = true)
    public void removeSites(List<Long> sites) {
        siteRepository.deleteAllById(sites);
        Data.removeAllById(sites);
    }

    @Override
    @Transactional(readOnly = true)
    public Locale getLocale(Long chatId) {
        return Locale.forLanguageTag("ru");
    }
}
