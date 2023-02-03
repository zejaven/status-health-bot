package org.zeveon.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zeveon.entity.ChatSettings;
import org.zeveon.entity.Host;
import org.zeveon.model.Method;
import org.zeveon.repository.ChatSettingsRepository;
import org.zeveon.service.ChatSettingsService;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.singleton;

/**
 * @author Stanislav Vafin
 */
@Service
@RequiredArgsConstructor
public class ChatSettingsServiceImpl implements ChatSettingsService {

    private final ChatSettingsRepository chatSettingsRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSettings save(Long chatId) {
        return save(ChatSettings.builder()
                .chatId(chatId)
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSettings save(ChatSettings chatSettings) {
        return chatSettingsRepository.save(chatSettings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatSettings> getAllChatSettings() {
        return chatSettingsRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChatSettings> getChatSettings(Long chatId) {
        return chatSettingsRepository.findById(chatId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<ChatSettings> findChatSettingsByHostAndMethod(Host host, Method method) {
        return chatSettingsRepository.findChatSettingsByHostsInAndMethod(singleton(host), method);
    }

    @Override
    @Transactional(readOnly = true)
    public Locale getLocale(Long chatId) {
        return getChatSettings(chatId)
                .map(ChatSettings::getLocale)
                .map(Locale::forLanguageTag)
                .orElse(Locale.forLanguageTag(ChatSettings.builder().build().getLocale()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLocale(Long chatId, String locale) {
        chatSettingsRepository.findById(chatId)
                .ifPresentOrElse(
                        c -> c.setLocale(locale),
                        () -> save(ChatSettings.builder()
                                .chatId(chatId)
                                .locale(locale)
                                .build())
                );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMethod(Long chatId, Method method) {
        chatSettingsRepository.findById(chatId)
                .ifPresentOrElse(
                        c -> c.setMethod(method),
                        () -> save(ChatSettings.builder()
                                .chatId(chatId)
                                .method(method)
                                .build())
                );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCheckRate(Long chatId, Duration rate) {
        chatSettingsRepository.findById(chatId)
                .ifPresentOrElse(
                        c -> c.setCheckRate(rate),
                        () -> save(ChatSettings.builder()
                                .chatId(chatId)
                                .checkRate(rate)
                                .build())
                );
    }
}
