package org.zeveon.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zeveon.cache.Cache;
import org.zeveon.data.Data;
import org.zeveon.entity.ChatSettings;
import org.zeveon.entity.Host;
import org.zeveon.repository.HostRepository;
import org.zeveon.service.ChatSettingsService;
import org.zeveon.service.HealthService;

import java.util.List;
import java.util.Set;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

/**
 * @author Stanislav Vafin
 */
@Service
@RequiredArgsConstructor
public class HealthServiceImpl implements HealthService {

    private final HostRepository hostRepository;
    private final ChatSettingsService chatSettingsService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(key = "#chatId", value = Cache.HOSTS)
    public void saveHosts(Set<String> hostUrls, Long chatId) {
        var currentChatHostUrls = getHosts(chatId).stream().map(Host::getUrl).toList();
        var chatSettings = chatSettingsService.getChatSettings(chatId)
                .orElseGet(() -> chatSettingsService.save(chatId));
        var chatHosts = hostUrls.stream()
                .filter(h -> !currentChatHostUrls.contains(h))
                .map(h -> buildHost(chatSettings, h))
                .toList();
        var otherChatHosts = getAllHosts().stream()
                .filter(h -> !h.getChatSettings().contains(chatSettings))
                .filter(chatHosts::contains)
                .toList();
        var excludedChatHosts = chatHosts.stream()
                .filter(h -> !otherChatHosts.contains(h))
                .toList();
        hostRepository.saveAll(excludedChatHosts);
        chatSettings.getHosts().addAll(concat(
                otherChatHosts.stream(),
                excludedChatHosts.stream()
        ).collect(toSet()));
        Data.addAll(excludedChatHosts);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Host> getAllHosts() {
        return hostRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(key = "#chatId", value = Cache.HOSTS)
    public Set<Host> getHosts(Long chatId) {
        return hostRepository.findByChatSettingsChatId(chatId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(key = "#chatId", value = Cache.HOSTS)
    public void removeHosts(Set<Long> hostIds, Long chatId) {
        var chatSettings = chatSettingsService.getChatSettings(chatId)
                .orElseGet(() -> chatSettingsService.save(chatId));
        var chatHosts = chatSettings.getHosts();
        var filteredHostIds = chatHosts.stream()
                .filter(host -> host.getChatSettings().size() == 1)
                .map(Host::getId)
                .filter(hostIds::contains)
                .collect(toSet());
        chatHosts.removeIf(h -> hostIds.contains(h.getId()));
        hostRepository.deleteAllById(filteredHostIds);
        Data.removeAllById(filteredHostIds);
    }

    private Host buildHost(ChatSettings chatSettings, String url) {
        return Host.builder()
                .url(url)
                .chatSettings(singleton(chatSettings))
                .build();
    }
}
