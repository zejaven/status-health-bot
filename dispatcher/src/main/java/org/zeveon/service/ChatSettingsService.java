package org.zeveon.service;

import org.zeveon.entity.ChatSettings;

import java.util.Locale;

/**
 * @author Stanislav Vafin
 */
public interface ChatSettingsService {

    Locale getLocale(Long chatId);

    ChatSettings getChatSettings(Long chatId);

    void changeLocale(Long chatId, String locale);
}
