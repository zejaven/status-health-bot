package org.zeveon.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.zeveon.cache.Cache;
import org.zeveon.entity.Host;
import org.zeveon.entity.Statistic;
import org.zeveon.model.Method;
import org.zeveon.service.ChatSettingsService;
import org.zeveon.service.HealthService;
import org.zeveon.service.StatisticService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * @author Stanislav Vafin
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticServiceImpl implements StatisticService {

    public static final double NANOS_IN_SECOND = 1_000_000_000.0;
    public static final String DEFAULT_STATISTIC_FILE_NAME = "speed-test-statistic.xlsx";
    public static final String STATISTIC_TEMPLATE_TIME = "%s, %s, %s";
    public static final String STATISTIC_TEMPLATE_CODE = "%s, %s";
    public static final String URL = "URL";
    public static final Map<Method, String> DESCRIPTION = Map.of(
            Method.APACHE_HTTP_CLIENT, "method.apache",
            Method.JAVA_HTTP_CLIENT, "method.java",
            Method.CURL_PROCESS, "method.curl"
    );

    private final MessageSource messageSource;

    private final HealthService healthService;

    private final ChatSettingsService chatSettingsService;

    @Override
    @CacheEvict(key = "#chatId", value = Cache.HOSTS, beforeInvocation = true)
    @Transactional(readOnly = true)
    public Optional<InputFile> generateStatistic(Long chatId) {
        try (var workbook = new XSSFWorkbook(); var outputStream = new ByteArrayOutputStream()) {
            buildStatistic(workbook, chatId);
            workbook.write(outputStream);
            return of(new InputFile()
                    .setMedia(new ByteArrayInputStream(outputStream.toByteArray()), DEFAULT_STATISTIC_FILE_NAME));
        } catch (IOException e) {
            log.error(e.getMessage());
            return empty();
        }
    }

    private void buildStatistic(XSSFWorkbook workbook, Long chatId) {
        var locale = chatSettingsService.getLocale(chatId);
        var sheet = workbook.createSheet();
        var rowHeader = sheet.createRow(0);
        rowHeader.createCell(0).setCellValue(URL);
        for (int i = 0; i < Method.values().length; i++) {
            rowHeader.createCell(i + 1)
                    .setCellValue(STATISTIC_TEMPLATE_TIME.formatted(
                            getLocalizedMessage(DESCRIPTION.get(Method.values()[i]), locale),
                            getLocalizedMessage("statistic.response_time", locale),
                            getLocalizedMessage("statistic.secs", locale)
                    ));
        }
        for (int i = 0; i < Method.values().length; i++) {
            rowHeader.createCell(i + 4)
                    .setCellValue(STATISTIC_TEMPLATE_CODE.formatted(
                            getLocalizedMessage(DESCRIPTION.get(Method.values()[i]), locale),
                            getLocalizedMessage("statistic.response_code", locale)
                    ));
        }
        var hosts = healthService.getHosts(chatId);
        var iterator = hosts.iterator();
        for (int i = 0; i < hosts.size(); i++) {
            var host = iterator.next();
            var row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(host.getUrl());
            for (int j = 0; j < Method.values().length; j++) {
                row.createCell(j + 1).setCellValue(getResponseTimeInSeconds(host, Method.values()[j]));
            }
            for (int j = 0; j < Method.values().length; j++) {
                row.createCell(j + 4).setCellValue(getResponseCode(host, Method.values()[j]));
            }
        }
        for (int i = 0; i < 7; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private Double getResponseTimeInSeconds(Host host, Method apacheHttpClient) {
        return host.getStatistic().stream()
                .filter(s -> s.getId().getMethod().equals(apacheHttpClient))
                .map(s -> s.getResponseTime().toNanos())
                .map(n -> n / NANOS_IN_SECOND)
                .findAny().orElse(Double.NaN);
    }

    private Integer getResponseCode(Host host, Method apacheHttpClient) {
        return host.getStatistic().stream()
                .filter(s -> s.getId().getMethod().equals(apacheHttpClient))
                .map(Statistic::getResponseCode)
                .findAny().orElse(0);
    }

    private String getLocalizedMessage(String code, Locale locale) {
        return messageSource.getMessage(code, null, locale);
    }
}
