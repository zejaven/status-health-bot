package org.zeveon.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zeveon.entity.ChatSettings;
import org.zeveon.repository.ChatSettingsRepository;
import org.zeveon.service.ChatSettingsService;

import java.util.Locale;

/**
 * @author Stanislav Vafin
 */
@Service
@RequiredArgsConstructor
public class ChatSettingsServiceImpl implements ChatSettingsService {

    private final ChatSettingsRepository chatSettingsRepository;

    @Value("${chat.default-locale}")
    private String defaultLocale;

    @Override
    public Locale getLocale(Long chatId) {
        return Locale.forLanguageTag(getChatSettings(chatId).getLocale());
    }

    @Override
    @Transactional(readOnly = true)
    public ChatSettings getChatSettings(Long chatId) {
        return chatSettingsRepository.findById(chatId)
                .orElse(ChatSettings.builder()
                        .chatId(chatId)
                        .locale(defaultLocale)
                        .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeLocale(Long chatId, String locale) {
        chatSettingsRepository.save(ChatSettings.builder()
                .chatId(chatId)
                .locale(locale)
                .build());
    }
}
