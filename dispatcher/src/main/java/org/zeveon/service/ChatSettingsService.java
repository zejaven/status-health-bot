package org.zeveon.service;

import org.zeveon.entity.ChatSettings;
import org.zeveon.entity.Host;
import org.zeveon.model.Method;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * @author Stanislav Vafin
 */
public interface ChatSettingsService {

    ChatSettings save(Long chatId);

    ChatSettings save(ChatSettings chatSettings);

    List<ChatSettings> getAllChatSettings();

    Optional<ChatSettings> getChatSettings(Long chatId);

    Set<ChatSettings> findChatSettingsByHostAndMethod(Host host, Method method);

    Locale getLocale(Long chatId);

    void updateLocale(Long chatId, String locale);

    void updateMethod(Long chatId, Method method);

    void updateCheckRate(Long chatId, Duration rate);
}
