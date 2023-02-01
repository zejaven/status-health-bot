package org.zeveon.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.zeveon.component.HealthBot;
import org.zeveon.context.UserContext;
import org.zeveon.data.Data;
import org.zeveon.entity.ChatSettings;
import org.zeveon.entity.Host;
import org.zeveon.model.Command;
import org.zeveon.model.HealthInfo;
import org.zeveon.model.Language;
import org.zeveon.service.ChatSettingsService;
import org.zeveon.service.HealthService;
import org.zeveon.service.StatisticService;

import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
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

    private static final String HELP_TEMPLATE = "%s - %s";
    private static final String NEW_LINE_TEMPLATE = "%s\n%s";
    private static final String HOST_LIST_TEMPLATE = "%s: %s";
    private static final String HOST_STATUS_CHANGED_TEMPLATE = "";

    private final MessageSource messageSource;

    private final HealthService healthService;

    private final StatisticService statisticService;

    private final ChatSettingsService chatSettingsService;

    private HealthBot healthBot;

    public void registerBot(HealthBot healthBot) {
        this.healthBot = healthBot;
        Data.initialize(healthService.getAllHosts());
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

    public void reportStatusCodeChanged(Host host, HealthInfo healthInfo) {
        chatSettingsService.findChatSettingsByHost(host)
                .forEach(c -> sendResponse(
                        buildStatusCodeChangedResponse(c.getChatId(), host, healthInfo), c.getChatId()));
    }

    private void processMessage(Update update) {
        var message = update.getMessage();
        setCurrentUser(message);
        var chatId = message.getChatId();
        if (isTrue(message.getGroupchatCreated())) {
            sendResponse(buildChatCreatedResponse(chatId), chatId);
        } else {
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
    }

    private void setCurrentUser(Message message) {
        UserContext.setInstance(message.getFrom());
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
        Set<String> argsSet = !args.isEmpty()
                ? stream(args.split(LF)).map(String::strip).collect(toSet())
                : emptySet();
        if (!argsSet.isEmpty()) {
            healthService.saveHosts(argsSet, chatId);
            return buildHostsResponse(chatId);
        } else {
            return getLocalizedMessage("message.nothing_to_add", chatId);
        }
    }

    private String buildHostsResponse(Long chatId) {
        return healthService.getHosts(chatId).stream()
                .sorted(comparing(Host::getId))
                .map(h -> HOST_LIST_TEMPLATE.formatted(h.getId(), h.getUrl()))
                .reduce(NEW_LINE_TEMPLATE::formatted)
                .orElse(getLocalizedMessage("message.empty_hosts", chatId));
    }

    private String buildRemoveResponse(String args, Long chatId) {
        Set<Long> argsSet = !args.isEmpty()
                ? stream(args.split(COMMA)).map(String::strip).map(Long::parseLong).collect(toSet())
                : emptySet();
        if (!argsSet.isEmpty()) {
            healthService.removeHosts(argsSet, chatId);
            return buildHostsResponse(chatId);
        } else {
            return getLocalizedMessage("message.nothing_to_remove", chatId);
        }
    }

    private Optional<InputFile> buildStatisticResponse(Long chatId) {
        return statisticService.generateStatistic(chatId);
    }

    private String buildChangeLanguageResponse(Long chatId, String language) {
        if (languageSupported(language)) {
            chatSettingsService.changeLocale(chatId, language);
            return getLocalizedMessage("message.change_language_success", chatId);
        } else {
            return getLocalizedMessage("message.no_such_language", chatId).concat(SPACE).concat(language);
        }
    }

    private boolean languageSupported(String language) {
        return stream(Language.values()).anyMatch(l -> l.name().equals(language));
    }

    private String buildChatCreatedResponse(Long chatId) {
        var user = UserContext.getInstance().getUser();
        var language = ofNullable(user.getLanguageCode())
                .map(String::toUpperCase)
                .orElse(ChatSettings.builder().build().getLocale());
        if (stream(Language.values()).map(Language::name).anyMatch(l -> l.equals(language))) {
            chatSettingsService.save(ChatSettings.builder()
                    .chatId(chatId)
                    .locale(language)
                    .build());
        } else {
            chatSettingsService.save(chatId);
        }
        return NEW_LINE_TEMPLATE.formatted(
                getLocalizedMessage("message.new_chat", chatId),
                buildHelpResponse(chatId));
    }

    private String buildStatusCodeChangedResponse(Long chatId, Host host, HealthInfo healthInfo) {
        int responseCode = healthInfo.getResponseCode();
        if (!healthInfo.isStatisticExists()) {
            return responseCode == 0
                    ? getLocalizedMessage("message.host_just_added_not_reachable", chatId).formatted(host.getUrl())
                    : getLocalizedMessage("message.host_just_added", chatId).formatted(host.getUrl(), responseCode);
        }
        if (responseCode == 0) {
            return getLocalizedMessage("message.host_not_reachable", chatId).formatted(host.getUrl());
        }
        return HttpStatus.valueOf(responseCode).is2xxSuccessful()
                ? getLocalizedMessage("message.host_restored", chatId).formatted(host.getUrl(), responseCode)
                : getLocalizedMessage("message.host_down", chatId).formatted(host.getUrl(), responseCode);
    }

    private String getLocalizedMessage(String code, Long chatId) {
        return messageSource.getMessage(code, null, chatSettingsService.getLocale(chatId));
    }
}
