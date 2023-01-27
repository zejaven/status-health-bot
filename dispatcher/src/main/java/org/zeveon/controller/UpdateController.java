package org.zeveon.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.zeveon.data.Data;
import org.zeveon.entity.Site;
import org.zeveon.model.Command;
import org.zeveon.service.HealthService;
import org.zeveon.service.StatisticService;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.zeveon.util.StringUtil.*;

/**
 * @author Stanislav Vafin
 */
@Slf4j
@Controller
public class UpdateController {

    private final HealthService healthService;

    private final StatisticService statisticService;

    private HealthBot healthBot;

    public UpdateController(HealthService healthService, StatisticService statisticService) {
        this.healthService = healthService;
        this.statisticService = statisticService;
    }

    public void registerBot(HealthBot healthBot) {
        this.healthBot = healthBot;
        Data.initialize(healthService.getSites());
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
            case Command.HELP -> sendResponse(buildHelpResponse(), chatId);
            case Command.ADD -> sendResponse(buildAddResponse(args), chatId);
            case Command.GET_SITES -> sendResponse(buildSitesResponse(), chatId);
            case Command.REMOVE -> sendResponse(buildRemoveResponse(args), chatId);
            case Command.STATISTIC -> sendResponse(buildStatisticResponse(), chatId);
            default -> sendResponse(buildEmptyResponse(), chatId);
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
                () -> sendResponse(STATISTIC_GENERATION_FAILED, chatId)
        );
    }

    private void sendResponse(InputFile inputFile, Long chatId) {
        var response = new SendDocument();
        response.setChatId(chatId);
        response.setDocument(inputFile);
        sendResponse(response);
    }

    public void sendResponse(SendMessage message) {
        try {
            healthBot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendResponse(SendDocument document) {
        try {
            healthBot.execute(document);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String buildEmptyResponse() {
        return EMPTY_RESPONSE;
    }

    private String buildHelpResponse() {
        return Command.LIST.entrySet().stream()
                .map(e -> HELP_TEMPLATE.formatted(e.getKey(), e.getValue()))
                .reduce(NEW_LINE_TEMPLATE::formatted)
                .orElse(EMPTY_HELP_RESPONSE);
    }

    private String buildAddResponse(String args) {
        List<String> argsList = !args.isEmpty()
                ? stream(args.split(LF)).map(String::strip).toList()
                : emptyList();
        if (!argsList.isEmpty()) {
            healthService.saveSites(argsList);
            return buildSitesResponse();
        } else {
            return NOTHING_TO_ADD_RESPONSE;
        }
    }

    private String buildSitesResponse() {
        return healthService.getSites().stream()
                .sorted(comparing(Site::getId))
                .map(s -> SITE_LIST_TEMPLATE.formatted(s.getId(), s.getUrl()))
                .reduce(NEW_LINE_TEMPLATE::formatted)
                .orElse(EMPTY_SITES_RESPONSE);
    }

    private String buildRemoveResponse(String args) {
        List<Long> argsList = !args.isEmpty()
                ? stream(args.split(COMMA)).map(String::strip).map(Long::parseLong).toList()
                : emptyList();
        if (!argsList.isEmpty()) {
            healthService.removeSites(argsList);
            return buildSitesResponse();
        } else {
            return NOTHING_TO_REMOVE_RESPONSE;
        }
    }

    private Optional<InputFile> buildStatisticResponse() {
        return statisticService.generateStatistic();
    }
}
