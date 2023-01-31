package org.zeveon.service;

import org.zeveon.entity.ChatSettings;

import java.util.Locale;
import java.util.Optional;

/**
 * @author Stanislav Vafin
 */
public interface ChatSettingsService {

    Locale getLocale(Long chatId);

    Optional<ChatSettings> getChatSettings(Long chatId);

    void changeLocale(Long chatId, String locale);

    ChatSettings save(Long chatId);

    ChatSettings save(ChatSettings chatSettings);
}
