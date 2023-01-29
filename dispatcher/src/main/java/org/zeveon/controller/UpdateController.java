package org.zeveon.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.zeveon.data.Data;
import org.zeveon.entity.Host;
import org.zeveon.model.Command;
import org.zeveon.model.Language;
import org.zeveon.service.ChatSettingsService;
import org.zeveon.service.HealthService;
import org.zeveon.service.StatisticService;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static org.apache.commons.lang3.StringUtils.*;
import static org.zeveon.util.StringUtil.COMMA;
import static org.zeveon.util.StringUtil.WHITESPACE_CHARACTER;

/**
 * @author Stanislav Vafin
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class UpdateController {

    public static final String HELP_TEMPLATE = "%s - %s";
    public static final String NEW_LINE_TEMPLATE = "%s\n%s";
    public static final String HOST_LIST_TEMPLATE = "%s: %s";

    private final MessageSource messageSource;

    private final HealthService healthService;

    private final StatisticService statisticService;

    private final ChatSettingsService chatSettingsService;

    private HealthBot healthBot;

    public void registerBot(HealthBot healthBot) {
        this.healthBot = healthBot;
        Data.initialize(healthService.getHosts());
        try {
            new TelegramBotsApi(DefaultBotSession.class).registerBot(healthBot);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void processUpdate(Update update) {
        if (update == null) {
            log.error("Received update is null");
            return;
        }

        if (update.getMessage() != null) {
            processMessage(update);
        } else {
            log.error("Received unsupported message type: " + update);
        }
    }

    private void processMessage(Update update) {
        var message = update.getMessage();
        var chatId = message.getChatId();
        var text = message.getText();
        var command = text.split(WHITESPACE_CHARACTER)[0];
        var args = text.replace(command, EMPTY).strip();
        switch (command) {
            case Command.HELP -> sendResponse(buildHelpResponse(chatId), chatId);
            case Command.ADD -> sendResponse(buildAddResponse(args, chatId), chatId);
            case Command.GET_HOSTS -> sendResponse(buildHostsResponse(chatId), chatId);
            case Command.REMOVE -> sendResponse(buildRemoveResponse(args, chatId), chatId);
            case Command.STATISTIC -> sendResponse(buildStatisticResponse(chatId), chatId);
            case Command.CHANGE_LANGUAGE -> sendResponse(buildChangeLanguageResponse(chatId, args.toUpperCase()), chatId);
            default -> sendResponse(buildEmptyResponse(chatId), chatId);
        }
    }

    private void sendResponse(String message, Long chatId) {
        var response = new SendMessage();
        response.setChatId(chatId);
        response.setText(message);
        sendResponse(response);
    }

    private void sendResponse(Optional<InputFile> inputFile, Long chatId) {
        inputFile.ifPresentOrElse(
                i -> sendResponse(i, chatId),
                () -> sendResponse(getLocalizedMessage("message.statistic_generation_failed", chatId), chatId)
        );
    }

    private void sendResponse(InputFile inputFile, Long chatId) {
        var response = new SendDocument();
        response.setChatId(chatId);
        response.setDocument(inputFile);
        sendResponse(response);
    }

    private void sendResponse(SendMessage message) {
        try {
            healthBot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendResponse(SendDocument document) {
        try {
            healthBot.execute(document);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String buildEmptyResponse(Long chatId) {
        return getLocalizedMessage("message.empty", chatId);
    }

    private String buildHelpResponse(Long chatId) {
        return Command.LIST.entrySet().stream()
                .map(e -> HELP_TEMPLATE.formatted(e.getKey(), getLocalizedMessage(e.getValue(), chatId)))
                .reduce(NEW_LINE_TEMPLATE::formatted)
                .orElse(getLocalizedMessage("message.empty_help", chatId));
    }

    private String buildAddResponse(String args, Long chatId) {
        List<String> argsList = !args.isEmpty()
                ? stream(args.split(LF)).map(String::strip).toList()
                : emptyList();
        if (!argsList.isEmpty()) {
            healthService.saveHosts(argsList);
            return buildHostsResponse(chatId);
        } else {
            return getLocalizedMessage("message.nothing_to_add", chatId);
        }
    }

    private String buildHostsResponse(Long chatId) {
        return healthService.getHosts().stream()
                .sorted(comparing(Host::getId))
                .map(s -> HOST_LIST_TEMPLATE.formatted(s.getId(), s.getUrl()))
                .reduce(NEW_LINE_TEMPLATE::formatted)
                .orElse(getLocalizedMessage("message.empty_hosts", chatId));
    }

    private String buildRemoveResponse(String args, Long chatId) {
        List<Long> argsList = !args.isEmpty()
                ? stream(args.split(COMMA)).map(String::strip).map(Long::parseLong).toList()
                : emptyList();
        if (!argsList.isEmpty()) {
            healthService.removeHosts(argsList);
            return buildHostsResponse(chatId);
        } else {
            return getLocalizedMessage("message.nothing_to_remove", chatId);
        }
    }

    private Optional<InputFile> buildStatisticResponse(Long chatId) {
        return statisticService.generateStatistic(chatId);
    }

    private String buildChangeLanguageResponse(Long chatId, String language) {
        if (stream(Language.values()).anyMatch(l -> l.name().equals(language))) {
            chatSettingsService.changeLocale(chatId, language);
            return getLocalizedMessage("message.change_language_success", chatId);
        } else {
            return getLocalizedMessage("message.no_such_language", chatId).concat(SPACE).concat(language);
        }
    }

    private String getLocalizedMessage(String code, Long chatId) {
        return messageSource.getMessage(code, null, chatSettingsService.getLocale(chatId));
    }
}
