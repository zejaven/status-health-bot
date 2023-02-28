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
import org.zeveon.model.Method;
import org.zeveon.service.ChatSettingsService;
import org.zeveon.service.HealthService;
import org.zeveon.service.PersonService;
import org.zeveon.service.StatisticService;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.*;
import static org.zeveon.util.StringUtil.*;

/**
 * @author Stanislav Vafin
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class UpdateController {

    private static final String NEW_LINE_TEMPLATE = "%s\n%s";
    private static final String HOST_LIST_TEMPLATE = "%s: %s";
    public static final String UTC = "UTC";

    private final MessageSource messageSource;

    private final HealthService healthService;

    private final StatisticService statisticService;

    private final ChatSettingsService chatSettingsService;

    private final PersonService personService;

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
        var message = update.getMessage();
        var chatId = message.getChatId();
        if (isTrue(message.getGroupchatCreated())) {
            sendResponse(buildChatCreatedResponse(chatId), chatId);
        } else {
            var text = message.getText();
            var command = text.split(WHITESPACE_CHARACTER)[0];
            if (command.startsWith(SLASH)) {
                var args = text.replace(command, EMPTY).strip();
                switch (command) {
                    case Command.HELP -> sendResponse(buildHelpResponse(chatId), chatId);
                    case Command.ADD -> sendResponse(buildAddResponse(chatId, args), chatId);
                    case Command.GET_HOSTS -> sendResponse(buildHostsResponse(chatId), chatId);
                    case Command.REMOVE -> sendResponse(buildRemoveResponse(chatId, args), chatId);
                    case Command.REMOVE_ALL -> sendResponse(buildRemoveAllResponse(chatId), chatId);
                    case Command.STATISTIC -> sendResponse(buildStatisticResponse(chatId), chatId);
                    case Command.SETTINGS -> sendResponse(buildSettingsResponse(chatId), chatId);
                    case Command.CHANGE_LANGUAGE -> sendResponse(buildChangeLanguageResponse(chatId, args.toUpperCase()), chatId);
                    case Command.CHANGE_OFFSET -> sendResponse(buildChangeOffsetResponse(chatId, args.toUpperCase()), chatId);
                    case Command.CHANGE_METHOD -> sendResponse(buildChangeMethodResponse(chatId, args.toUpperCase()), chatId);
                    case Command.CHANGE_RATE -> sendResponse(buildChangeRateResponse(chatId, args), chatId);
                    case Command.ADD_ADMIN -> sendResponse(buildAddAdminResponse(chatId, args), chatId);
                    case Command.REMOVE_ADMIN -> sendResponse(buildRemoveAdminResponse(chatId, args), chatId);
                    default -> sendResponse(buildEmptyResponse(chatId), chatId);
                }
            }
        }
    }

    public void reportStatusCodeChanged(Host host, Method method, HealthInfo healthInfo) {
        chatSettingsService.findChatSettingsByHostAndMethod(host, method)
                .forEach(c -> sendResponse(
                        buildStatusCodeChangedResponse(c.getChatId(), host, healthInfo),
                        c.getChatId()
                ));
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
        var isSuperAdmin = UserContext.getInstance().isSuperAdmin();
        return Command.LIST.entrySet().stream()
                .filter(c -> !Command.SUPER_ADMIN_COMMANDS.contains(c.getKey()) || isSuperAdmin)
                .map(e -> getLocalizedMessage(e.getValue(), chatId).formatted(e.getKey()))
                .reduce(NEW_LINE_TEMPLATE::formatted)
                .orElse(getLocalizedMessage("message.empty_help", chatId));
    }

    private String buildAddResponse(Long chatId, String args) {
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
                .map(h -> HOST_LIST_TEMPLATE.formatted(h.getId(), h.getUrl().replace(DOT, INVISIBLE_DOT)))
                .reduce(NEW_LINE_TEMPLATE::formatted)
                .orElse(getLocalizedMessage("message.empty_hosts", chatId));
    }

    private String buildRemoveResponse(Long chatId, String args) {
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
            chatSettingsService.updateLocale(chatId, language);
            return getLocalizedMessage("message.change_language_success", chatId);
        } else {
            return getLocalizedMessage("message.no_such_language", chatId).formatted(language);
        }
    }

    private String buildChangeOffsetResponse(Long chatId, String offset) {
        try {
            var zoneId = ZoneId.ofOffset(UTC, ZoneOffset.of(offset));
            chatSettingsService.updateZoneId(chatId, zoneId.getId());
            return getLocalizedMessage("message.change_offset_success", chatId);
        } catch (DateTimeException e) {
            return getLocalizedMessage("message.change_offset_wrong_format", chatId);
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
        return getLocalizedMessage("message.new_chat", chatId).formatted(buildHelpResponse(chatId));
    }

    private String buildStatusCodeChangedResponse(Long chatId, Host host, HealthInfo healthInfo) {
        var responseUrl = host.getUrl().replace(DOT, INVISIBLE_DOT);
        int responseCode = healthInfo.getResponseCode();
        if (!healthInfo.isStatisticExists()) {
            return responseCode == 0
                    ? getLocalizedMessage("message.host_just_added_not_reachable", chatId).formatted(responseUrl)
                    : getLocalizedMessage("message.host_just_added", chatId).formatted(responseUrl, responseCode);
        }
        if (responseCode == 0) {
            return getLocalizedMessage("message.host_not_reachable", chatId).formatted(responseUrl);
        }
        return HttpStatus.valueOf(responseCode).is2xxSuccessful()
                ? getLocalizedMessage("message.host_restored", chatId).formatted(responseUrl, responseCode)
                : getLocalizedMessage("message.host_down", chatId).formatted(responseUrl, responseCode);
    }

    private String buildRemoveAllResponse(Long chatId) {
        healthService.removeAllHosts(chatId);
        return buildHostsResponse(chatId);
    }

    private String buildChangeMethodResponse(Long chatId, String method) {
        if (methodSupported(method)) {
            chatSettingsService.updateMethod(chatId, Method.valueOf(method));
            return getLocalizedMessage("message.change_method_success", chatId);
        } else {
            return getLocalizedMessage("message.no_such_method", chatId).formatted(method);
        }
    }

    private String buildChangeRateResponse(Long chatId, String durationStr) {
        var userContext = UserContext.getInstance();
        try {
            var duration = Duration.parse(durationStr);
            if (userContext.isAdmin()) {
                if (duration.toSeconds() >= 1) {
                    chatSettingsService.updateCheckRate(chatId, duration);
                    return getLocalizedMessage("message.change_rate_success", chatId)
                            .formatted(getDurationReadableFormat(duration, chatId));
                } else {
                    return getLocalizedMessage("message.change_rate_less_than_second", chatId);
                }
            } else {
                return getLocalizedMessage("message.change_rate_access_denied", chatId);
            }
        } catch (RuntimeException re) {
            try {
                var minutes = Long.parseLong(durationStr);
                var duration = Duration.ofMinutes(minutes);
                if (minutes >= 1L) {
                    chatSettingsService.updateCheckRate(chatId, duration);
                    return getLocalizedMessage("message.change_rate_success", chatId)
                            .formatted(getDurationReadableFormat(duration, chatId));
                } else {
                    return getLocalizedMessage("message.change_rate_less_than_second", chatId);
                }
            } catch (NumberFormatException nfe) {
                return getLocalizedMessage("message.change_rate_wrong_format", chatId);
            }
        }
    }

    private String getDurationReadableFormat(Duration duration, Long chatId) {
        return new LinkedHashMap<String, Number>() {{
            put(getLocalizedMessage("duration.days", chatId), duration.toDaysPart());
            put(getLocalizedMessage("duration.hours", chatId), duration.toHoursPart());
            put(getLocalizedMessage("duration.minutes", chatId), duration.toMinutesPart());
            put(getLocalizedMessage("duration.seconds", chatId), duration.toSecondsPart());
        }}.entrySet().stream()
                .filter(e -> e.getValue().longValue() > 0)
                .map(e -> e.getKey().formatted(e.getValue()))
                .reduce((a, b) -> a.concat(SPACE).concat(b))
                .orElse(EMPTY);
    }

    private String buildAddAdminResponse(Long chatId, String username) {
        var userContext = UserContext.getInstance();
        if (userContext.isSuperAdmin()) {
            return personService.updateAdminRights(username, true).isPresent()
                    ? getLocalizedMessage("message.add_admin_success", chatId).formatted(username)
                    : getLocalizedMessage("message.add_admin_failed", chatId).formatted(username);
        } else {
            return getLocalizedMessage("message.super_admin_access_denied", chatId);
        }
    }

    private String buildRemoveAdminResponse(Long chatId, String username) {
        var userContext = UserContext.getInstance();
        if (userContext.isSuperAdmin()) {
            return personService.updateAdminRights(username, false).isPresent()
                    ? getLocalizedMessage("message.remove_admin_success", chatId).formatted(username)
                    : getLocalizedMessage("message.remove_admin_failed", chatId).formatted(username);
        } else {
            return getLocalizedMessage("message.super_admin_access_denied", chatId);
        }
    }

    private String buildSettingsResponse(Long chatId) {
        return chatSettingsService.getChatSettings(chatId)
                .map(c -> buildSettingFields(chatId, c).stream()
                        .reduce(NEW_LINE_TEMPLATE::formatted)
                        .orElse(getLocalizedMessage("message.empty_settings", chatId))
                ).orElse(getLocalizedMessage("message.empty_settings", chatId));
    }

    private List<String> buildSettingFields(Long chatId, ChatSettings settings) {
        return List.of(
                getLocalizedMessage("chat_settings.locale", chatId).formatted(settings.getLocale()),
                getLocalizedMessage("chat_settings.method", chatId).formatted(settings.getMethod()),
                getLocalizedMessage("chat_settings.check_rate", chatId).formatted(
                        getDurationReadableFormat(settings.getCheckRate(), chatId)),
                getLocalizedMessage("chat_settings.modified_date", chatId).formatted(
                        ofNullable(settings.getModifiedDate())
                                .map(d -> d.withZoneSameInstant(getTimezone(settings))
                                        .format(DateTimeFormatter.RFC_1123_DATE_TIME))
                                .orElse(EMPTY)),
                getLocalizedMessage("chat_settings.modified_by", chatId).formatted(
                        ofNullable(settings.getModifiedBy())
                                .orElse(EMPTY))
        );
    }

    private ZoneId getTimezone(ChatSettings settings) {
        return ZoneId.of(settings.getZoneId());
    }

    private boolean methodSupported(String method) {
        return stream(Method.values()).anyMatch(m -> m.name().equals(method));
    }

    private String getLocalizedMessage(String code, Long chatId) {
        return messageSource.getMessage(code, null, chatSettingsService.getLocale(chatId));
    }
}
