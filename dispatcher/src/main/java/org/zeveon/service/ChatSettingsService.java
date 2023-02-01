package org.zeveon.service;

import org.zeveon.entity.ChatSettings;
import org.zeveon.entity.Host;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * @author Stanislav Vafin
 */
public interface ChatSettingsService {

    Set<ChatSettings> findChatSettingsByHost(Host host);

    Locale getLocale(Long chatId);

    Optional<ChatSettings> getChatSettings(Long chatId);

    void changeLocale(Long chatId, String locale);

    ChatSettings save(Long chatId);

    ChatSettings save(ChatSettings chatSettings);
}
