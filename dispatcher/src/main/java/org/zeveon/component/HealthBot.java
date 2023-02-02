package org.zeveon.component;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.zeveon.controller.UpdateController;
import org.zeveon.model.BotInfo;
import org.zeveon.model.Method;

/**
 * @author Stanislav Vafin
 */
@Component
@RequiredArgsConstructor
@PropertySource("classpath:hidden.properties")
public class HealthBot extends TelegramLongPollingBot {

    @Value("${bot.token}")
    private String accessToken;

    @Value("${bot.name}")
    private String botUsername;

    @Value("${health-check.connection-timeout}")
    private Integer connectionTimeout;

    private final UpdateController updateController;

    @PostConstruct
    public void init() {
        updateController.registerBot(this);
    }

    @Override
    public void onUpdateReceived(Update update) {
        updateController.processUpdate(update);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return accessToken;
    }

    public BotInfo getBotInfo() {
        return BotInfo.builder()
                .healthCheckConnectionTimeout(connectionTimeout)
                .botUsername(getBotUsername())
                .build();
    }
}
