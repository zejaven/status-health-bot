package org.zeveon.service;

import org.zeveon.entity.ChatSettings;
import org.zeveon.entity.Host;
import org.zeveon.model.Method;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * @author Stanislav Vafin
 */
public interface ChatSettingsService {

    Set<ChatSettings> findChatSettingsByHostAndMethod(Host host, Method method);

    Locale getLocale(Long chatId);

    Optional<ChatSettings> getChatSettings(Long chatId);

    void changeLocale(Long chatId, String locale);

    void changeMethod(Long chatId, Method method);

    ChatSettings save(Long chatId);

    ChatSettings save(ChatSettings chatSettings);
}
